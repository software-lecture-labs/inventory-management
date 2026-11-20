package com.inventory.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Kullanıcının "Şifremi unuttum" talebi. Yönetici tek kullanımlık bir şifre belirleyerek
 * (RESOLVED) veya talebi yok sayarak (DISMISSED) kapatır.
 */
@Entity
@Table(name = "password_reset_requests")
public class PasswordResetRequest {

    public enum Status { PENDING, RESOLVED, DISMISSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @Column(length = 101)
    private String resolvedBy;

    protected PasswordResetRequest() {
    }

    public PasswordResetRequest(User user) {
        this.user = user;
    }

    public void close(Status status, String by, LocalDateTime at) {
        this.status = status;
        this.resolvedBy = by;
        this.resolvedAt = at;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Status getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
}
