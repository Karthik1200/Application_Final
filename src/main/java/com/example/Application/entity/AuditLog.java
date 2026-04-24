package com.example.Application.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType; // REGISTRATION, GATE_ENTRY, QR_SCAN, ROOM_ASSIGN, CHECKOUT, etc.

    @Column(nullable = false)
    private String entityType; // VISITOR, MEETING, ROOM, USER

    private Long entityId;

    private String performedBy;

    private String performedByRole;

    @Column(length = 2000)
    private String description;

    @Column(length = 2000)
    private String oldValue;

    @Column(length = 2000)
    private String newValue;

    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
