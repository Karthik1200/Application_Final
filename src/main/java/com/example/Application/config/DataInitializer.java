package com.example.Application.config;

import com.example.Application.entity.*;
import com.example.Application.enums.*;
import com.example.Application.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private AppUserRepository appUserRepository;
    @Autowired private MeetingRoomRepository meetingRoomRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private VisitorRepository visitorRepository;
    @Autowired private BlacklistRepository blacklistRepository;

    @Override
    public void run(String... args) {
        if (appUserRepository.count() == 0) {
            appUserRepository.save(AppUser.builder()
                    .username("admin").password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator").email("admin@vrgt.com")
                    .phone("9876543210").department("IT").role(UserRole.ADMIN).active(true).build());

            appUserRepository.save(AppUser.builder()
                    .username("guard1").password(passwordEncoder.encode("guard123"))
                    .fullName("Rajesh Kumar").email("guard1@vrgt.com")
                    .phone("9876543211").department("Security").role(UserRole.SECURITY_GUARD).active(true).build());

            appUserRepository.save(AppUser.builder()
                    .username("reception1").password(passwordEncoder.encode("reception123"))
                    .fullName("Priya Sharma").email("reception1@vrgt.com")
                    .phone("9876543212").department("Front Desk").role(UserRole.RECEPTIONIST).active(true).build());

            appUserRepository.save(AppUser.builder()
                    .username("host1").password(passwordEncoder.encode("host123"))
                    .fullName("Amit Patel").email("host1@vrgt.com")
                    .phone("9876543213").department("Engineering").role(UserRole.HOST).active(true).build());

            appUserRepository.save(AppUser.builder()
                    .username("host2").password(passwordEncoder.encode("host123"))
                    .fullName("Sneha Reddy").email("host2@vrgt.com")
                    .phone("9876543214").department("Marketing").role(UserRole.HOST).active(true).build());

            appUserRepository.save(AppUser.builder()
                    .username("host3").password(passwordEncoder.encode("host123"))
                    .fullName("Vikram Singh").email("host3@vrgt.com")
                    .phone("9876543215").department("HR").role(UserRole.HOST).active(true).build());
        }

        if (meetingRoomRepository.count() == 0) {
            String[][] rooms = {
                {"MR-101", "Lotus", "1", "6", "350", "180"},
                {"MR-102", "Orchid", "1", "4", "500", "180"},
                {"MR-103", "Jasmine", "1", "8", "650", "180"},
                {"MR-201", "Tulip", "2", "10", "350", "380"},
                {"MR-202", "Iris", "2", "6", "500", "380"},
                {"MR-203", "Dahlia", "2", "12", "650", "380"},
                {"MR-301", "Sunflower", "3", "15", "350", "530"},
                {"MR-302", "Marigold", "3", "8", "500", "530"},
            };

            for (String[] r : rooms) {
                meetingRoomRepository.save(MeetingRoom.builder()
                        .roomCode(r[0]).roomName(r[1]).floor(r[2])
                        .capacity(Integer.parseInt(r[3]))
                        .status(RoomStatus.AVAILABLE)
                        .facilities("Projector, Whiteboard, Video Conferencing")
                        .locationX(r[4]).locationY(r[5])
                        .currentOccupancy(0).build());
            }
        }

        if (visitorRepository.count() == 0) {
            visitorRepository.save(Visitor.builder()
                    .fullName("Rahul Mehta").email("rahul@techcorp.com").phone("9988776655")
                    .company("TechCorp Solutions").idType("Aadhar").idNumber("1234-5678-9012")
                    .purpose("Project Discussion").hostName("Amit Patel").hostEmail("host1@vrgt.com")
                    .status(VisitorStatus.CHECKED_IN_GATE).gateCheckedInAt(LocalDateTime.now().minusMinutes(15))
                    .build());

            visitorRepository.save(Visitor.builder()
                    .fullName("Meera Iyer").email("meera@globalinc.com").phone("9988776656")
                    .company("Global Inc").idType("Passport").idNumber("A1234567")
                    .purpose("Business Review").hostName("Sneha Reddy").hostEmail("host2@vrgt.com")
                    .status(VisitorStatus.IN_MEETING).assignedRoomId("MR-201")
                    .gateCheckedInAt(LocalDateTime.now().minusHours(1))
                    .receptionCheckedInAt(LocalDateTime.now().minusMinutes(50))
                    .meetingStartedAt(LocalDateTime.now().minusMinutes(40))
                    .build());

            visitorRepository.save(Visitor.builder()
                    .fullName("Arjun Nair").email("arjun@startupxyz.com").phone("9988776657")
                    .company("StartupXYZ").idType("DrivingLicense").idNumber("DL-9876543")
                    .purpose("Interview").hostName("Vikram Singh").hostEmail("host3@vrgt.com")
                    .status(VisitorStatus.PRE_REGISTERED)
                    .build());
        }

        if (blacklistRepository.count() == 0) {
            blacklistRepository.save(BlacklistEntry.builder()
                    .fullName("John Banned").email("banned@test.com").phone("0000000000")
                    .idNumber("BL-999").reason("Security violation - unauthorized access attempt")
                    .addedBy("admin").active(true).build());
        }
    }
}
