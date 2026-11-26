package com.inventory.service;

import com.inventory.dto.NotificationResponse;
import com.inventory.dto.PageResponse;
import com.inventory.exception.NotFoundException;
import com.inventory.model.Notification;
import com.inventory.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Kullanıcının kendi uygulama içi bildirimleri: listeleme ve okundu işaretleme.
 * Bildirimleri oluşturan taraf NotificationService'tir.
 */
@Service
public class InboxService {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public InboxService(NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.from(notificationRepository.findByUserId(userId, pageable), NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        Notification n = notificationRepository.findById(id)
                .filter(x -> x.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Bildirim bulunamadı"));
        if (n.getReadAt() == null) {
            n.setReadAt(LocalDateTime.now(clock));
        }
        return NotificationResponse.from(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId, LocalDateTime.now(clock));
    }

    /** 30 günden eski, okunmuş bildirimleri siler (günlük iş). */
    @Transactional
    public int cleanup() {
        return notificationRepository.deleteReadBefore(LocalDateTime.now(clock).minusDays(30));
    }
}
