package com.example.Application.controller;

import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.enums.VisitorLocation;
import com.example.Application.service.TrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {

    @Autowired
    private TrackingService trackingService;

    @PostMapping("/location")
    public ResponseEntity<?> recordLocation(@RequestBody Map<String, Object> request) {
        try {
            Long visitorId = Long.parseLong(request.get("visitorId").toString());
            VisitorLocation location = VisitorLocation.valueOf((String) request.get("location"));
            String zoneId = (String) request.getOrDefault("zoneId", "");
            String floor = (String) request.getOrDefault("floor", "1");
            Double posX = request.get("posX") != null ? Double.parseDouble(request.get("posX").toString()) : 0.0;
            Double posY = request.get("posY") != null ? Double.parseDouble(request.get("posY").toString()) : 0.0;

            var event = trackingService.recordLocation(visitorId, location, zoneId, floor, posX, posY);
            return ResponseEntity.ok(ApiResponseDTO.success("Location recorded", event));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/all-locations")
    public ResponseEntity<?> getAllLocations() {
        return ResponseEntity.ok(ApiResponseDTO.success("All visitor locations",
                trackingService.getAllVisitorLocations()));
    }

    @GetMapping("/history/{visitorId}")
    public ResponseEntity<?> getVisitorHistory(@PathVariable Long visitorId) {
        return ResponseEntity.ok(ApiResponseDTO.success("Visitor location history",
                trackingService.getVisitorHistory(visitorId)));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<?> getHeatmap() {
        return ResponseEntity.ok(ApiResponseDTO.success("Location heatmap",
                trackingService.getHeatmapData()));
    }

    @PostMapping("/emergency")
    public ResponseEntity<?> activateEmergency() {
        var locations = trackingService.getEmergencyLocations();
        return ResponseEntity.ok(ApiResponseDTO.success("Emergency protocol activated", locations));
    }
}
