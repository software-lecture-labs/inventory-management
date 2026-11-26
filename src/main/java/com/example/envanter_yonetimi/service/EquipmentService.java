package com.example.envanter_yonetimi.service;

import com.example.envanter_yonetimi.model.Equipment;
import com.example.envanter_yonetimi.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Service
public class EquipmentService {

    @Autowired
    private final EquipmentRepository equipmentRepository;

    public EquipmentService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    public List<Equipment> getActiveEquipments() {
        return equipmentRepository.findByStatusTrue();
    }

    // Hepsi (status bakmaz)
    public List<Equipment> getAllEquipments() {
        return equipmentRepository.findAll();
    }

    public Equipment addEquipment(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    public Equipment getEquipmentById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ekipman bulunamadı: " + id));
    }

    public void softDeleteEquipment(Long id) {
        Equipment eq = getEquipmentById(id);
        eq.setStatus(false);
        equipmentRepository.save(eq);
    }


    // Statusü false olan ekipmanları getir
    public List<Equipment> getInactiveEquipment() {
        return equipmentRepository.findByStatusFalse();
    }

    public Equipment updateEquipment(Long id, Equipment updatedEquipment) {
        return equipmentRepository.findById(id)
                .map(equipment -> {
                    equipment.setBrand(updatedEquipment.getBrand());
                    equipment.setModel(updatedEquipment.getModel());
                    equipment.setEquipmentName(updatedEquipment.getEquipmentName());
                    equipment.setSerialNumber(updatedEquipment.getSerialNumber());
                    equipment.setCalibrationDate(updatedEquipment.getCalibrationDate());
                    equipment.setDescription(updatedEquipment.getDescription());
                    return equipmentRepository.save(equipment);
                })
                .orElseThrow(() -> new RuntimeException("Ekipman bulunamadı: " + id));
    }
}
