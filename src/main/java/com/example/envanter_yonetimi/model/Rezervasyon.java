package com.example.envanter_yonetimi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rezervasyon")
public class Rezervasyon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "begin_date", nullable = false)
    private LocalDateTime beginDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    public Rezervasyon() {}

    public Rezervasyon(Employee employee, Equipment equipment, LocalDateTime beginDate, LocalDateTime endDate) {
        this.employee = employee;
        this.equipment = equipment;
        this.beginDate = beginDate;
        this.endDate = endDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public LocalDateTime getBeginDate() { return beginDate; }
    public void setBeginDate(LocalDateTime beginDate) { this.beginDate = beginDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
}
