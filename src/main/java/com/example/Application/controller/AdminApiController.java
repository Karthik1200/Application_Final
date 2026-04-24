package com.example.Application.controller;

import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.service.AnalyticsService;
import com.example.Application.service.AuditService;
import com.example.Application.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    @Autowired private AnalyticsService analyticsService;
    @Autowired private AuditService auditService;
    @Autowired private NotificationService notificationService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats() {
        return ResponseEntity.ok(ApiResponseDTO.success("Dashboard stats",
                analyticsService.getDashboardStats()));
    }

    @GetMapping("/analytics/visitors")
    public ResponseEntity<?> getVisitorAnalytics(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponseDTO.success("Visitor analytics",
                analyticsService.getVisitorAnalytics(days)));
    }

    @GetMapping("/analytics/meetings")
    public ResponseEntity<?> getMeetingAnalytics() {
        return ResponseEntity.ok(ApiResponseDTO.success("Meeting analytics",
                analyticsService.getMeetingAnalytics()));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs() {
        return ResponseEntity.ok(ApiResponseDTO.success("Audit logs",
                auditService.getRecentLogs()));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications() {
        return ResponseEntity.ok(ApiResponseDTO.success("Notifications",
                notificationService.getRecent()));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Notification marked as read", null));
    }
}
