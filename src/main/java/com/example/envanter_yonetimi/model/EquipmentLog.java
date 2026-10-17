package com.example.envanter_yonetimi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_log")
public class EquipmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Güncelleme yapılan ekipmanın ID’si
    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    // Güncelleme yapan çalışanın adı
    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    // Değişen kolonların virgülle ayrılmış listesi
    @Column(name = "changed_columns")
    private String changedColumns;

    // Değişen kolonların eski değerleri
    @Column(name = "old_values")
    private String oldValues;

    // Değişen kolonların yeni değerleri
    @Column(name = "new_values")
    private String newValues;

    // Güncellemenin yapıldığı tarih/saat
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ----- Getter ve Setter -----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getChangedColumns() {
        return changedColumns;
    }

    public void setChangedColumns(String changedColumns) {
        this.changedColumns = changedColumns;
    }

    public String getOldValues() {
        return oldValues;
    }

    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
