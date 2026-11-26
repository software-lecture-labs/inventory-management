package com.example.envanter_yonetimi.controller;

import com.example.envanter_yonetimi.model.RezervasyonDTO;
import com.example.envanter_yonetimi.model.Employee;
import com.example.envanter_yonetimi.model.Equipment;
import com.example.envanter_yonetimi.model.Rezervasyon;
import com.example.envanter_yonetimi.repository.EmployeeRepository;
import com.example.envanter_yonetimi.repository.EquipmentRepository;
import com.example.envanter_yonetimi.service.RezervasyonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/rezervasyon")
public class RezervasyonController {

    @Autowired
    private RezervasyonService rezervasyonService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    public RezervasyonController(RezervasyonService rezervasyonService) {
        this.rezervasyonService = rezervasyonService;
    }

    
    @GetMapping
    public List<RezervasyonDTO> getAll() {
        return rezervasyonService.getAll()
                .stream()
                .map(RezervasyonDTO::new)
                .collect(Collectors.toList());
    }

    @PostMapping
    public RezervasyonDTO addRezervasyon(@RequestParam Long employeeId,
                                         @RequestParam Long equipmentId,
                                         @RequestBody Rezervasyon payload) throws Exception {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new Exception("Çalışan bulunamadı"));
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new Exception("Ekipman bulunamadı"));

        LocalDateTime beginDate = payload.getBeginDate();
        LocalDateTime endDate = payload.getEndDate();

        Rezervasyon saved = rezervasyonService.addRezervasyon(employee, equipment, beginDate, endDate);
        return new RezervasyonDTO(saved);
    }
}
