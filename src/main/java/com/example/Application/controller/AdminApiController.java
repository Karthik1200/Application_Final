package com.example.Application.controller;

import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.entity.AppUser;
import com.example.Application.enums.UserRole;
import com.example.Application.repository.AppUserRepository;
import com.example.Application.service.AnalyticsService;
import com.example.Application.service.AuditService;
import com.example.Application.service.ExportService;
import com.example.Application.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private static final String EXCEL_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_MIME   = "application/pdf";

    @Autowired private AnalyticsService analyticsService;
    @Autowired private AuditService auditService;
    @Autowired private NotificationService notificationService;
    @Autowired private ExportService exportService;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

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

    // ── User management ───────────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(ApiResponseDTO.success("All users", appUserRepository.findAll()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String fullName = body.get("fullName");
            String email    = body.get("email");
            if (username == null || password == null || fullName == null || email == null) {
                return ResponseEntity.badRequest().body(ApiResponseDTO.error("username, password, fullName and email are required"));
            }
            if (appUserRepository.existsByUsername(username)) {
                return ResponseEntity.badRequest().body(ApiResponseDTO.error("Username already taken"));
            }
            if (appUserRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(ApiResponseDTO.error("Email already registered"));
            }
            UserRole role = UserRole.HOST;
            try { role = UserRole.valueOf(body.getOrDefault("role", "HOST")); } catch (Exception ignored) {}
            AppUser user = AppUser.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .email(email)
                    .phone(body.getOrDefault("phone", ""))
                    .department(body.getOrDefault("department", ""))
                    .role(role)
                    .active(true)
                    .build();
            AppUser saved = appUserRepository.save(user);
            auditService.log("USER_CREATED", "APP_USER", saved.getId(), "admin", "ADMIN", "User created: " + username);
            return ResponseEntity.ok(ApiResponseDTO.success("User created", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            AppUser user = appUserRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (body.containsKey("fullName"))   user.setFullName(body.get("fullName"));
            if (body.containsKey("email"))      user.setEmail(body.get("email"));
            if (body.containsKey("phone"))      user.setPhone(body.get("phone"));
            if (body.containsKey("department")) user.setDepartment(body.get("department"));
            if (body.containsKey("role")) {
                try { user.setRole(UserRole.valueOf(body.get("role"))); } catch (Exception ignored) {}
            }
            if (body.containsKey("password") && !body.get("password").isBlank()) {
                user.setPassword(passwordEncoder.encode(body.get("password")));
            }
            if (body.containsKey("active")) {
                user.setActive(Boolean.parseBoolean(body.get("active")));
            }
            AppUser saved = appUserRepository.save(user);
            auditService.log("USER_UPDATED", "APP_USER", id, "admin", "ADMIN", "User updated: " + user.getUsername());
            return ResponseEntity.ok(ApiResponseDTO.success("User updated", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        try {
            AppUser user = appUserRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setActive(false);
            appUserRepository.save(user);
            auditService.log("USER_DEACTIVATED", "APP_USER", id, "admin", "ADMIN", "User deactivated: " + user.getUsername());
            return ResponseEntity.ok(ApiResponseDTO.success("User deactivated", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
