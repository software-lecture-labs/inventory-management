package com.inventory.repository;

import com.inventory.model.PasswordResetRequest;
import com.inventory.model.PasswordResetRequest.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {

    boolean existsByUserIdAndStatus(Long userId, Status status);

    List<PasswordResetRequest> findByUserIdAndStatus(Long userId, Status status);

    @Query("SELECT r FROM PasswordResetRequest r JOIN FETCH r.user WHERE r.status = :status ORDER BY r.createdAt")
    List<PasswordResetRequest> findWithUserByStatus(Status status);

    long countByStatus(Status status);
}
