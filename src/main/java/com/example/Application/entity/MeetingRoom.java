package com.example.Application.entity;

import com.example.Application.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeetingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String roomCode; // MR-101, MR-201 etc.

    @Column(nullable = false)
    private String roomName;

    @Column(nullable = false)
    private String floor;

    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    private String facilities; // Projector, Whiteboard, Video Conf

    private String locationX; // Map coordinates

    private String locationY;

    private Long currentMeetingId;

    private int currentOccupancy;

    private LocalDateTime lastStatusUpdate;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = RoomStatus.AVAILABLE;
        lastStatusUpdate = LocalDateTime.now();
    }
}
