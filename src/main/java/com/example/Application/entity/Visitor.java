package com.example.Application.entity;

import com.example.Application.enums.VisitorStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    private String company;

    private String idType; 

    private String idNumber;

    private String photoUrl;

    private String purpose;

    @Column(nullable = false)
    private String hostName;

    private String hostEmail;

    private String hostPhone;

    private Long hostUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitorStatus status;

    private String qrCode; 

    private String qrImageBase64;

    private LocalDateTime qrExpiry;

    private String otp;

    private LocalDateTime otpExpiry;

    private boolean otpVerified;

    private boolean emailVerified;

    private String assignedRoomId;

    private String nfcTagId;

    private LocalDateTime preRegisteredAt;

    private LocalDateTime gateCheckedInAt;

    private LocalDateTime receptionCheckedInAt;

    private LocalDateTime meetingStartedAt;

    private LocalDateTime checkedOutAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = VisitorStatus.PRE_REGISTERED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
