package com.example.Application.controller;

import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.service.GateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gate")
public class GateController {

    @Autowired
    private GateService gateService;

    @PostMapping("/scan-qr")
    public ResponseEntity<?> scanQR(@RequestBody Map<String, String> request) {
        try {
            String qrData = request.get("qrData");
            String gateId = request.getOrDefault("gateId", "GATE_1");
            String guardUsername = request.getOrDefault("guardUsername", "guard1");

            var result = gateService.scanQRAtGate(qrData, gateId, guardUsername);
            if ((boolean) result.get("success")) {
                return ResponseEntity.ok(ApiResponseDTO.success(
                        (String) result.get("message"), result));
            } else {
                return ResponseEntity.badRequest().body(
                        ApiResponseDTO.error((String) result.get("message")));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/verify-id")
    public ResponseEntity<?> verifyId(@RequestBody Map<String, Object> request) {
        try {
            Long visitorId = Long.parseLong(request.get("visitorId").toString());
            String gateId = (String) request.getOrDefault("gateId", "GATE_1");
            String guardUsername = (String) request.getOrDefault("guardUsername", "guard1");
            boolean verified = (boolean) request.getOrDefault("verified", true);

            var result = gateService.verifyVisitorId(visitorId, gateId, guardUsername, verified);
            return ResponseEntity.ok(ApiResponseDTO.success("ID verification recorded", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/logs/today")
    public ResponseEntity<?> getTodayLogs() {
        return ResponseEntity.ok(ApiResponseDTO.success("Today's gate logs", gateService.getTodayLogs()));
    }

    @GetMapping("/logs/visitor/{visitorId}")
    public ResponseEntity<?> getVisitorLogs(@PathVariable Long visitorId) {
        return ResponseEntity.ok(ApiResponseDTO.success("Visitor gate logs",
                gateService.getVisitorGateLogs(visitorId)));
    }
}
