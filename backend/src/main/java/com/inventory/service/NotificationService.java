package com.inventory.service;

import com.inventory.model.Equipment;
import com.inventory.model.Notification;
import com.inventory.model.NotificationType;
import com.inventory.model.Reservation;
import com.inventory.model.ReservationStatus;
import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.model.WaitlistEntry;
import com.inventory.repository.NotificationRepository;
import com.inventory.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Uygulama içi bildirimleri (üst bardaki zil) oluşturur.
 * Bildirimler, onları tetikleyen işlemle aynı veritabanı işleminde kaydedilir;
 * işlem geri alınırsa bildirim de oluşmaz.
 */
@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public NotificationService(UserRepository userRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    // ---------- Rezervasyon ----------

    /** Onay gerektiren bir talep oluştuysa yöneticilere haber verir. */
    public void reservationCreated(Reservation r) {
        if (r.getStatus() != ReservationStatus.PENDING) {
            return;
        }
        for (User admin : admins()) {
            save(admin, NotificationType.APPROVAL_REQUEST, "Onay bekleyen talep",
                    r.getUser().getFullName() + ", " + r.getEquipment().getName() + " için rezervasyon talep etti. "
                            + period(r), r);
        }
    }

    public void reservationApproved(Reservation r) {
        save(r.getUser(), NotificationType.RESERVATION_APPROVED, "Rezervasyonunuz onaylandı",
                r.getEquipment().getName() + " rezervasyonunuz onaylandı. " + period(r), r);
    }

    public void reservationRejected(Reservation r) {
        save(r.getUser(), NotificationType.RESERVATION_REJECTED, "Rezervasyonunuz reddedildi",
                r.getEquipment().getName() + " rezervasyon talebiniz reddedildi. " + period(r) + reason(r.getNote()), r);
    }

    /** Rezervasyon sahibi dışında biri (yönetici) iptal ettiğinde sahibine haber verir. */
    public void reservationCancelledByOther(Reservation r, String cancelledBy) {
        save(r.getUser(), NotificationType.RESERVATION_CANCELLED, "Rezervasyonunuz iptal edildi",
                r.getEquipment().getName() + " rezervasyonunuz " + cancelledBy + " tarafından iptal edildi. "
                        + period(r) + reason(r.getNote()), r);
    }

    public void reservationReminder(Reservation r) {
        save(r.getUser(), NotificationType.RESERVATION_REMINDER, "Rezervasyonunuz yaklaşıyor",
                r.getEquipment().getName() + " rezervasyonunuz yakında başlıyor. " + period(r)
                        + " Ekipmanı aldığınızda \"Teslim Al\" düğmesine basmayı unutmayın.", r);
    }

    public void reservationOverdue(Reservation r) {
        save(r.getUser(), NotificationType.RESERVATION_OVERDUE, "İade süresi geçti",
                r.getEquipment().getName() + " ekipmanının iade süresi geçti (bitiş: "
                        + r.getEndTime().format(DATE_TIME) + "). Lütfen en kısa sürede iade edin.", r);
    }

    /** Bekleme listesindeki aralık boşaldı; bildirimde rezervasyon formunu dolduracak bilgiler saklanır. */
    public void waitlistSlotAvailable(WaitlistEntry entry) {
        notificationRepository.save(new Notification(entry.getUser(), NotificationType.WAITLIST_SLOT_AVAILABLE,
                "Beklediğiniz aralık boşaldı",
                entry.getEquipment().getName() + " için bekleme listesine girdiğiniz aralık artık boş ("
                        + entry.getStartTime().format(DATE_TIME) + " - " + entry.getEndTime().format(DATE_TIME)
                        + "). Bekleme listesinden çıkarıldınız; yer kapılmadan rezervasyon yapabilirsiniz.")
                .about(entry.getEquipment(), null, entry.getStartTime(), entry.getEndTime()));
    }

    // ---------- Ekipman ----------

    /** Kalibrasyon tarihi yaklaşan / geçmiş ekipmanları yöneticilere bildirir (günlük iş). */
    public void calibrationDigest(List<Equipment> equipment) {
        if (equipment.isEmpty()) {
            return;
        }
        String names = String.join(", ", equipment.stream().limit(5).map(Equipment::getName).toList())
                + (equipment.size() > 5 ? " ..." : "");
        for (User admin : admins()) {
            notificationRepository.save(new Notification(admin, NotificationType.CALIBRATION_DUE,
                    "Kalibrasyon hatırlatması",
                    equipment.size() + " ekipmanın kalibrasyon tarihi yaklaşıyor veya geçti: " + names));
        }
    }

    // ---------- Hesap ----------

    /** Kullanıcı şifresini unuttuğunu bildirdi: yöneticilere haber verir. */
    public void passwordResetRequested(User user) {
        for (User admin : admins()) {
            notificationRepository.save(new Notification(admin, NotificationType.PASSWORD_RESET_REQUEST,
                    "Şifre sıfırlama talebi",
                    user.getFullName() + " (" + user.getEmail() + ") şifresini unuttuğunu bildirdi. "
                            + "Kullanıcılar sayfasındaki \"Şifre Talepleri\" sekmesinden tek kullanımlık bir şifre belirleyin."));
        }
    }

    // ---------- yardımcılar ----------

    /** Rezervasyonla ilgili bildirim oluşturur. */
    private void save(User user, NotificationType type, String title, String message, Reservation r) {
        notificationRepository.save(new Notification(user, type, title, message)
                .about(r.getEquipment(), r.getId(), r.getStartTime(), r.getEndTime()));
    }

    private List<User> admins() {
        return userRepository.findByRoleAndEnabledTrue(Role.ADMIN);
    }

    private static String period(Reservation r) {
        return "(" + r.getStartTime().format(DATE_TIME) + " - " + r.getEndTime().format(DATE_TIME) + ")";
    }

    private static String reason(String note) {
        return (note == null || note.isBlank()) ? "" : " Açıklama: " + note;
    }
}
