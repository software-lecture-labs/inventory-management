package com.inventory.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sistemde yapılan önemli işlemlerin kaydı (ekipman, rezervasyon, kullanıcı, bakım).
 * <p>
 * Not: entityId / entityName alanları, eski sürümle uyumlu kalmak için
 * veritabanında equipment_id / equipment_name kolonlarında tutulur.
 */
@Entity
@Table(name = "audit_logs", indexes = @Index(name = "idx_audit_logs_created_at", columnList = "createdAt"))
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    // Eski kayıtların hepsi ekipman kaydıdır
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'EQUIPMENT'")
    @Column(nullable = false, length = 20)
    private AuditEntityType entityType = AuditEntityType.EQUIPMENT;

    @Column(name = "equipment_id", nullable = false)
    private Long entityId;

    @Column(name = "equipment_name", nullable = false, length = 100)
    private String entityName;

    @Column(nullable = false, length = 101)
    private String performedBy;

    // Red gerekçesi, iade notu gibi ek açıklama
    @Column(length = 500)
    private String note;

    @ElementCollection
    @CollectionTable(name = "audit_log_changes", joinColumns = @JoinColumn(name = "audit_log_id"))
    private List<FieldChange> changes = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(AuditAction action, AuditEntityType entityType, Long entityId, String entityName,
                    String performedBy, List<FieldChange> changes, String note) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.entityName = truncate(entityName, 100);
        this.performedBy = truncate(performedBy, 101);
        this.changes = new ArrayList<>(changes);
        this.note = truncate(note, 500);
    }

    private static String truncate(String value, int max) {
        return (value == null || value.length() <= max) ? value : value.substring(0, max);
    }

    public Long getId() { return id; }
    public AuditAction getAction() { return action; }
    public AuditEntityType getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getEntityName() { return entityName; }
    public String getPerformedBy() { return performedBy; }
    public String getNote() { return note; }
    public List<FieldChange> getChanges() { return changes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
