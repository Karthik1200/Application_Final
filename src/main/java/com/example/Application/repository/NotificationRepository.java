package com.example.Application.repository;

import com.example.Application.entity.Notification;
import com.example.Application.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTargetUserIdAndReadFalseOrderByCreatedAtDesc(Long targetUserId);
    List<Notification> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);
    List<Notification> findByTargetRoleOrderByCreatedAtDesc(String targetRole);
    List<Notification> findByVisitorIdOrderByCreatedAtDesc(Long visitorId);
    List<Notification> findByAlertType(AlertType alertType);
    long countByTargetUserIdAndReadFalse(Long targetUserId);
    List<Notification> findTop50ByOrderByCreatedAtDesc();
}
