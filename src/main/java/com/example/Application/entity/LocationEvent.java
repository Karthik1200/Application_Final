package com.example.Application.entity;

import com.example.Application.enums.VisitorLocation;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "location_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LocationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long visitorId;

    private String visitorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitorLocation location;

    private String zoneId;

    private String floor;

    private Double positionX;

    private Double positionY;

    private boolean restrictedZone;

    private boolean alertTriggered;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private Long dwellTimeSeconds; 

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
