package com.example.envanter_yonetimi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "delete_log")
public class DeleteLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Güncelleme yapılan ekipmanın ID’si
    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    // Değişen kolonların virgülle ayrılmış listesi
    @Column(name = "equipment_name")
    private String equipmentName;

    // Değişen kolonların eski değerleri
    @Column(name = "employee_name")
    private String employeeName;

    // Değişen kolonların yeni değerleri
    @CreationTimestamp
    @Column(name = "deleted_date", nullable = false, updatable = false)
    private LocalDateTime deletedDate;


    // Güncellemenin yapıldığı tarih/saat
    @Column(name = "description")
    private String description;
    

    // ----- Getter ve Setter -----

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEquipmentId(){
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId){
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public LocalDateTime getDeletedDate(){
        return deletedDate;
    }

    public void setDeletedDate(LocalDateTime deletedDate){
        this.deletedDate = deletedDate;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }
}
