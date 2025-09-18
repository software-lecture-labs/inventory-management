package com.example.envanter_yonetimi.controller;

import com.example.envanter_yonetimi.model.DeleteLog;
import com.example.envanter_yonetimi.service.DeleteLogService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/equipment/delete")
@CrossOrigin(origins = "http://localhost:3000") // React için CORS
public class DeleteLogController {

    private final DeleteLogService deleteLogService;

    public DeleteLogController(DeleteLogService deleteLogService) {
        this.deleteLogService = deleteLogService;
    }

    // Tüm log kayıtlarını getir
    @GetMapping
    public List<DeleteLog> getAllLogs() {
        return deleteLogService.getAllLogs();
    }

    @PostMapping
    public DeleteLog createLog(@RequestBody DeleteLog log) {
        return deleteLogService.saveLog(log);
    }

}