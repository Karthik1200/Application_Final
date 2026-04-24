package com.example.Application.repository;

import com.example.Application.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByVisitorId(Long visitorId);
    List<Meeting> findByHostId(Long hostId);
    List<Meeting> findByRoomId(Long roomId);
    List<Meeting> findByStatus(String status);
    List<Meeting> findByRoomIdAndStatus(Long roomId, String status);

    @Query("SELECT m FROM Meeting m WHERE m.status = 'IN_PROGRESS'")
    List<Meeting> findActiveMeetings();

    @Query("SELECT m FROM Meeting m WHERE m.createdAt >= :start AND m.createdAt <= :end")
    List<Meeting> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT AVG(DATEDIFF(MINUTE, m.actualStart, m.actualEnd)) FROM Meeting m WHERE m.actualEnd IS NOT NULL AND m.createdAt >= :start")
    Double findAverageDuration(@Param("start") LocalDateTime start);
}
