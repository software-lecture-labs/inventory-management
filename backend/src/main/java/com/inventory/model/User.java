package com.inventory.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    // false = yönetici tarafından engellenmiş; giriş yapamaz
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean enabled = true;

    // Art arda yapılan hatalı giriş denemesi sayısı (başarılı girişte sıfırlanır)
    @ColumnDefault("0")
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    // Bu zamana kadar giriş yapılamaz (çok fazla hatalı deneme)
    private LocalDateTime lockedUntil;

    // true = yöneticinin verdiği tek kullanımlık şifreyle giriş yaptı, yeni şifre belirlemeden devam edemez
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean mustChangePassword = false;

    // Tek kullanımlık şifrenin geçerlilik sonu
    private LocalDateTime temporaryPasswordExpiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public LocalDateTime getTemporaryPasswordExpiresAt() { return temporaryPasswordExpiresAt; }
    public void setTemporaryPasswordExpiresAt(LocalDateTime t) { this.temporaryPasswordExpiresAt = t; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
