package com.example.Application.repository;

import com.example.Application.entity.EmergencyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyLogRepository extends JpaRepository<EmergencyLog, Long> {

    List<EmergencyLog> findByStatusOrderByTriggeredAtDesc(String status);

    @Query("SELECT e FROM EmergencyLog e ORDER BY e.triggeredAt DESC")
    List<EmergencyLog> findAllOrderByTriggeredAtDesc();

    Optional<EmergencyLog> findFirstByStatusOrderByTriggeredAtDesc(String status);

    @Query("SELECT COUNT(e) FROM EmergencyLog e WHERE e.status = 'ACTIVE'")
    long countActive();
}
