package com.example.Application.service;

import com.example.Application.entity.EmergencyLog;
import com.example.Application.entity.Visitor;
import com.example.Application.repository.EmergencyLogRepository;
import com.example.Application.repository.VisitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmergencyService {

    @Autowired private EmergencyLogRepository emergencyLogRepository;
    @Autowired private VisitorRepository visitorRepository;
    @Autowired private AuditService auditService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    private static final Map<String, String> EVACUATION_ROUTES = Map.of(
        "1F", "Floor 1 → Main Lobby → Emergency Exit A (North) or Exit B (South)",
        "2F", "Floor 2 → Stairwell West → Ground Floor → Emergency Exit C",
        "3F", "Floor 3 → Stairwell East → Ground Floor → Emergency Exit A",
        "4F", "Floor 4 → Stairwell West → Ground Floor → Assembly Point D",
        "BF", "Basement → Emergency Stairs → Ground Floor → Exit A or B",
        "G",  "Ground Floor → Direct to Assembly Point (North Lawn)"
    );

    /**
     * Activate emergency protocol — records event, counts active visitors, broadcasts alerts.
     */
    public EmergencyLog activateEmergency(String type, String severity,
                                           String triggeredBy, String description,
                                           String affectedZones) {
        List<Visitor> activeVisitors = visitorRepository.findActiveVisitors();
        int total = activeVisitors.size();

        EmergencyLog log = EmergencyLog.builder()
                .emergencyType(type)
                .severity(severity != null ? severity : "HIGH")
                .triggeredBy(triggeredBy)
                .description(description)
                .affectedZones(affectedZones)
                .totalVisitors(total)
                .evacuatedCount(0)
                .missingCount(total)
                .status("ACTIVE")
                .triggeredAt(LocalDateTime.now())
                .build();

        log = emergencyLogRepository.save(log);

        auditService.log("EMERGENCY_ACTIVATED", "EMERGENCY", log.getId(),
                triggeredBy, "ADMIN", type + " emergency activated: " + description);

        // Broadcast emergency alert via WebSocket
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "EMERGENCY");
        alert.put("emergencyType", type);
        alert.put("severity", severity);
        alert.put("description", description);
        alert.put("affectedZones", affectedZones);
        alert.put("totalVisitors", total);
        alert.put("evacuationRoutes", EVACUATION_ROUTES);
        alert.put("triggeredAt", log.getTriggeredAt().toString());
        alert.put("logId", log.getId());

        messagingTemplate.convertAndSend("/topic/emergency", alert);
        messagingTemplate.convertAndSend("/topic/notifications", Map.of(
                "title", "🚨 EMERGENCY — " + type,
                "message", description != null ? description : "Evacuate immediately via nearest exit",
                "type", "EMERGENCY"
        ));

        return log;
    }

    /**
     * Update evacuation progress — mark a visitor as safely evacuated.
     */
    public EmergencyLog recordEvacuation(Long emergencyId, int evacuatedCount) {
        EmergencyLog log = emergencyLogRepository.findById(emergencyId)
                .orElseThrow(() -> new RuntimeException("Emergency log not found: " + emergencyId));

        log.setEvacuatedCount(evacuatedCount);
        log.setMissingCount(Math.max(0, log.getTotalVisitors() - evacuatedCount));
        log = emergencyLogRepository.save(log);

        Map<String, Object> update = new HashMap<>();
        update.put("type", "EVACUATION_UPDATE");
        update.put("logId", emergencyId);
        update.put("evacuated", evacuatedCount);
        update.put("missing", log.getMissingCount());
        messagingTemplate.convertAndSend("/topic/emergency", update);

        return log;
    }

    /**
     * Resolve / stand-down an emergency.
     */
    public EmergencyLog resolveEmergency(Long emergencyId, String resolvedBy) {
        EmergencyLog log = emergencyLogRepository.findById(emergencyId)
                .orElseThrow(() -> new RuntimeException("Emergency log not found: " + emergencyId));

        log.setStatus("RESOLVED");
        log.setResolvedBy(resolvedBy);
        log.setResolvedAt(LocalDateTime.now());
        log = emergencyLogRepository.save(log);

        auditService.log("EMERGENCY_RESOLVED", "EMERGENCY", emergencyId,
                resolvedBy, "ADMIN", log.getEmergencyType() + " emergency resolved");

        messagingTemplate.convertAndSend("/topic/emergency", Map.of(
                "type", "EMERGENCY_RESOLVED",
                "logId", emergencyId,
                "resolvedBy", resolvedBy,
                "resolvedAt", log.getResolvedAt().toString()
        ));
        messagingTemplate.convertAndSend("/topic/notifications", Map.of(
                "title", "✅ Emergency Resolved",
                "message", log.getEmergencyType() + " situation has been resolved",
                "type", "SUCCESS"
        ));

        return log;
    }

    /**
     * Return all active visitors with their last known location — for evacuation roll call.
     */
    public Map<String, Object> getEvacuationStatus() {
        List<Visitor> active = visitorRepository.findActiveVisitors();

        List<Map<String, Object>> visitorList = active.stream().map(v -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", v.getId());
            entry.put("name", v.getFullName());
            entry.put("company", v.getCompany());
            entry.put("status", v.getStatus().name());
            entry.put("assignedRoom", v.getAssignedRoomId());
            entry.put("hostName", v.getHostName());
            entry.put("phone", v.getPhone());
            entry.put("lastSeen", v.getUpdatedAt() != null ? v.getUpdatedAt().toString() : null);
            return entry;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("totalActive", active.size());
        result.put("visitors", visitorList);
        result.put("evacuationRoutes", EVACUATION_ROUTES);
        result.put("assemblyPoint", "North Lawn — Gate 1");
        result.put("emergencyContacts", List.of(
                Map.of("name", "Security Control", "phone", "100"),
                Map.of("name", "Fire Department",  "phone", "101"),
                Map.of("name", "Medical/Ambulance","phone", "108")
        ));
        return result;
    }

    public List<EmergencyLog> getActiveEmergencies() {
        return emergencyLogRepository.findByStatusOrderByTriggeredAtDesc("ACTIVE");
    }

    public List<EmergencyLog> getAllEmergencies() {
        return emergencyLogRepository.findAllOrderByTriggeredAtDesc();
    }

    public long countActive() {
        return emergencyLogRepository.countActive();
    }
}
