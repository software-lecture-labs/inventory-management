package com.inventory.controller;

import com.inventory.dto.AuthDtos.AuthResponse;
import com.inventory.dto.AuthDtos.ChangePasswordRequest;
import com.inventory.dto.AuthDtos.ForgotPasswordRequest;
import com.inventory.dto.AuthDtos.LoginRequest;
import com.inventory.dto.AuthDtos.MessageResponse;
import com.inventory.dto.AuthDtos.RegisterRequest;
import com.inventory.dto.AuthDtos.UserResponse;
import com.inventory.security.UserPrincipal;
import com.inventory.service.AuthService;
import com.inventory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Email kayıtlı olsun ya da olmasın aynı cevap döner
    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return new MessageResponse("Talebiniz yöneticiye iletildi. Yönetici size tek kullanımlık bir şifre verecek; "
                + "bu şifreyle giriş yaptığınızda yeni şifrenizi belirleyeceksiniz.");
    }

    /** Şifre değiştirme (tek kullanımlık şifreyle girildiyse zorunlu adım). */
    @PostMapping("/change-password")
    public UserResponse changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return authService.changePassword(principal.id(), request.currentPassword(), request.newPassword());
    }

    // Token'a ait güncel kullanıcı bilgisi (sayfa yenilendiğinde rol kontrolü için)
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getProfile(principal.id());
    }
}
