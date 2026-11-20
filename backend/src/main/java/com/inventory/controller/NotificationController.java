package com.inventory.controller;

import com.inventory.dto.NotificationResponse;
import com.inventory.dto.PageResponse;
import com.inventory.security.UserPrincipal;
import com.inventory.service.InboxService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Oturumdaki kullanıcının uygulama içi bildirimleri. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final InboxService inboxService;

    public NotificationController(InboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return inboxService.list(principal.id(), Paging.page(page), Paging.size(size));
    }

    /** Zil üzerindeki sayı için (arayüz düzenli aralıklarla sorar). */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("count", inboxService.unreadCount(principal.id()));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return inboxService.markRead(id, principal.id());
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("updated", inboxService.markAllRead(principal.id()));
    }
}
