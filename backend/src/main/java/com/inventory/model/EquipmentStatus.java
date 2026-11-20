package com.inventory.model;

/**
 * Ekipmanın fiziksel durumu. Sadece AVAILABLE durumundaki ekipmanlar rezerve edilebilir.
 */
public enum EquipmentStatus {
    AVAILABLE,      // Kullanılabilir
    OUT_OF_ORDER,   // Arızalı
    MAINTENANCE,    // Bakımda
    CALIBRATION     // Kalibrasyonda
}
