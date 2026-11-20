package com.inventory.repository;

import com.inventory.model.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    /** Bir ekipman için verilen aralıkla kesişen, henüz başlamamış bekleme kayıtları (ilk gelen önce). */
    @Query("""
            SELECT w FROM WaitlistEntry w JOIN FETCH w.user
            WHERE w.equipment.id = :equipmentId
              AND w.startTime < :to AND w.endTime > :from
              AND w.startTime > :now
            ORDER BY w.createdAt
            """)
    List<WaitlistEntry> findWaiting(@Param("equipmentId") Long equipmentId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    @Param("now") LocalDateTime now);

    @Query("""
            SELECT w FROM WaitlistEntry w JOIN FETCH w.equipment
            WHERE w.user.id = :userId AND w.endTime > :now
            ORDER BY w.startTime
            """)
    List<WaitlistEntry> findActiveForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    boolean existsByEquipmentIdAndUserIdAndStartTimeAndEndTime(Long equipmentId, Long userId,
                                                               LocalDateTime startTime, LocalDateTime endTime);

    /** Kullanıcının bu ekipman için verilen aralıkla çakışan bekleme kayıtlarını siler. */
    @Modifying
    @Query("""
            DELETE FROM WaitlistEntry w
            WHERE w.equipment.id = :equipmentId AND w.user.id = :userId
              AND w.startTime < :end AND w.endTime > :start
            """)
    int deleteForUserOverlapping(@Param("equipmentId") Long equipmentId,
                                 @Param("userId") Long userId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Modifying
    @Query("DELETE FROM WaitlistEntry w WHERE w.endTime < :time")
    int deleteExpired(@Param("time") LocalDateTime time);

    @Modifying
    @Query("DELETE FROM WaitlistEntry w WHERE w.equipment.id = :equipmentId")
    int deleteByEquipmentId(@Param("equipmentId") Long equipmentId);
}
