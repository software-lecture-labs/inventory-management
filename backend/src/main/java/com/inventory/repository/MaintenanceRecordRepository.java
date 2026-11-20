package com.inventory.repository;

import com.inventory.model.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByEquipmentIdOrderByPerformedDateDescIdDesc(Long equipmentId);
}
