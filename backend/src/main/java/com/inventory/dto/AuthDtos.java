package com.inventory.dto;

import com.inventory.model.PasswordResetRequest;
import com.inventory.model.Role;
import com.inventory.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Kimlik doğrulama ve kullanıcı işlemlerinde kullanılan istek / cevap modelleri.
 */
public final class AuthDtos {

    // En az 8 karakter; büyük harf, küçük harf ve özel karakter
    static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$";

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Email zorunludur")
            @Email(message = "Geçerli bir email giriniz")
            String email,

            @NotBlank(message = "Şifre zorunludur")
            String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank(message = "Ad zorunludur")
            @Size(max = 50, message = "Ad en fazla 50 karakter olabilir")
            @Pattern(regexp = "^[\\p{L} ]+$", message = "Ad sadece harf içerebilir")
            String firstName,

            @NotBlank(message = "Soyad zorunludur")
            @Size(max = 50, message = "Soyad en fazla 50 karakter olabilir")
            @Pattern(regexp = "^[\\p{L} ]+$", message = "Soyad sadece harf içerebilir")
            String lastName,

            @NotBlank(message = "Email zorunludur")
            @Email(message = "Geçerli bir email giriniz")
            @Size(max = 100)
            String email,

            @NotBlank(message = "Telefon zorunludur")
            @Pattern(regexp = "^[0-9]{10,15}$", message = "Telefon 10-15 haneli olmalıdır")
            String phone,

            @NotBlank(message = "Şifre zorunludur")
            @Pattern(
                    regexp = PASSWORD_REGEX,
                    message = "Şifre en az 8 karakter olmalı; büyük harf, küçük harf ve özel karakter içermelidir"
            )
            String password
    ) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record UserResponse(
            Long id,
            String firstName,
            String lastName,
            String fullName,
            String email,
            String phone,
            Role role,
            boolean enabled,
            // Hatalı denemeler nedeniyle kilitliyse kilidin açılacağı zaman, değilse null
            LocalDateTime lockedUntil,
            // true = tek kullanımlık şifreyle girdi, yeni şifre belirlemeli
            boolean mustChangePassword,
            LocalDateTime createdAt
    ) {
        public static UserResponse from(User user) {
            LocalDateTime now = LocalDateTime.now();
            return new UserResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.isEnabled(),
                    user.isLocked(now) ? user.getLockedUntil() : null,
                    user.isMustChangePassword(),
                    user.getCreatedAt()
            );
        }
    }

    public record RoleUpdateRequest(
            @NotNull(message = "Rol zorunludur")
            Role role
    ) {
    }

    public record EnabledUpdateRequest(
            @NotNull(message = "Durum zorunludur")
            Boolean enabled
    ) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email zorunludur")
            @Email(message = "Geçerli bir email giriniz")
            String email
    ) {
    }

    /** Yöneticinin belirlediği tek kullanımlık şifre. */
    public record TemporaryPasswordRequest(
            @NotBlank(message = "Şifre zorunludur")
            @Size(min = 8, max = 64, message = "Tek kullanımlık şifre 8-64 karakter olmalıdır")
            String password
    ) {
    }

    public record ChangePasswordRequest(
            // Tek kullanımlık şifreyle girildiyse boş bırakılabilir
            String currentPassword,

            @NotBlank(message = "Yeni şifre zorunludur")
            @Pattern(
                    regexp = PASSWORD_REGEX,
                    message = "Şifre en az 8 karakter olmalı; büyük harf, küçük harf ve özel karakter içermelidir"
            )
            String newPassword
    ) {
    }

    public record PasswordResetRequestResponse(
            Long id,
            Long userId,
            String fullName,
            String email,
            LocalDateTime createdAt
    ) {
        public static PasswordResetRequestResponse from(PasswordResetRequest r) {
            return new PasswordResetRequestResponse(r.getId(), r.getUser().getId(), r.getUser().getFullName(),
                    r.getUser().getEmail(), r.getCreatedAt());
        }
    }

    public record MessageResponse(String message) {
    }
}
