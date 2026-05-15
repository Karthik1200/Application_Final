package com.example.Application.controller;

import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.service.MeetingRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class MeetingRoomController {

    @Autowired
    private MeetingRoomService meetingRoomService;

    @GetMapping
    public ResponseEntity<?> getAllRooms() {
        return ResponseEntity.ok(ApiResponseDTO.success("All rooms", meetingRoomService.getAllRooms()));
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableRooms() {
        return ResponseEntity.ok(ApiResponseDTO.success("Available rooms",
                meetingRoomService.getAvailableRooms()));
    }

    @GetMapping("/floor/{floor}")
    public ResponseEntity<?> getRoomsByFloor(@PathVariable String floor) {
        return ResponseEntity.ok(ApiResponseDTO.success("Rooms on floor " + floor,
                meetingRoomService.getRoomsByFloor(floor)));
    }

    @PostMapping("/meeting/{meetingId}/start")
    public ResponseEntity<?> startMeeting(@PathVariable Long meetingId) {
        try {
            var meeting = meetingRoomService.startMeeting(meetingId);
            return ResponseEntity.ok(ApiResponseDTO.success("Meeting started", meeting));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/meeting/{meetingId}/extend")
    public ResponseEntity<?> extendMeeting(@PathVariable Long meetingId,
                                            @RequestBody Map<String, Object> request) {
        try {
            int minutes = (int) request.getOrDefault("minutes", 30);
            String performedBy = (String) request.getOrDefault("performedBy", "host");
            var meeting = meetingRoomService.extendMeeting(meetingId, minutes, performedBy);
            return ResponseEntity.ok(ApiResponseDTO.success("Meeting extended by " + minutes + " mins", meeting));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/meeting/{meetingId}/end")
    public ResponseEntity<?> endMeeting(@PathVariable Long meetingId) {
        try {
            var meeting = meetingRoomService.endMeeting(meetingId, "host");
            return ResponseEntity.ok(ApiResponseDTO.success("Meeting ended", meeting));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @GetMapping("/meetings/active")
    public ResponseEntity<?> getActiveMeetings() {
        return ResponseEntity.ok(ApiResponseDTO.success("Active meetings",
                meetingRoomService.getActiveMeetings()));
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody Map<String, Object> body) {
        try {
            String roomCode   = (String) body.get("roomCode");
            String roomName   = (String) body.get("roomName");
            String floor      = (String) body.getOrDefault("floor", "1");
            int    capacity   = body.get("capacity") != null ? Integer.parseInt(body.get("capacity").toString()) : 2;
            String facilities = (String) body.getOrDefault("facilities", "");
            String locationX  = (String) body.getOrDefault("locationX", "0");
            String locationY  = (String) body.getOrDefault("locationY", "0");
            if (roomCode == null || roomName == null) {
                return ResponseEntity.badRequest().body(ApiResponseDTO.error("roomCode and roomName are required"));
            }
            var room = meetingRoomService.createRoom(roomCode, roomName, floor, capacity, facilities, locationX, locationY);
            return ResponseEntity.ok(ApiResponseDTO.success("Room created", room));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String roomName   = (String) body.get("roomName");
            String floor      = (String) body.get("floor");
            int    capacity   = body.get("capacity") != null ? Integer.parseInt(body.get("capacity").toString()) : 0;
            String facilities = (String) body.get("facilities");
            String locationX  = (String) body.get("locationX");
            String locationY  = (String) body.get("locationY");
            var room = meetingRoomService.updateRoom(id, roomName, floor, capacity, facilities, locationX, locationY);
            return ResponseEntity.ok(ApiResponseDTO.success("Room updated", room));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        try {
            meetingRoomService.deleteRoom(id);
            return ResponseEntity.ok(ApiResponseDTO.success("Room deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }
}
