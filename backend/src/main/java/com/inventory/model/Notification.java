package com.inventory.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Kullanıcıya gösterilen uygulama içi bildirim (üst bardaki zil).
 * İlgili ekipman ve zaman aralığı varsa saklanır; böylece bildirimden doğrudan rezervasyon formu açılabilir.
 */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_user", columnList = "user_id, readAt"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    // İlgili kayıtlar (varsa) - silinseler bile bildirim kalsın diye yabancı anahtar değil, sadece kimlik
    private Long equipmentId;

    @Column(length = 100)
    private String equipmentName;

    private Long reservationId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    // null = okunmadı
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Notification() {
    }

    public Notification(User user, NotificationType type, String title, String message) {
        this.user = user;
        this.type = type;
        this.title = truncate(title, 150);
        this.message = truncate(message, 1000);
    }

    /** Bildirimi bir ekipman ve zaman aralığıyla ilişkilendirir. */
    public Notification about(Equipment equipment, Long reservationId, LocalDateTime start, LocalDateTime end) {
        this.equipmentId = equipment.getId();
        this.equipmentName = truncate(equipment.getName(), 100);
        this.reservationId = reservationId;
        this.startTime = start;
        this.endTime = end;
        return this;
    }

    private static String truncate(String value, int max) {
        return (value == null || value.length() <= max) ? value : value.substring(0, max);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Long getEquipmentId() { return equipmentId; }
    public String getEquipmentName() { return equipmentName; }
    public Long getReservationId() { return reservationId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
