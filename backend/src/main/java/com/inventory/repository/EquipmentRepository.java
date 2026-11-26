package com.inventory.repository;

import com.inventory.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

    List<Equipment> findByActiveTrueOrderByNameAsc();

    Optional<Equipment> findByIdAndActiveTrue(Long id);

    boolean existsBySerialNumberIgnoreCaseAndActiveTrue(String serialNumber);

    boolean existsBySerialNumberIgnoreCaseAndActiveTrueAndIdNot(String serialNumber, Long id);

    long countByActiveTrue();

    @Query("SELECT DISTINCT e.category FROM Equipment e WHERE e.active = true AND e.category IS NOT NULL ORDER BY e.category")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT e.location FROM Equipment e WHERE e.active = true AND e.location IS NOT NULL ORDER BY e.location")
    List<String> findDistinctLocations();

    /** Duruma göre ekipman sayıları: [EquipmentStatus, Long] */
    @Query("SELECT e.status, COUNT(e) FROM Equipment e WHERE e.active = true GROUP BY e.status")
    List<Object[]> countByStatus();

    /** Kalibrasyon tarihi verilen günden önce / o gün olan aktif ekipmanlar (en yakın önce). */
    @Query("""
            SELECT e FROM Equipment e
            WHERE e.active = true AND e.calibrationDate IS NOT NULL AND e.calibrationDate <= :date
            ORDER BY e.calibrationDate ASC
            """)
    List<Equipment> findCalibrationDueBy(@Param("date") LocalDate date);
}
