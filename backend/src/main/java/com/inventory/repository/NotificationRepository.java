package com.inventory.repository;

import com.inventory.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.user.id = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Okunmuş ve eskimiş bildirimleri temizler. */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.readAt IS NOT NULL AND n.createdAt < :before")
    int deleteReadBefore(@Param("before") LocalDateTime before);
}
