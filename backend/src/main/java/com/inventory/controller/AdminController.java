package com.inventory.controller;

import com.inventory.dto.AuditLogResponse;
import com.inventory.dto.AuthDtos.EnabledUpdateRequest;
import com.inventory.dto.AuthDtos.PasswordResetRequestResponse;
import com.inventory.dto.AuthDtos.TemporaryPasswordRequest;
import com.inventory.dto.AuthDtos.RoleUpdateRequest;
import com.inventory.dto.AuthDtos.UserResponse;
import com.inventory.dto.PageResponse;
import com.inventory.model.AuditAction;
import com.inventory.model.AuditEntityType;
import com.inventory.model.Role;
import com.inventory.security.UserPrincipal;
import com.inventory.service.AuditLogService;
import com.inventory.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Sadece yöneticilerin erişebildiği işlemler (yetki kontrolü SecurityConfig'te).
 */
@RestController
@RequestMapping("/api")
public class AdminController {

    private final UserAdminService userAdminService;
    private final AuditLogService auditLogService;

    public AdminController(UserAdminService userAdminService, AuditLogService auditLogService) {
        this.userAdminService = userAdminService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> getUsers(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) Role role,
                                               @RequestParam(required = false) Boolean enabled,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return userAdminService.search(new UserAdminService.Filter(q, role, enabled),
                Paging.page(page), Paging.size(size));
    }

    @PatchMapping("/users/{id}/role")
    public UserResponse changeRole(@PathVariable Long id,
                                   @Valid @RequestBody RoleUpdateRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return userAdminService.changeRole(id, request.role(), principal);
    }

    @PatchMapping("/users/{id}/enabled")
    public UserResponse setEnabled(@PathVariable Long id,
                                   @Valid @RequestBody EnabledUpdateRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return userAdminService.setEnabled(id, request.enabled(), principal);
    }

    // ---------- Şifre sıfırlama talepleri ----------

    @GetMapping("/users/password-requests")
    public List<PasswordResetRequestResponse> passwordRequests() {
        return userAdminService.pendingResetRequests();
    }

    @PostMapping("/users/{id}/temporary-password")
    public UserResponse setTemporaryPassword(@PathVariable Long id,
                                             @Valid @RequestBody TemporaryPasswordRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return userAdminService.setTemporaryPassword(id, request.password(), principal);
    }

    @PostMapping("/users/password-requests/{id}/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismissPasswordRequest(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        userAdminService.dismissResetRequest(id, principal);
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogResponse> getAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditLogService.search(new AuditLogService.Filter(action, entityType, q, from, to),
                Paging.page(page), Paging.size(size));
    }
}
