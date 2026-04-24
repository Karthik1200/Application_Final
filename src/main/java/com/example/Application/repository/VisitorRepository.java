package com.example.Application.repository;

import com.example.Application.entity.Visitor;
import com.example.Application.enums.VisitorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long> {
    List<Visitor> findByStatus(VisitorStatus status);
    List<Visitor> findByStatusIn(List<VisitorStatus> statuses);
    Optional<Visitor> findByEmail(String email);
    Optional<Visitor> findByPhone(String phone);
    Optional<Visitor> findByQrCode(String qrCode);
    List<Visitor> findByHostUserId(Long hostUserId);
    List<Visitor> findByHostEmail(String hostEmail);

    @Query("SELECT v FROM Visitor v WHERE v.status NOT IN ('CHECKED_OUT','EXPIRED','BLACKLISTED') ORDER BY v.createdAt DESC")
    List<Visitor> findActiveVisitors();

    @Query("SELECT v FROM Visitor v WHERE v.createdAt >= :startDate AND v.createdAt <= :endDate")
    List<Visitor> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT v FROM Visitor v WHERE v.status = 'CHECKED_IN_GATE' ORDER BY v.gateCheckedInAt ASC")
    List<Visitor> findReceptionQueue();

    @Query("SELECT COUNT(v) FROM Visitor v WHERE v.createdAt >= :today")
    long countTodayVisitors(@Param("today") LocalDateTime today);

    List<Visitor> findByIdNumberAndStatusNot(String idNumber, VisitorStatus status);
}
