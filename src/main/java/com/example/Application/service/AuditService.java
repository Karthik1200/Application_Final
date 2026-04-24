package com.example.Application.service;

import com.example.Application.entity.AuditLog;
import com.example.Application.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String eventType, String entityType, Long entityId,
                    String performedBy, String performedByRole, String description) {
        AuditLog log = AuditLog.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(performedBy)
                .performedByRole(performedByRole)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    public void log(String eventType, String entityType, Long entityId,
                    String performedBy, String performedByRole, String description,
                    String oldValue, String newValue) {
        AuditLog log = AuditLog.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(performedBy)
                .performedByRole(performedByRole)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }
}
