package com.inventory.model;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {
    PENDING,      // Yönetici onayı bekliyor
    APPROVED,     // Onaylandı, henüz teslim alınmadı
    CHECKED_OUT,  // Ekipman teslim alındı
    RETURNED,     // Ekipman iade edildi
    REJECTED,     // Yönetici reddetti
    CANCELLED;    // İptal edildi

    /** Takvimde yer kaplayan (çakışma kontrolüne giren) durumlar. */
    public static final Set<ReservationStatus> BLOCKING = EnumSet.of(PENDING, APPROVED, CHECKED_OUT);
}
