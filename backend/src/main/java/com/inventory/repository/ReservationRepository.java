package com.inventory.repository;

import com.inventory.model.Reservation;
import com.inventory.model.ReservationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    /**
     * Aynı ekipman için [start, end) aralığıyla çakışan, takvimde yer kaplayan bir rezervasyon var mı?
     * İki aralık, biri diğeri bitmeden başlıyorsa çakışır.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reservation r
            WHERE r.equipment.id = :equipmentId
              AND r.status IN :statuses
              AND r.startTime < :end
              AND r.endTime > :start
            """)
    boolean existsOverlapping(@Param("equipmentId") Long equipmentId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end,
                              @Param("statuses") Collection<ReservationStatus> statuses);

    /** Takvim için: verilen aralıkla kesişen rezervasyonlar. */
    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.equipment JOIN FETCH r.user
            WHERE r.startTime < :to AND r.endTime > :from
              AND r.status IN :statuses
            ORDER BY r.startTime
            """)
    List<Reservation> findInRange(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("statuses") Collection<ReservationStatus> statuses);

    /** Bir ekipmanın verilen zamandan sonra başlayan, hâlâ geçerli rezervasyonları. */
    List<Reservation> findByEquipmentIdAndStatusInAndStartTimeAfter(Long equipmentId,
                                                                     Collection<ReservationStatus> statuses,
                                                                     LocalDateTime time);

    /** Bir kullanıcının verilen zamandan sonra başlayan, hâlâ geçerli rezervasyonları. */
    List<Reservation> findByUserIdAndStatusInAndStartTimeAfter(Long userId,
                                                                Collection<ReservationStatus> statuses,
                                                                LocalDateTime time);

    /** Hatırlatma gönderilecekler: [from, to) arasında başlayan onaylı rezervasyonlar. */
    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.equipment JOIN FETCH r.user
            WHERE r.status = com.inventory.model.ReservationStatus.APPROVED
              AND r.reminderSent = false
              AND r.startTime >= :from AND r.startTime < :to
            """)
    List<Reservation> findNeedingReminder(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** İade zamanı geçmiş, henüz uyarı gönderilmemiş rezervasyonlar. */
    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.equipment JOIN FETCH r.user
            WHERE r.status = com.inventory.model.ReservationStatus.CHECKED_OUT
              AND r.overdueNotified = false
              AND r.endTime < :now
            """)
    List<Reservation> findOverdueNotNotified(@Param("now") LocalDateTime now);

    long countByStatus(ReservationStatus status);

    long countByStatusAndEndTimeBefore(ReservationStatus status, LocalDateTime time);

    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.status IN :statuses AND r.startTime < :to AND r.endTime > :from
            """)
    long countInRange(@Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to,
                      @Param("statuses") Collection<ReservationStatus> statuses);

    /** En çok rezerve edilen ekipmanlar: [equipmentId, equipmentName, count] */
    @Query("""
            SELECT r.equipment.id, r.equipment.name, COUNT(r) FROM Reservation r
            WHERE r.startTime >= :from AND r.status IN :statuses
            GROUP BY r.equipment.id, r.equipment.name
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> findTopEquipment(@Param("from") LocalDateTime from,
                                    @Param("statuses") Collection<ReservationStatus> statuses,
                                    Pageable pageable);

    @Query("""
            SELECT r.startTime FROM Reservation r
            WHERE r.startTime >= :from AND r.startTime < :to AND r.status IN :statuses
            """)
    List<LocalDateTime> findStartTimes(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       @Param("statuses") Collection<ReservationStatus> statuses);

    @Query("""
            SELECT r FROM Reservation r JOIN FETCH r.equipment
            WHERE r.user.id = :userId AND r.status IN :statuses AND r.endTime > :now
            ORDER BY r.startTime
            """)
    List<Reservation> findUpcomingForUser(@Param("userId") Long userId,
                                          @Param("statuses") Collection<ReservationStatus> statuses,
                                          @Param("now") LocalDateTime now,
                                          Pageable pageable);
}
