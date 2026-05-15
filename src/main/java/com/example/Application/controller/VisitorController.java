package com.example.Application.controller;

import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.dto.VisitorRegistrationDTO;
import com.example.Application.entity.Visitor;
import com.example.Application.enums.VisitorStatus;
import com.example.Application.service.VisitorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/visitor")
public class VisitorController {

    @Autowired
    private VisitorService visitorService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody VisitorRegistrationDTO dto) {
        try {
            Visitor visitor = visitorService.register(dto);
            return ResponseEntity.ok(ApiResponseDTO.success(
                    "Registration successful. OTP: " + visitor.getOtp() + " (In production, sent via SMS/Email)",
                    Map.of("visitorId", visitor.getId(), "otp", visitor.getOtp())
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        try {
            Long visitorId = Long.parseLong(request.get("visitorId"));
            String otp = request.get("otp");
            Visitor visitor = visitorService.verifyOtp(visitorId, otp);
            return ResponseEntity.ok(ApiResponseDTO.success("OTP verified. QR pass generated!", visitor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllVisitors() {
        return ResponseEntity.ok(ApiResponseDTO.success("All visitors", visitorService.getAllVisitors()));
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveVisitors() {
        return ResponseEntity.ok(ApiResponseDTO.success("Active visitors", visitorService.getActiveVisitors()));
    }

    @GetMapping("/queue")
    public ResponseEntity<?> getReceptionQueue() {
        return ResponseEntity.ok(ApiResponseDTO.success("Reception queue", visitorService.getReceptionQueue()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVisitor(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponseDTO.success("Visitor details", visitorService.getById(id)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getByStatus(@PathVariable String status) {
        try {
            VisitorStatus vs = VisitorStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(ApiResponseDTO.success("Visitors by status", visitorService.getByStatus(vs)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<?> checkout(@PathVariable Long id) {
        try {
            Visitor visitor = visitorService.checkout(id, "system");
            return ResponseEntity.ok(ApiResponseDTO.success("Visitor checked out", visitor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/resend-otp")
    public ResponseEntity<?> resendOtp(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String channels = body != null ? body.getOrDefault("channels", "sms") : "sms";
            visitorService.resendOtp(id, channels);
            return ResponseEntity.ok(ApiResponseDTO.success("OTP resent via " + channels, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/today-count")
    public ResponseEntity<?> getTodayCount() {
        return ResponseEntity.ok(ApiResponseDTO.success("Today's count", visitorService.getTodayCount()));
    }
}
