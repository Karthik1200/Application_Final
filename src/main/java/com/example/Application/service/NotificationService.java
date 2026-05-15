package com.example.Application.service;

import com.example.Application.entity.Notification;
import com.example.Application.enums.AlertType;
import com.example.Application.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Notification createAndSend(AlertType alertType, String title, String message,
                                       Long targetUserId, String targetRole,
                                       Long visitorId, String visitorName, String channel) {
        Notification notification = Notification.builder()
                .alertType(alertType)
                .title(title)
                .message(message)
                .targetUserId(targetUserId)
                .targetRole(targetRole)
                .visitorId(visitorId)
                .visitorName(visitorName)
                .channel(channel != null ? channel : "SYSTEM")
                .read(false)
                .sent(true)
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);

        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("id", notification.getId());
        wsPayload.put("type", alertType.name());
        wsPayload.put("title", title);
        wsPayload.put("message", message);
        wsPayload.put("visitorId", visitorId);
        wsPayload.put("visitorName", visitorName);
        wsPayload.put("timestamp", notification.getCreatedAt().toString());

        messagingTemplate.convertAndSend("/topic/notifications", wsPayload);

        if (targetUserId != null) {
            messagingTemplate.convertAndSend("/topic/notifications/" + targetUserId, wsPayload);
        }

        if (targetRole != null) {
            messagingTemplate.convertAndSend("/topic/notifications/role/" + targetRole, wsPayload);
        }

        return notification;
    }

    public void notifyVisitorAtGate(Long visitorId, String visitorName, Long hostUserId) {
        createAndSend(AlertType.VISITOR_AT_GATE, "Visitor at Gate",
                visitorName + " has arrived at the gate",
                hostUserId, "HOST", visitorId, visitorName, "SYSTEM");
    }

    public void notifyVisitorAtReception(Long visitorId, String visitorName, Long hostUserId) {
        createAndSend(AlertType.VISITOR_AT_RECEPTION, "Visitor at Reception",
                visitorName + " is waiting at reception. Please confirm the meeting.",
                hostUserId, "HOST", visitorId, visitorName, "SYSTEM");
    }

    public void notifyHostConfirmationRequired(Long visitorId, String visitorName, Long hostUserId) {
        createAndSend(AlertType.HOST_CONFIRMATION_REQUIRED, "Confirmation Required",
                "Please confirm meeting with " + visitorName,
                hostUserId, "HOST", visitorId, visitorName, "SYSTEM");
    }

    public void notifyVisitorEnRoute(Long visitorId, String visitorName, Long hostUserId) {
        createAndSend(AlertType.VISITOR_EN_ROUTE, "Visitor En Route",
                visitorName + " is on the way to the meeting room",
                hostUserId, "HOST", visitorId, visitorName, "SYSTEM");
    }

    public void notifyVisitorAtMeetingRoom(Long visitorId, String visitorName, Long hostUserId) {
        createAndSend(AlertType.VISITOR_AT_MEETING_ROOM, "Visitor Arrived",
                visitorName + " has arrived at the meeting room",
                hostUserId, "HOST", visitorId, visitorName, "SYSTEM");
    }

    public void notifyRestrictedZone(Long visitorId, String visitorName) {
        createAndSend(AlertType.RESTRICTED_ZONE_ALERT, "⚠ RESTRICTED ZONE ALERT",
                visitorName + " has entered a restricted zone!",
                null, "SECURITY_GUARD", visitorId, visitorName, "SYSTEM");
        createAndSend(AlertType.RESTRICTED_ZONE_ALERT, "⚠ RESTRICTED ZONE ALERT",
                visitorName + " has entered a restricted zone!",
                null, "ADMIN", visitorId, visitorName, "SYSTEM");
    }

    public void notifyTimeout(Long visitorId, String visitorName, String location) {
        createAndSend(AlertType.TIMEOUT_ALERT, "Timeout Alert",
                visitorName + " has been at " + location + " for too long",
                null, "SECURITY_GUARD", visitorId, visitorName, "SYSTEM");
    }

    public void notifyEmergency(String message) {
        createAndSend(AlertType.EMERGENCY, "🚨 EMERGENCY ALERT", message,
                null, null, null, null, "SYSTEM");
    }

    public List<Notification> getUnreadForUser(Long userId) {
        return notificationRepository.findByTargetUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getAllForUser(Long userId) {
        return notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getRecent() {
        return notificationRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
            notificationRepository.save(n);
        });
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByTargetUserIdAndReadFalse(userId);
    }
}
