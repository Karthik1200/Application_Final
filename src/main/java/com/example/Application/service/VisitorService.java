package com.example.Application.service;

import com.example.Application.dto.VisitorRegistrationDTO;
import com.example.Application.entity.Visitor;
import com.example.Application.enums.VisitorStatus;
import com.example.Application.repository.BlacklistRepository;
import com.example.Application.repository.VisitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class VisitorService {

    @Autowired private VisitorRepository visitorRepository;
    @Autowired private BlacklistRepository blacklistRepository;
    @Autowired private QRCodeService qrCodeService;
    @Autowired private AuditService auditService;
    @Autowired private NotificationService notificationService;
    @Autowired private Msg91Service msg91Service;

    public Visitor register(VisitorRegistrationDTO dto) {
        var blacklisted = blacklistRepository.findMatchingBlacklist(
                dto.getEmail(), dto.getPhone(),
                dto.getIdNumber() != null ? dto.getIdNumber() : "");
        if (!blacklisted.isEmpty()) {
            throw new RuntimeException("Visitor is blacklisted: " + blacklisted.get(0).getReason());
        }

        if (dto.getIdNumber() != null && !dto.getIdNumber().isEmpty()) {
            var duplicates = visitorRepository.findByIdNumberAndStatusNot(dto.getIdNumber(), VisitorStatus.CHECKED_OUT);
            if (!duplicates.isEmpty()) {
                throw new RuntimeException("An active visit already exists for this ID number");
            }
        }

        String otp = String.format("%06d", new Random().nextInt(999999));

        Visitor visitor = Visitor.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .company(dto.getCompany())
                .idType(dto.getIdType())
                .idNumber(dto.getIdNumber())
                .purpose(dto.getPurpose())
                .hostName(dto.getHostName())
                .hostEmail(dto.getHostEmail())
                .hostPhone(dto.getHostPhone())
                .status(VisitorStatus.PRE_REGISTERED)
                .otp(otp)
                .otpExpiry(LocalDateTime.now().plusMinutes(10))
                .otpVerified(false)
                .emailVerified(false)
                .preRegisteredAt(LocalDateTime.now())
                .build();

        visitor = visitorRepository.save(visitor);

        auditService.log("REGISTRATION", "VISITOR", visitor.getId(),
                "SYSTEM", "SYSTEM", "Visitor pre-registered: " + visitor.getFullName());

        String channels = dto.getOtpChannel() != null ? dto.getOtpChannel() : "sms";
        Msg91Service.OtpResult result = msg91Service.sendOtp(
                visitor.getPhone(), visitor.getEmail(), visitor.getFullName(), otp, channels);
        auditService.log("OTP_SENT", "VISITOR", visitor.getId(),
                "SYSTEM", "SYSTEM", result.summary());

        return visitor;
    }

    public Visitor resendOtp(Long visitorId, String channels) {
        Visitor visitor = getById(visitorId);
        if (visitor.isOtpVerified()) {
            throw new RuntimeException("OTP already verified — no need to resend");
        }

        String newOtp = String.format("%06d", new Random().nextInt(999999));
        visitor.setOtp(newOtp);
        visitor.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        visitor = visitorRepository.save(visitor);

        Msg91Service.OtpResult result = msg91Service.sendOtp(
                visitor.getPhone(), visitor.getEmail(), visitor.getFullName(), newOtp,
                channels != null ? channels : "sms");
        auditService.log("OTP_RESENT", "VISITOR", visitor.getId(),
                "SYSTEM", "SYSTEM", result.summary());

        return visitor;
    }

    public Visitor verifyOtp(Long visitorId, String otp) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        if (visitor.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        if (!otp.equals(visitor.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        visitor.setOtpVerified(true);
        visitor.setEmailVerified(true);
        visitor.setStatus(VisitorStatus.VERIFIED);

        long expiryTimestamp = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        Map<String, String> qrPass = qrCodeService.createVisitorQRPass(
                visitor.getId(), visitor.getFullName(), visitor.getPurpose(), expiryTimestamp);

        visitor.setQrCode(qrPass.get("encryptedData"));
        visitor.setQrImageBase64(qrPass.get("qrImageBase64"));
        visitor.setQrExpiry(LocalDateTime.now().plusHours(24));

        visitor = visitorRepository.save(visitor);

        auditService.log("OTP_VERIFIED", "VISITOR", visitor.getId(),
                "SYSTEM", "SYSTEM", "OTP verified, QR pass generated for: " + visitor.getFullName());

        return visitor;
    }

    public Visitor getById(Long id) {
        return visitorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visitor not found with id: " + id));
    }

    public List<Visitor> getActiveVisitors() {
        return visitorRepository.findActiveVisitors();
    }

    public List<Visitor> getReceptionQueue() {
        return visitorRepository.findReceptionQueue();
    }

    public List<Visitor> getByStatus(VisitorStatus status) {
        return visitorRepository.findByStatus(status);
    }

    public List<Visitor> getAllVisitors() {
        return visitorRepository.findAll();
    }

    public long getTodayCount() {
        return visitorRepository.countTodayVisitors(LocalDateTime.now().toLocalDate().atStartOfDay());
    }

    public List<Visitor> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return visitorRepository.findByDateRange(start, end);
    }

    public Visitor updateStatus(Long visitorId, VisitorStatus newStatus, String performedBy) {
        Visitor visitor = getById(visitorId);
        String oldStatus = visitor.getStatus().name();
        visitor.setStatus(newStatus);

        switch (newStatus) {
            case CHECKED_IN_GATE -> visitor.setGateCheckedInAt(LocalDateTime.now());
            case AT_RECEPTION -> visitor.setReceptionCheckedInAt(LocalDateTime.now());
            case IN_MEETING -> visitor.setMeetingStartedAt(LocalDateTime.now());
            case CHECKED_OUT -> visitor.setCheckedOutAt(LocalDateTime.now());
            default -> {}
        }

        visitor = visitorRepository.save(visitor);

        auditService.log("STATUS_CHANGE", "VISITOR", visitor.getId(),
                performedBy, "", "Status changed", oldStatus, newStatus.name());

        return visitor;
    }

    public Visitor checkout(Long visitorId, String performedBy) {
        return updateStatus(visitorId, VisitorStatus.CHECKED_OUT, performedBy);
    }
}
