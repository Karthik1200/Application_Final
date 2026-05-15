package com.example.Application.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmergencyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String emergencyType;  // FIRE, SECURITY, MEDICAL, EVACUATION, DRILL

    @Column(nullable = false)
    private String severity;       // CRITICAL, HIGH, MEDIUM, LOW

    private String triggeredBy;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String affectedZones;

    private Integer totalVisitors;
    private Integer evacuatedCount;
    private Integer missingCount;

    @Column(nullable = false)
    private String status;         // ACTIVE, RESOLVED, DRILL

    private String resolvedBy;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (triggeredAt == null) triggeredAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (severity == null) severity = "HIGH";
        if (totalVisitors == null) totalVisitors = 0;
        if (evacuatedCount == null) evacuatedCount = 0;
        if (missingCount == null) missingCount = 0;
    }
}
