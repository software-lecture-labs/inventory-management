package com.example.envanter_yonetimi.repository;

import com.example.envanter_yonetimi.model.Rezervasyon;
import com.example.envanter_yonetimi.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RezervasyonRepository extends JpaRepository<Rezervasyon, Long> {

    // Çakışan rezervasyonları kontrol eden sorgu
    @Query("SELECT r FROM Rezervasyon r " +
       "WHERE r.equipment = :equipment " +
       "AND (:beginDate <= r.endDate AND :endDate >= r.beginDate)" +
       "AND r.beginDate <> r.endDate")

    List<Rezervasyon> findConflictingReservations(
            Equipment equipment,
            LocalDateTime beginDate,
            LocalDateTime endDate
    );
    List<Rezervasyon> findAll();
}
