package com.example.Application.controller;

import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.service.ReceptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reception")
public class ReceptionController {

    @Autowired
    private ReceptionService receptionService;

    @GetMapping("/queue")
    public ResponseEntity<?> getQueue() {
        return ResponseEntity.ok(ApiResponseDTO.success("Reception queue",
                receptionService.getReceptionQueue()));
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, String> request) {
        try {
            Long visitorId = Long.parseLong(request.get("visitorId"));
            String roomCode = request.get("roomCode");
            String receptionist = request.getOrDefault("receptionist", "reception1");

            var result = receptionService.checkInAtReception(visitorId, roomCode, receptionist);
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

    @PostMapping("/auto-assign-room")
    public ResponseEntity<?> autoAssignRoom(@RequestBody Map<String, Integer> request) {
        try {
            int capacity = request.getOrDefault("capacity", 2);
            var room = receptionService.autoAssignRoom(capacity);
            return ResponseEntity.ok(ApiResponseDTO.success("Room auto-assigned", room));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/start-route/{visitorId}")
    public ResponseEntity<?> startRoute(@PathVariable Long visitorId) {
        try {
            var visitor = receptionService.startRoute(visitorId, "reception1");
            return ResponseEntity.ok(ApiResponseDTO.success("Route started", visitor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }
}
