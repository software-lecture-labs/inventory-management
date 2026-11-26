package com.example.envanter_yonetimi.repository;

import com.example.envanter_yonetimi.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;   // 👈 BUNU EKLEMEYİ UNUTMA

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByStatusTrue();
    List<Equipment> findAll();
    List<Equipment> findByStatusFalse();
}
