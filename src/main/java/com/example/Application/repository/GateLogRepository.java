package com.example.Application.repository;

import com.example.Application.entity.GateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GateLogRepository extends JpaRepository<GateLog, Long> {
    List<GateLog> findByVisitorIdOrderByTimestampDesc(Long visitorId);
    List<GateLog> findByGateIdOrderByTimestampDesc(String gateId);

    @Query("SELECT g FROM GateLog g WHERE g.timestamp >= :start AND g.timestamp <= :end ORDER BY g.timestamp DESC")
    List<GateLog> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT g.gateId, COUNT(g) FROM GateLog g WHERE g.timestamp >= :today GROUP BY g.gateId")
    List<Object[]> countTodayByGate(@Param("today") LocalDateTime today);
}
