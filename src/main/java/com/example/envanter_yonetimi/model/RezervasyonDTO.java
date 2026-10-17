package com.example.envanter_yonetimi.model;

import com.example.envanter_yonetimi.model.Rezervasyon;
import java.time.LocalDateTime;

public class RezervasyonDTO {
    private Long id;
    private String employeeName;
    private String equipmentName;   // ekipman adı
    private String serialNumber;    // seri numarası
    private LocalDateTime beginDate;
    private LocalDateTime endDate;

    public RezervasyonDTO(Rezervasyon rez) {
        this.id = rez.getId();
        this.employeeName = rez.getEmployee().getFirstName() + " " + rez.getEmployee().getLastName();
        this.equipmentName = rez.getEquipment().getEquipmentName();
        this.serialNumber = rez.getEquipment().getSerialNumber();
        this.beginDate = rez.getBeginDate();
        this.endDate = rez.getEndDate();
    }

    // getterlar
    public Long getId() { return id; }
    public String getEmployeeName() { return employeeName; }
    public String getEquipmentName() { return equipmentName; }
    public String getSerialNumber() { return serialNumber; }
    public LocalDateTime getBeginDate() { return beginDate; }
    public LocalDateTime getEndDate() { return endDate; }
}
