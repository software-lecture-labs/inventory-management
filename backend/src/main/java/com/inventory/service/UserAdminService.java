package com.inventory.service;

import com.inventory.dto.AuthDtos.PasswordResetRequestResponse;
import com.inventory.dto.AuthDtos.UserResponse;
import com.inventory.exception.NotFoundException;
import com.inventory.model.PasswordResetRequest;
import com.inventory.repository.PasswordResetRequestRepository;
import com.inventory.dto.PageResponse;
import com.inventory.exception.BusinessException;
import com.inventory.model.AuditAction;
import com.inventory.model.AuditEntityType;
import com.inventory.model.FieldChange;
import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.repository.UserRepository;
import com.inventory.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Yöneticinin kullanıcılar üzerinde yaptığı işlemler: listeleme, rol değiştirme, engelleme.
 */
@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final ReservationService reservationService;
    private final AuditLogService auditLogService;
    private final PasswordResetRequestRepository resetRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final int temporaryPasswordHours;

    public UserAdminService(UserRepository userRepository,
                            UserService userService,
                            ReservationService reservationService,
                            AuditLogService auditLogService,
                            PasswordResetRequestRepository resetRequestRepository,
                            PasswordEncoder passwordEncoder,
                            Clock clock,
                            @Value("${app.security.temporary-password-hours:24}") int temporaryPasswordHours) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.reservationService = reservationService;
        this.auditLogService = auditLogService;
        this.resetRequestRepository = resetRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.temporaryPasswordHours = temporaryPasswordHours;
    }

    /** Arama kriterleri; boş (null) alanlar filtrelemeye katılmaz. */
    public record Filter(String q, Role role, Boolean enabled) {
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(Filter filter, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("firstName", "lastName", "id"));
        return PageResponse.from(userRepository.findAll(toSpec(filter), pageable), UserResponse::from);
    }

    @Transactional
    public UserResponse changeRole(Long userId, Role newRole, UserPrincipal admin) {
        if (userId.equals(admin.id())) {
            throw new BusinessException("Kendi rolünüzü değiştiremezsiniz");
        }
        User user = userService.getById(userId);
        Role old = user.getRole();
        if (old != newRole) {
            user.setRole(newRole);
            auditLogService.record(AuditAction.ROLE_CHANGE, AuditEntityType.USER, user.getId(), user.getFullName(),
                    admin.fullName(), List.of(new FieldChange("role", old.name(), newRole.name())), null);
        }
        return UserResponse.from(user);
    }

    /**
     * Kullanıcıyı engeller veya engelini kaldırır.
     * Engellenen kullanıcı giriş yapamaz, mevcut oturumu da geçersiz olur
     * ve ileri tarihli rezervasyonları iptal edilir.
     * Engel kaldırılırken hatalı giriş kilidi de sıfırlanır.
     */
    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled, UserPrincipal admin) {
        if (userId.equals(admin.id())) {
            throw new BusinessException("Kendi hesabınızı engelleyemezsiniz");
        }
        User user = userService.getById(userId);
        if (user.isEnabled() == enabled) {
            if (enabled && user.getLockedUntil() != null) {
                // Sadece kilit açma
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
            }
            return UserResponse.from(user);
        }

        user.setEnabled(enabled);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        String note = null;
        if (!enabled) {
            int cancelled = reservationService.cancelFutureForUser(user.getId(), admin.fullName(),
                    "Kullanıcı hesabı engellendi");
            note = cancelled > 0 ? cancelled + " ileri tarihli rezervasyon iptal edildi" : null;
        }
        auditLogService.record(enabled ? AuditAction.ENABLE : AuditAction.DISABLE, AuditEntityType.USER,
                user.getId(), user.getFullName(), admin.fullName(), note);
        return UserResponse.from(user);
    }

    // ================= Şifre sıfırlama =================

    @Transactional(readOnly = true)
    public List<PasswordResetRequestResponse> pendingResetRequests() {
        return resetRequestRepository.findWithUserByStatus(PasswordResetRequest.Status.PENDING).stream()
                .map(PasswordResetRequestResponse::from)
                .toList();
    }

    /**
     * Kullanıcıya tek kullanımlık şifre belirler. Kullanıcının eski şifresi geçersiz olur;
     * bu şifreyle girdiğinde yeni şifre belirlemeden devam edemez. Bekleyen talepleri kapatılır.
     */
    @Transactional
    public UserResponse setTemporaryPassword(Long userId, String password, UserPrincipal admin) {
        if (userId.equals(admin.id())) {
            throw new BusinessException("Kendi şifrenizi buradan sıfırlayamazsınız");
        }
        User user = userService.getById(userId);
        if (!user.isEnabled()) {
            throw new BusinessException("Engellenmiş bir kullanıcının şifresi sıfırlanamaz; önce engelini kaldırın");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        user.setPassword(passwordEncoder.encode(password));
        user.setMustChangePassword(true);
        user.setTemporaryPasswordExpiresAt(now.plusHours(temporaryPasswordHours));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        resetRequestRepository.findByUserIdAndStatus(userId, PasswordResetRequest.Status.PENDING)
                .forEach(r -> r.close(PasswordResetRequest.Status.RESOLVED, admin.fullName(), now));

        auditLogService.record(AuditAction.PASSWORD_RESET, AuditEntityType.USER, user.getId(), user.getFullName(),
                admin.fullName(), "Tek kullanımlık şifre verildi (" + temporaryPasswordHours + " saat geçerli)");
        return UserResponse.from(user);
    }

    /** Talebi şifre vermeden kapatır (ör. kullanıcı şifresini hatırladı). */
    @Transactional
    public void dismissResetRequest(Long requestId, UserPrincipal admin) {
        PasswordResetRequest request = resetRequestRepository.findById(requestId)
                .filter(r -> r.getStatus() == PasswordResetRequest.Status.PENDING)
                .orElseThrow(() -> new NotFoundException("Bekleyen talep bulunamadı"));
        request.close(PasswordResetRequest.Status.DISMISSED, admin.fullName(), LocalDateTime.now(clock));
    }

    private static Specification<User> toSpec(Filter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(root.get("phone"), like)));
            }
            if (f.role() != null) {
                predicates.add(cb.equal(root.get("role"), f.role()));
            }
            if (f.enabled() != null) {
                predicates.add(cb.equal(root.get("enabled"), f.enabled()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
