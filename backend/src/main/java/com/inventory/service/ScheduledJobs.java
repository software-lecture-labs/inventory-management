package com.inventory.service;

import com.inventory.model.Equipment;
import com.inventory.repository.EquipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Arka planda düzenli çalışan işler. app.scheduler.enabled=false ile kapatılabilir (testlerde kapalıdır).
 */
@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final ReservationService reservationService;
    private final InboxService inboxService;
    private final EquipmentRepository equipmentRepository;
    private final NotificationService notificationService;
    private final Clock clock;
    private final int reminderMinutes;
    private final int calibrationWarningDays;

    public ScheduledJobs(ReservationService reservationService,
                         InboxService inboxService,
                         EquipmentRepository equipmentRepository,
                         NotificationService notificationService,
                         Clock clock,
                         @Value("${app.reservation.reminder-minutes:60}") int reminderMinutes,
                         @Value("${app.calibration.warning-days:7}") int calibrationWarningDays) {
        this.reservationService = reservationService;
        this.inboxService = inboxService;
        this.equipmentRepository = equipmentRepository;
        this.notificationService = notificationService;
        this.clock = clock;
        this.reminderMinutes = reminderMinutes;
        this.calibrationWarningDays = calibrationWarningDays;
    }

    /** Her 5 dakikada bir: yaklaşan rezervasyon hatırlatmaları ve geç iade uyarıları. */
    @Scheduled(fixedDelayString = "${app.scheduler.interval-ms:300000}", initialDelay = 60_000)
    public void reservationNotifications() {
        try {
            int reminders = reservationService.sendReminders(reminderMinutes);
            int overdue = reservationService.notifyOverdue();
            if (reminders + overdue > 0) {
                log.info("Bildirimler: {} hatırlatma, {} geç iade uyarısı", reminders, overdue);
            }
        } catch (RuntimeException e) {
            log.error("Rezervasyon bildirimleri gönderilemedi", e);
        }
    }

    /** Her sabah: kalibrasyon hatırlatması ve eski kayıtların temizliği. */
    @Scheduled(cron = "${app.scheduler.daily-cron:0 0 8 * * *}")
    public void daily() {
        try {
            LocalDate today = LocalDate.now(clock);
            List<Equipment> due = equipmentRepository.findCalibrationDueBy(today.plusDays(calibrationWarningDays));
            notificationService.calibrationDigest(due);

            int waitlist = reservationService.cleanupWaitlist();
            int notifications = inboxService.cleanup();
            log.info("Günlük iş: {} kalibrasyon uyarısı, {} eski bekleme kaydı ve {} eski bildirim silindi",
                    due.size(), waitlist, notifications);
        } catch (RuntimeException e) {
            log.error("Günlük iş çalıştırılamadı", e);
        }
    }
}
