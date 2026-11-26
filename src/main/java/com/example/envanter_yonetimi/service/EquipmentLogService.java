package com.example.envanter_yonetimi.service;

import com.example.envanter_yonetimi.model.EquipmentLog;
import com.example.envanter_yonetimi.repository.EquipmentLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipmentLogService {

    private final EquipmentLogRepository equipmentLogRepository;

    public EquipmentLogService(EquipmentLogRepository equipmentLogRepository) {
        this.equipmentLogRepository = equipmentLogRepository;
    }

    // Tüm logları getir
    public List<EquipmentLog> getAllLogs() {
        return equipmentLogRepository.findAll();
    }

    // ID’ye göre log getir
    public EquipmentLog getLogById(Long id) {
        return equipmentLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log bulunamadı: " + id));
    }

    // Manuel log kaydet (gerekirse)
    public EquipmentLog saveLog(EquipmentLog log) {
        return equipmentLogRepository.save(log);
    }
}
