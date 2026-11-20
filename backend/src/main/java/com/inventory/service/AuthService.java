package com.inventory.service;

import com.inventory.dto.AuthDtos.AuthResponse;
import com.inventory.dto.AuthDtos.LoginRequest;
import com.inventory.dto.AuthDtos.RegisterRequest;
import com.inventory.dto.AuthDtos.UserResponse;
import com.inventory.exception.BusinessException;
import com.inventory.model.PasswordResetRequest;
import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.repository.PasswordResetRequestRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtService;
import com.inventory.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetRequestRepository resetRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final Clock clock;
    private final int maxLoginAttempts;
    private final int lockMinutes;

    public AuthService(UserRepository userRepository,
                       PasswordResetRequestRepository resetRequestRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       NotificationService notificationService,
                       Clock clock,
                       @Value("${app.security.max-login-attempts:5}") int maxLoginAttempts,
                       @Value("${app.security.lock-minutes:15}") int lockMinutes) {
        this.userRepository = userRepository;
        this.resetRequestRepository = resetRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.clock = clock;
        this.maxLoginAttempts = maxLoginAttempts;
        this.lockMinutes = lockMinutes;
    }

    /**
     * Yeni kullanıcı kaydı. Kayıt olan herkes USER rolüyle başlar;
     * yönetici yetkisini sadece başka bir yönetici verebilir.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Bu email adresi zaten kayıtlı", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new BusinessException("Bu telefon numarası zaten kayıtlı", HttpStatus.CONFLICT);
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        return toAuthResponse(userRepository.save(user));
    }

    /**
     * Giriş. Art arda {maxLoginAttempts} hatalı denemeden sonra hesap {lockMinutes} dakika kilitlenir.
     * Yöneticinin verdiği tek kullanımlık şifreyle girilirse cevaptaki user.mustChangePassword true olur
     * ve kullanıcı yeni şifre belirlemeden başka işlem yapamaz.
     * <p>
     * Bilerek @Transactional değildir: hatalı deneme sayacı, hata fırlatılsa bile kaydedilmelidir.
     */
    public AuthResponse login(LoginRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Email veya şifre hatalı"));

        if (user.isLocked(now)) {
            throw lockedError(user.getLockedUntil(), now);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= maxLoginAttempts) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(now.plusMinutes(lockMinutes));
                userRepository.save(user);
                throw lockedError(user.getLockedUntil(), now);
            }
            user.setFailedLoginAttempts(attempts);
            userRepository.save(user);

            int remaining = maxLoginAttempts - attempts;
            if (remaining <= 2) {
                throw new BusinessException("Email veya şifre hatalı. Hesabınız kilitlenmeden önce "
                        + remaining + " deneme hakkınız kaldı", HttpStatus.UNAUTHORIZED);
            }
            throw new BadCredentialsException("Email veya şifre hatalı");
        }

        if (!user.isEnabled()) {
            throw new BusinessException("Hesabınız yönetici tarafından engellenmiş", HttpStatus.FORBIDDEN);
        }
        if (user.isMustChangePassword() && user.getTemporaryPasswordExpiresAt() != null
                && now.isAfter(user.getTemporaryPasswordExpiresAt())) {
            throw new BusinessException("Tek kullanımlık şifrenizin süresi doldu. "
                    + "\"Şifremi unuttum\" ile yöneticiden yeni bir şifre isteyin", HttpStatus.UNAUTHORIZED);
        }

        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        return toAuthResponse(user);
    }

    /**
     * "Şifremi unuttum": yöneticilere talep iletir. Email kayıtlı olmasa da aynı cevap döner;
     * böylece hangi adreslerin kayıtlı olduğu dışarıdan öğrenilemez.
     * Kullanıcının zaten bekleyen bir talebi varsa yenisi oluşturulmaz.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(email.trim())
                .filter(User::isEnabled)
                .filter(u -> !resetRequestRepository.existsByUserIdAndStatus(u.getId(), PasswordResetRequest.Status.PENDING))
                .ifPresent(user -> {
                    resetRequestRepository.save(new PasswordResetRequest(user));
                    notificationService.passwordResetRequested(user);
                });
    }

    /**
     * Şifre değiştirme. Tek kullanımlık şifreyle girmiş kullanıcıdan mevcut şifre istenmez
     * (az önce girdi); diğer durumlarda mevcut şifre doğru olmalıdır.
     */
    @Transactional
    public UserResponse changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));

        if (!user.isMustChangePassword()
                && (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword()))) {
            throw new BusinessException("Mevcut şifre hatalı");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("Yeni şifre, mevcut / tek kullanımlık şifreden farklı olmalıdır");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setTemporaryPasswordExpiresAt(null);
        return UserResponse.from(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(UserPrincipal.from(user));
        return new AuthResponse(token, UserResponse.from(user));
    }

    private static BusinessException lockedError(LocalDateTime lockedUntil, LocalDateTime now) {
        long minutes = Math.max(1, (Duration.between(now, lockedUntil).getSeconds() + 59) / 60);
        return new BusinessException("Çok fazla hatalı giriş denemesi. Hesabınız " + minutes
                + " dakika sonra tekrar açılacak", HttpStatus.TOO_MANY_REQUESTS);
    }
}
