package com.example.Application.service;

import com.example.Application.entity.Meeting;
import com.example.Application.entity.MeetingRoom;
import com.example.Application.entity.Visitor;
import com.example.Application.enums.RoomStatus;
import com.example.Application.enums.VisitorStatus;
import com.example.Application.repository.MeetingRepository;
import com.example.Application.repository.MeetingRoomRepository;
import com.example.Application.repository.VisitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReceptionService {

    @Autowired private VisitorRepository visitorRepository;
    @Autowired private MeetingRoomRepository meetingRoomRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private AuditService auditService;
    @Autowired private NotificationService notificationService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    
    public List<Visitor> getReceptionQueue() {
        return visitorRepository.findReceptionQueue();
    }

   
    public Map<String, Object> checkInAtReception(Long visitorId, String roomCode,
                                                    String receptionistUsername) {
        Map<String, Object> result = new HashMap<>();

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        MeetingRoom room = meetingRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Meeting room not found: " + roomCode));

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            result.put("success", false);
            result.put("message", "Room " + roomCode + " is not available. Current status: " + room.getStatus());
            return result;
        }

        visitor.setStatus(VisitorStatus.HOST_CONFIRMED);
        visitor.setReceptionCheckedInAt(LocalDateTime.now());
        visitor.setAssignedRoomId(roomCode);
        visitor.setNfcTagId("NFC-" + visitorId + "-" + System.currentTimeMillis());
        visitorRepository.save(visitor);

        room.setStatus(RoomStatus.RESERVED);
        room.setCurrentOccupancy(room.getCurrentOccupancy() + 1);
        room.setLastStatusUpdate(LocalDateTime.now());
        meetingRoomRepository.save(room);

        Meeting meeting = Meeting.builder()
                .visitorId(visitorId)
                .visitorName(visitor.getFullName())
                .roomId(room.getId())
                .roomCode(roomCode)
                .hostId(visitor.getHostUserId() != null ? visitor.getHostUserId() : 0L)
                .hostName(visitor.getHostName())
                .purpose(visitor.getPurpose())
                .scheduledStart(LocalDateTime.now())
                .scheduledEnd(LocalDateTime.now().plusHours(1))
                .status("SCHEDULED")
                .hostConfirmed(true)
                .build();
        meetingRepository.save(meeting);

        auditService.log("RECEPTION_CHECKIN", "VISITOR", visitorId,
                receptionistUsername, "RECEPTIONIST",
                "Checked in at reception, assigned room: " + roomCode);

        if (visitor.getHostUserId() != null) {
            notificationService.notifyVisitorAtReception(visitorId, visitor.getFullName(), visitor.getHostUserId());
        }

        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "RECEPTION_CHECKIN");
        wsPayload.put("visitorId", visitorId);
        wsPayload.put("visitorName", visitor.getFullName());
        wsPayload.put("roomCode", roomCode);
        wsPayload.put("timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/reception-events", wsPayload);
        messagingTemplate.convertAndSend("/topic/tracking", wsPayload);

        result.put("success", true);
        result.put("message", "Visitor checked in at reception");
        result.put("visitor", visitor);
        result.put("room", room);
        result.put("nfcTagId", visitor.getNfcTagId());
        return result;
    }

    
    public MeetingRoom autoAssignRoom(int requiredCapacity) {
        List<MeetingRoom> available = meetingRoomRepository
                .findByStatusAndCapacityGreaterThanEqual(RoomStatus.AVAILABLE, requiredCapacity);
        if (available.isEmpty()) {
            throw new RuntimeException("No available rooms with capacity >= " + requiredCapacity);
        }
        // Return the smallest available room that fits
        available.sort(Comparator.comparingInt(MeetingRoom::getCapacity));
        return available.get(0);
    }

    
    public Visitor startRoute(Long visitorId, String performedBy) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        visitor.setStatus(VisitorStatus.EN_ROUTE);
        visitorRepository.save(visitor);

        auditService.log("ROUTE_STARTED", "VISITOR", visitorId,
                performedBy, "RECEPTIONIST", "Visitor en route to room: " + visitor.getAssignedRoomId());

        if (visitor.getHostUserId() != null) {
            notificationService.notifyVisitorEnRoute(visitorId, visitor.getFullName(), visitor.getHostUserId());
        }

        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "VISITOR_EN_ROUTE");
        wsPayload.put("visitorId", visitorId);
        wsPayload.put("visitorName", visitor.getFullName());
        wsPayload.put("roomCode", visitor.getAssignedRoomId());
        messagingTemplate.convertAndSend("/topic/tracking", wsPayload);

        return visitor;
    }
}
