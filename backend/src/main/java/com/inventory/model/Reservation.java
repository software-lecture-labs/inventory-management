package com.inventory.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservations_equipment_time", columnList = "equipment_id, startTime, endTime"),
        @Index(name = "idx_reservations_start", columnList = "startTime")
})
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // Bu alan eklenmeden önce oluşturulmuş rezervasyonlar onaylı kabul edilir
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'APPROVED'")
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.APPROVED;

    // Red / iptal gerekçesi ya da iade notu
    @Column(length = 500)
    private String note;

    private LocalDateTime checkedOutAt;

    private LocalDateTime returnedAt;

    // Başlamadan önceki hatırlatma bildirimi gönderildi mi
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean reminderSent = false;

    // Geç iade uyarısı gönderildi mi
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean overdueNotified = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Reservation() {
    }

    public Reservation(Equipment equipment, User user, LocalDateTime startTime, LocalDateTime endTime) {
        this.equipment = equipment;
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** Bitiş zamanı geçtiği halde ekipman hâlâ iade edilmemiş mi? */
    public boolean isOverdue(LocalDateTime now) {
        return status == ReservationStatus.CHECKED_OUT && now.isAfter(endTime);
    }

    /** Ekipman bitiş zamanından sonra mı iade edildi? */
    public boolean isReturnedLate() {
        return returnedAt != null && returnedAt.isAfter(endTime);
    }

    public Long getId() { return id; }
    public Equipment getEquipment() { return equipment; }
    public User getUser() { return user; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(LocalDateTime checkedOutAt) { this.checkedOutAt = checkedOutAt; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }

    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }

    public boolean isOverdueNotified() { return overdueNotified; }
    public void setOverdueNotified(boolean overdueNotified) { this.overdueNotified = overdueNotified; }
}
