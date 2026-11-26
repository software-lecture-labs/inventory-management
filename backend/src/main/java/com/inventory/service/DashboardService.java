package com.inventory.service;

import com.inventory.dto.DashboardResponse;
import com.inventory.dto.DashboardResponse.CalibrationItem;
import com.inventory.dto.DashboardResponse.DailyCount;
import com.inventory.dto.DashboardResponse.TopEquipment;
import com.inventory.dto.ReservationDtos.ReservationResponse;
import com.inventory.model.EquipmentStatus;
import com.inventory.model.ReservationStatus;
import com.inventory.repository.EquipmentRepository;
import com.inventory.repository.ReservationRepository;
import com.inventory.security.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class DashboardService {

    static final int DAILY_CHART_DAYS = 14;
    static final int CALIBRATION_WARNING_DAYS = 30;
    static final int TOP_EQUIPMENT_DAYS = 30;

    // Gerçekleşmiş veya gerçekleşecek rezervasyonlar (iptal / red hariç)
    private static final Set<ReservationStatus> COUNTED = EnumSet.of(
            ReservationStatus.PENDING, ReservationStatus.APPROVED,
            ReservationStatus.CHECKED_OUT, ReservationStatus.RETURNED);

    private final EquipmentRepository equipmentRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public DashboardService(EquipmentRepository equipmentRepository,
                            ReservationRepository reservationRepository,
                            Clock clock) {
        this.equipmentRepository = equipmentRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get(UserPrincipal currentUser) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        boolean admin = currentUser.isAdmin();

        Map<EquipmentStatus, Long> byStatus = new EnumMap<>(EquipmentStatus.class);
        for (EquipmentStatus s : EquipmentStatus.values()) {
            byStatus.put(s, 0L);
        }
        for (Object[] row : equipmentRepository.countByStatus()) {
            byStatus.put((EquipmentStatus) row[0], (Long) row[1]);
        }

        long activeNow = reservationRepository.countInRange(now, now,
                EnumSet.of(ReservationStatus.APPROVED, ReservationStatus.CHECKED_OUT));
        long todayCount = reservationRepository.countInRange(today.atStartOfDay(),
                today.plusDays(1).atStartOfDay(), COUNTED);

        List<CalibrationItem> calibrationDue = equipmentRepository
                .findCalibrationDueBy(today.plusDays(CALIBRATION_WARNING_DAYS)).stream()
                .limit(8)
                .map(e -> new CalibrationItem(e.getId(), e.getName(), e.getSerialNumber(), e.getCalibrationDate(),
                        e.getCalibrationDate().isBefore(today)))
                .toList();

        List<TopEquipment> top = reservationRepository
                .findTopEquipment(now.minusDays(TOP_EQUIPMENT_DAYS), COUNTED, PageRequest.of(0, 5)).stream()
                .map(row -> new TopEquipment((Long) row[0], (String) row[1], (Long) row[2]))
                .toList();

        List<ReservationResponse> myUpcoming = reservationRepository
                .findUpcomingForUser(currentUser.id(), ReservationStatus.BLOCKING, now, PageRequest.of(0, 5)).stream()
                .map(r -> ReservationResponse.from(r, now))
                .toList();

        return new DashboardResponse(
                equipmentRepository.countByActiveTrue(),
                byStatus,
                activeNow,
                todayCount,
                admin ? reservationRepository.countByStatus(ReservationStatus.PENDING) : 0,
                admin ? reservationRepository.countByStatusAndEndTimeBefore(ReservationStatus.CHECKED_OUT, now) : 0,
                calibrationDue,
                top,
                dailyCounts(today),
                myUpcoming
        );
    }

    /** Son {DAILY_CHART_DAYS} günün (bugün dahil) her biri için başlayan rezervasyon sayısı. */
    private List<DailyCount> dailyCounts(LocalDate today) {
        LocalDate first = today.minusDays(DAILY_CHART_DAYS - 1L);
        Map<LocalDate, Long> counts = new TreeMap<>();
        for (int i = 0; i < DAILY_CHART_DAYS; i++) {
            counts.put(first.plusDays(i), 0L);
        }
        reservationRepository.findStartTimes(first.atStartOfDay(), today.plusDays(1).atStartOfDay(), COUNTED)
                .forEach(t -> counts.merge(t.toLocalDate(), 1L, Long::sum));

        List<DailyCount> result = new ArrayList<>();
        counts.forEach((date, count) -> result.add(new DailyCount(date, count)));
        return result;
    }
}
