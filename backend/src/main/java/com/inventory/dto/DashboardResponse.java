package com.inventory.dto;

import com.inventory.dto.ReservationDtos.ReservationResponse;
import com.inventory.model.EquipmentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Ana sayfadaki özet panelin verileri.
 * Yöneticiye özel alanlar (pendingApprovals, overdueReturns) normal kullanıcıda 0 döner.
 */
public record DashboardResponse(
        long totalEquipment,
        Map<EquipmentStatus, Long> equipmentByStatus,
        long activeNow,
        long todayReservations,
        long pendingApprovals,
        long overdueReturns,
        List<CalibrationItem> calibrationDue,
        List<TopEquipment> topEquipment,
        List<DailyCount> dailyReservations,
        List<ReservationResponse> myUpcoming
) {
    public record CalibrationItem(Long id, String name, String serialNumber, LocalDate calibrationDate, boolean overdue) {
    }

    public record TopEquipment(Long id, String name, long count) {
    }

    public record DailyCount(LocalDate date, long count) {
    }
}
