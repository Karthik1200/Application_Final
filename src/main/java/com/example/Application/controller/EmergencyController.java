package com.example.Application.controller;

import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.service.EmergencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/emergency")
public class EmergencyController {

    @Autowired
    private EmergencyService emergencyService;

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody Map<String, String> request) {
        try {
            var log = emergencyService.activateEmergency(
                    request.getOrDefault("type", "FIRE"),
                    request.getOrDefault("severity", "HIGH"),
                    request.getOrDefault("triggeredBy", "admin"),
                    request.get("description"),
                    request.get("affectedZones")
            );
            return ResponseEntity.ok(ApiResponseDTO.success("Emergency activated", log));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        try {
            String resolvedBy = body != null ? body.getOrDefault("resolvedBy", "admin") : "admin";
            var log = emergencyService.resolveEmergency(id, resolvedBy);
            return ResponseEntity.ok(ApiResponseDTO.success("Emergency resolved", log));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/evacuation-update")
    public ResponseEntity<?> evacuationUpdate(@PathVariable Long id,
                                               @RequestBody Map<String, Object> body) {
        try {
            int count = Integer.parseInt(body.getOrDefault("evacuatedCount", 0).toString());
            var log = emergencyService.recordEvacuation(id, count);
            return ResponseEntity.ok(ApiResponseDTO.success("Evacuation count updated", log));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getEvacuationStatus() {
        return ResponseEntity.ok(ApiResponseDTO.success("Evacuation status",
                emergencyService.getEvacuationStatus()));
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActive() {
        return ResponseEntity.ok(ApiResponseDTO.success("Active emergencies",
                emergencyService.getActiveEmergencies()));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(ApiResponseDTO.success("Emergency history",
                emergencyService.getAllEmergencies()));
    }
}
