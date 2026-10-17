package com.example.envanter_yonetimi.repository;

import com.example.envanter_yonetimi.model.EquipmentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentLogRepository extends JpaRepository<EquipmentLog, Long> {
    // İstersen ekipmana göre log listeleme gibi ek sorgular da eklenebilir
}
