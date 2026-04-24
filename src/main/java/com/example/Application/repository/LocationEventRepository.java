package com.example.Application.repository;

import com.example.Application.entity.LocationEvent;
import com.example.Application.enums.VisitorLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationEventRepository extends JpaRepository<LocationEvent, Long> {
    List<LocationEvent> findByVisitorIdOrderByTimestampDesc(Long visitorId);

    Optional<LocationEvent> findFirstByVisitorIdOrderByTimestampDesc(Long visitorId);

    List<LocationEvent> findByRestrictedZoneTrue();

    @Query("SELECT le.location, COUNT(le) FROM LocationEvent le WHERE le.timestamp >= :start GROUP BY le.location ORDER BY COUNT(le) DESC")
    List<Object[]> findLocationHeatmap(@Param("start") LocalDateTime start);

    @Query("SELECT le FROM LocationEvent le WHERE le.location = :location AND le.timestamp >= :start")
    List<LocationEvent> findByLocationAndTimeAfter(@Param("location") VisitorLocation location, @Param("start") LocalDateTime start);

    @Query("SELECT DISTINCT le.visitorId FROM LocationEvent le WHERE le.timestamp >= :cutoff AND le.visitorId NOT IN (SELECT DISTINCT le2.visitorId FROM LocationEvent le2 WHERE le2.timestamp >= :recent)")
    List<Long> findStaleVisitors(@Param("cutoff") LocalDateTime cutoff, @Param("recent") LocalDateTime recent);
}
