package com.example.envanter_yonetimi.repository;

import com.example.envanter_yonetimi.model.DeleteLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeleteLogRepository extends JpaRepository<DeleteLog, Long> {
    // Ek sorgular eklenebilir
}
