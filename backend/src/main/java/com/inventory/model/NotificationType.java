package com.inventory.model;

/** Uygulama içi bildirim türleri. */
public enum NotificationType {
    WAITLIST_SLOT_AVAILABLE,  // Bekleme listesindeki aralık boşaldı
    APPROVAL_REQUEST,         // (Yönetici) onay bekleyen yeni talep
    RESERVATION_APPROVED,
    RESERVATION_REJECTED,
    RESERVATION_CANCELLED,    // Başkası (yönetici) iptal etti
    RESERVATION_REMINDER,     // Rezervasyon yaklaşıyor
    RESERVATION_OVERDUE,      // İade süresi geçti
    CALIBRATION_DUE,          // (Yönetici) kalibrasyonu yaklaşan ekipmanlar
    PASSWORD_RESET_REQUEST    // (Yönetici) kullanıcı şifresini unuttu
}
