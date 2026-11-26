package com.example.envanter_yonetimi.controller;

import com.example.envanter_yonetimi.model.EquipmentLog;
import com.example.envanter_yonetimi.service.EquipmentLogService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/equipment/logs")
@CrossOrigin(origins = "http://localhost:3000") // React için CORS
public class EquipmentLogController {

    private final EquipmentLogService equipmentLogService;

    public EquipmentLogController(EquipmentLogService equipmentLogService) {
        this.equipmentLogService = equipmentLogService;
    }

    // Tüm logları getir
    @GetMapping
    public List<EquipmentLog> getAllLogs() {
        return equipmentLogService.getAllLogs();
    }

    // Tek bir logun detaylarını getir
    @GetMapping("/{id}")
    public List<Map<String, String>> getLogDetails(@PathVariable Long id) {
        EquipmentLog log = equipmentLogService.getLogById(id);

        String[] columns = log.getChangedColumns() != null ? log.getChangedColumns().split(",") : new String[0];
        String[] olds = log.getOldValues() != null ? log.getOldValues().split(",") : new String[0];
        String[] news = log.getNewValues() != null ? log.getNewValues().split(",") : new String[0];

        List<Map<String, String>> details = new ArrayList<>();
        for (int i = 0; i < columns.length; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("field", columns[i].trim());
            row.put("oldValue", i < olds.length ? olds[i].trim() : "");
            row.put("newValue", i < news.length ? news[i].trim() : "");
            row.put("employeeName", log.getEmployeeName()); // ekstra
            row.put("updatedAt", log.getUpdatedAt() != null ? log.getUpdatedAt().toString() : "");
            details.add(row);
        }
        return details;
    }

    // Manuel log ekleme (çok gerekmez ama dursun)
    @PostMapping
    public EquipmentLog createLog(@RequestBody EquipmentLog log) {
        return equipmentLogService.saveLog(log);
    }
}
