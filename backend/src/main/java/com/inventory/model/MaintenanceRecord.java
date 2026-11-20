package com.inventory.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bir ekipmana yapılmış bakım, kalibrasyon veya onarım kaydı.
 */
@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaintenanceType type;

    // İşlemin yapıldığı tarih
    @Column(nullable = false)
    private LocalDate performedDate;

    // Bir sonraki bakım / kalibrasyon tarihi (varsa)
    private LocalDate nextDueDate;

    // İşlemi yapan kişi veya firma
    @Column(length = 100)
    private String performedBy;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MaintenanceRecord() {
    }

    public MaintenanceRecord(Equipment equipment, MaintenanceType type, LocalDate performedDate,
                             LocalDate nextDueDate, String performedBy, String description, User createdBy) {
        this.equipment = equipment;
        this.type = type;
        this.performedDate = performedDate;
        this.nextDueDate = nextDueDate;
        this.performedBy = performedBy;
        this.description = description;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Equipment getEquipment() { return equipment; }
    public MaintenanceType getType() { return type; }
    public LocalDate getPerformedDate() { return performedDate; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public String getPerformedBy() { return performedBy; }
    public String getDescription() { return description; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
