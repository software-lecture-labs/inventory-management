package com.example.envanter_yonetimi.controller;

import com.example.envanter_yonetimi.model.Equipment;
import com.example.envanter_yonetimi.model.Employee;
import com.example.envanter_yonetimi.repository.EmployeeRepository;
import com.example.envanter_yonetimi.service.EquipmentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    @Autowired
    private final EquipmentService equipmentService;
    @Autowired
    private final EmployeeRepository employeeRepository;

    public EquipmentController(EquipmentService equipmentService, EmployeeRepository employeeRepository) {
        this.equipmentService = equipmentService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/active")
    public List<Equipment> getActiveEquipments() {
        return equipmentService.getActiveEquipments();
    }

    // Tüm ekipmanlar (status bakmaz)
    @GetMapping("/all")
    public List<Equipment> getAllEquipments() {
        return equipmentService.getAllEquipments();
    }

    // Statusü false olan ekipmanları çek
    @GetMapping("/inactive")
    public List<Equipment> getInactiveEquipment() {
        return equipmentService.getInactiveEquipment();
    }

    @GetMapping("/{id}")
    public Equipment getById(@PathVariable Long id) {
        return equipmentService.getEquipmentById(id);
    }

    @PostMapping
    public Equipment addEquipment(@RequestParam Long employeeId, @RequestBody Equipment payload) throws Exception {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new Exception("Çalışan bulunamadı"));

        payload.setEmployee(employee);
        return equipmentService.addEquipment(payload);
    }

    @PutMapping("/{id}")
    public Equipment updateEquipment(@PathVariable Long id, @RequestBody Equipment equipment) {
        return equipmentService.updateEquipment(id, equipment);
    }

    @DeleteMapping("/{id}")
    public void deleteEquipment(@PathVariable Long id) {
        equipmentService.softDeleteEquipment(id);
    }
}
