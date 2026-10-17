package com.example.envanter_yonetimi.service;

import com.example.envanter_yonetimi.model.Employee;
import com.example.envanter_yonetimi.model.Equipment;
import com.example.envanter_yonetimi.model.Rezervasyon;
import com.example.envanter_yonetimi.repository.RezervasyonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RezervasyonService {

    @Autowired
    private RezervasyonRepository rezervasyonRepository;

    public RezervasyonService(RezervasyonRepository rezervasyonRepository) {
        this.rezervasyonRepository = rezervasyonRepository;
    }
    
    public List<Rezervasyon> getAll() {
        return rezervasyonRepository.findAll();
    }

    public Rezervasyon addRezervasyon(Employee employee, Equipment equipment,
                                      LocalDateTime beginDate, LocalDateTime endDate) {
        // Tarih kontrolü
        if (beginDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Başlangıç tarihi bitiş tarihinden sonra olamaz.");
        }
        if (beginDate.isBefore(LocalDateTime.now()) || endDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Tarihler bugünden önce olamaz.");
        }

        // Çakışma kontrolü
        List<Rezervasyon> conflicts = rezervasyonRepository
                .findConflictingReservations(equipment, beginDate, endDate);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Bu tarihlerde ekipman zaten rezerve edilmiş.");
        }

        Rezervasyon rezervasyon = new Rezervasyon();
        rezervasyon.setEmployee(employee);
        rezervasyon.setEquipment(equipment);
        rezervasyon.setBeginDate(beginDate);
        rezervasyon.setEndDate(endDate);

        return rezervasyonRepository.save(rezervasyon);
    }
}
