package com.example.Application.service;

import com.example.Application.entity.GateLog;
import com.example.Application.entity.Visitor;
import com.example.Application.enums.VisitorStatus;
import com.example.Application.repository.GateLogRepository;
import com.example.Application.repository.VisitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GateService {

    @Autowired private GateLogRepository gateLogRepository;
    @Autowired private VisitorRepository visitorRepository;
    @Autowired private QRCodeService qrCodeService;
    @Autowired private AuditService auditService;
    @Autowired private NotificationService notificationService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    /**
     * Validate QR code at gate kiosk and log entry
     */
    public Map<String, Object> scanQRAtGate(String qrData, String gateId, String guardUsername) {
        Map<String, Object> result = new HashMap<>();

        // Validate QR
        Map<String, Object> qrValidation = qrCodeService.validateQRPass(qrData);

        if (!(boolean) qrValidation.getOrDefault("valid", false)) {
            result.put("success", false);
            result.put("message", "Invalid or expired QR code");

            // Log failed attempt
            gateLogRepository.save(GateLog.builder()
                    .visitorId(0L).gateId(gateId).action("ENTRY_FAILED")
                    .qrScanned(qrData).qrValid(false).verifiedBy(guardUsername)
                    .remarks("Invalid QR scan").timestamp(LocalDateTime.now()).build());

            return result;
        }

        Long visitorId = (Long) qrValidation.get("visitorId");
        Visitor visitor = visitorRepository.findById(visitorId).orElse(null);

        if (visitor == null) {
            result.put("success", false);
            result.put("message", "Visitor record not found");
            return result;
        }

        // Update visitor status
        visitor.setStatus(VisitorStatus.CHECKED_IN_GATE);
        visitor.setGateCheckedInAt(LocalDateTime.now());
        visitorRepository.save(visitor);

        // Log gate entry
        GateLog gateLog = GateLog.builder()
                .visitorId(visitorId)
                .visitorName(visitor.getFullName())
                .gateId(gateId)
                .action("ENTRY")
                .qrScanned(qrData)
                .qrValid(true)
                .idVerified(false)
                .verifiedBy(guardUsername)
                .timestamp(LocalDateTime.now())
                .build();
        gateLogRepository.save(gateLog);

        // Audit log
        auditService.log("GATE_ENTRY", "VISITOR", visitorId,
                guardUsername, "SECURITY_GUARD",
                "Visitor " + visitor.getFullName() + " entered via " + gateId);

        // Notify host
        if (visitor.getHostUserId() != null) {
            notificationService.notifyVisitorAtGate(visitorId, visitor.getFullName(), visitor.getHostUserId());
        }

        // Broadcast to reception dashboard via WebSocket
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "GATE_ENTRY");
        wsPayload.put("visitorId", visitorId);
        wsPayload.put("visitorName", visitor.getFullName());
        wsPayload.put("gateId", gateId);
        wsPayload.put("timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/gate-events", wsPayload);
        messagingTemplate.convertAndSend("/topic/reception-queue", wsPayload);

        result.put("success", true);
        result.put("message", "Gate entry validated successfully");
        result.put("visitor", visitor);
        return result;
    }

    /**
     * Guard manually verifies visitor ID
     */
    public Map<String, Object> verifyVisitorId(Long visitorId, String gateId, String guardUsername, boolean verified) {
        Map<String, Object> result = new HashMap<>();
        Visitor visitor = visitorRepository.findById(visitorId).orElse(null);

        if (visitor == null) {
            result.put("success", false);
            result.put("message", "Visitor not found");
            return result;
        }

        // Update gate log with ID verification
        List<GateLog> logs = gateLogRepository.findByVisitorIdOrderByTimestampDesc(visitorId);
        if (!logs.isEmpty()) {
            GateLog latestLog = logs.get(0);
            latestLog.setIdVerified(verified);
            latestLog.setVerifiedBy(guardUsername);
            gateLogRepository.save(latestLog);
        }

        auditService.log("ID_VERIFICATION", "VISITOR", visitorId,
                guardUsername, "SECURITY_GUARD",
                "ID " + (verified ? "verified" : "rejected") + " for " + visitor.getFullName());

        result.put("success", true);
        result.put("verified", verified);
        return result;
    }

    public List<GateLog> getTodayLogs() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        return gateLogRepository.findByDateRange(today, LocalDateTime.now());
    }

    public List<GateLog> getVisitorGateLogs(Long visitorId) {
        return gateLogRepository.findByVisitorIdOrderByTimestampDesc(visitorId);
    }
}
