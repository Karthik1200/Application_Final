package com.example.Application.controller;

import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.service.AnalyticsService;
import com.example.Application.service.AuditService;
import com.example.Application.service.ExportService;
import com.example.Application.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private static final String EXCEL_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_MIME   = "application/pdf";

    @Autowired private AnalyticsService analyticsService;
    @Autowired private AuditService auditService;
    @Autowired private NotificationService notificationService;
    @Autowired private ExportService exportService;

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

    // ── Export endpoints ──────────────────────────────────────────────────────

    @GetMapping("/export/visitors/pdf")
    public ResponseEntity<byte[]> exportVisitorsPdf() throws Exception {
        return pdfResponse(exportService.exportVisitorsPdf(), "visitors.pdf");
    }

    @GetMapping("/export/visitors/excel")
    public ResponseEntity<byte[]> exportVisitorsExcel() throws Exception {
        return fileResponse(exportService.exportVisitorsExcel(), "visitors.xlsx", EXCEL_MIME);
    }

    @GetMapping("/export/gate-logs/pdf")
    public ResponseEntity<byte[]> exportGateLogsPdf() throws Exception {
        return pdfResponse(exportService.exportGateLogsPdf(), "gate-logs.pdf");
    }

    @GetMapping("/export/gate-logs/excel")
    public ResponseEntity<byte[]> exportGateLogsExcel() throws Exception {
        return fileResponse(exportService.exportGateLogsExcel(), "gate-logs.xlsx", EXCEL_MIME);
    }

    @GetMapping("/export/audit-logs/pdf")
    public ResponseEntity<byte[]> exportAuditLogsPdf() throws Exception {
        return pdfResponse(exportService.exportAuditLogsPdf(), "audit-trail.pdf");
    }

    @GetMapping("/export/audit-logs/excel")
    public ResponseEntity<byte[]> exportAuditLogsExcel() throws Exception {
        return fileResponse(exportService.exportAuditLogsExcel(), "audit-trail.xlsx", EXCEL_MIME);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] data, String filename) {
        return fileResponse(data, filename, PDF_MIME);
    }

    private ResponseEntity<byte[]> fileResponse(byte[] data, String filename, String mime) {
        HttpHeaders h = new HttpHeaders();
        h.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        h.add(HttpHeaders.CONTENT_TYPE, mime);
        return ResponseEntity.ok().headers(h).body(data);
    }
}
