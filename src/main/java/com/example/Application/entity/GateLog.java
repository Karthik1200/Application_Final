package com.example.Application.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gate_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long visitorId;

    private String visitorName;

    @Column(nullable = false)
    private String gateId; // GATE_1, GATE_2, etc.

    @Column(nullable = false)
    private String action; // ENTRY, EXIT

    private String qrScanned;

    private boolean qrValid;

    private boolean idVerified;

    private String verifiedBy; // guard username

    private String remarks;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
