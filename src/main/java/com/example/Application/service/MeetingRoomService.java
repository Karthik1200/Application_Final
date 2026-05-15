package com.example.Application.service;

import com.example.Application.entity.Meeting;
import com.example.Application.entity.MeetingRoom;
import com.example.Application.enums.RoomStatus;
import com.example.Application.repository.MeetingRepository;
import com.example.Application.repository.MeetingRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MeetingRoomService {

    @Autowired private MeetingRoomRepository meetingRoomRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private AuditService auditService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    public List<MeetingRoom> getAllRooms() {
        return meetingRoomRepository.findAll();
    }

    public List<MeetingRoom> getAvailableRooms() {
        return meetingRoomRepository.findByStatus(RoomStatus.AVAILABLE);
    }

    public List<MeetingRoom> getRoomsByFloor(String floor) {
        return meetingRoomRepository.findByFloor(floor);
    }

    public MeetingRoom getRoomByCode(String roomCode) {
        return meetingRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomCode));
    }

    public Meeting startMeeting(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        meeting.setStatus("IN_PROGRESS");
        meeting.setActualStart(LocalDateTime.now());
        meetingRepository.save(meeting);

        MeetingRoom room = meetingRoomRepository.findById(meeting.getRoomId()).orElse(null);
        if (room != null) {
            room.setStatus(RoomStatus.OCCUPIED);
            room.setCurrentMeetingId(meetingId);
            room.setLastStatusUpdate(LocalDateTime.now());
            meetingRoomRepository.save(room);
        }

        broadcastRoomUpdate();
        return meeting;
    }

    public Meeting extendMeeting(Long meetingId, int additionalMinutes, String performedBy) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        meeting.setScheduledEnd(meeting.getScheduledEnd().plusMinutes(additionalMinutes));
        meeting.setExtensionCount(meeting.getExtensionCount() + 1);
        meetingRepository.save(meeting);

        auditService.log("MEETING_EXTENDED", "MEETING", meetingId,
                performedBy, "HOST", "Meeting extended by " + additionalMinutes + " minutes");

        broadcastRoomUpdate();
        return meeting;
    }

    public Meeting endMeeting(Long meetingId, String performedBy) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        meeting.setStatus("COMPLETED");
        meeting.setActualEnd(LocalDateTime.now());
        meetingRepository.save(meeting);

        MeetingRoom room = meetingRoomRepository.findById(meeting.getRoomId()).orElse(null);
        if (room != null) {
            room.setStatus(RoomStatus.AVAILABLE);
            room.setCurrentMeetingId(null);
            room.setCurrentOccupancy(0);
            room.setLastStatusUpdate(LocalDateTime.now());
            meetingRoomRepository.save(room);
        }

        auditService.log("MEETING_ENDED", "MEETING", meetingId,
                performedBy, "", "Meeting ended");

        broadcastRoomUpdate();
        return meeting;
    }

    public List<Meeting> getActiveMeetings() {
        return meetingRepository.findActiveMeetings();
    }

    public List<Meeting> getMeetingsByVisitor(Long visitorId) {
        return meetingRepository.findByVisitorId(visitorId);
    }

    public MeetingRoom createRoom(String roomCode, String roomName, String floor,
                                   int capacity, String facilities,
                                   String locationX, String locationY) {
        if (meetingRoomRepository.findByRoomCode(roomCode).isPresent()) {
            throw new RuntimeException("Room code already exists: " + roomCode);
        }
        MeetingRoom room = MeetingRoom.builder()
                .roomCode(roomCode).roomName(roomName).floor(floor)
                .capacity(capacity).status(RoomStatus.AVAILABLE)
                .facilities(facilities)
                .locationX(locationX).locationY(locationY)
                .currentOccupancy(0).build();
        MeetingRoom saved = meetingRoomRepository.save(room);
        broadcastRoomUpdate();
        return saved;
    }

    public MeetingRoom updateRoom(Long id, String roomName, String floor,
                                   int capacity, String facilities,
                                   String locationX, String locationY) {
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found: " + id));
        if (roomName  != null) room.setRoomName(roomName);
        if (floor     != null) room.setFloor(floor);
        if (capacity  > 0)     room.setCapacity(capacity);
        if (facilities != null) room.setFacilities(facilities);
        if (locationX != null) room.setLocationX(locationX);
        if (locationY != null) room.setLocationY(locationY);
        MeetingRoom saved = meetingRoomRepository.save(room);
        broadcastRoomUpdate();
        return saved;
    }

    public void deleteRoom(Long id) {
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found: " + id));
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new RuntimeException("Cannot delete an occupied room");
        }
        meetingRoomRepository.deleteById(id);
        broadcastRoomUpdate();
    }

    private void broadcastRoomUpdate() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ROOM_UPDATE");
        payload.put("rooms", getAllRooms());
        payload.put("timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/rooms", payload);
    }
}
