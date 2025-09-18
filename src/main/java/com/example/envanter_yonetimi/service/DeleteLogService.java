package com.example.envanter_yonetimi.service;

import com.example.envanter_yonetimi.model.DeleteLog;
import com.example.envanter_yonetimi.repository.DeleteLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeleteLogService {

    private final DeleteLogRepository deleteLogRepository;

    public DeleteLogService(DeleteLogRepository deleteLogRepository) {
        this.deleteLogRepository = deleteLogRepository;
    }

    public List<DeleteLog> getAllLogs() {
        return deleteLogRepository.findAll();
    }

    public DeleteLog saveLog(DeleteLog log) {
        return deleteLogRepository.save(log);
    }

    public DeleteLog getLogById(Long id) {
        return deleteLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log bulunamadı: " + id));
    }
}
