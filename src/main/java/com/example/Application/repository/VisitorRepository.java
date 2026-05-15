package com.example.Application.repository;

import com.example.Application.entity.Visitor;
import com.example.Application.enums.VisitorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Collection;
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

    @Query("SELECT v FROM Visitor v WHERE v.status NOT IN :statuses ORDER BY v.createdAt DESC")
    List<Visitor> findActiveVisitorsExcluding(@Param("statuses") Collection<VisitorStatus> statuses);

    default List<Visitor> findActiveVisitors() {
        return findActiveVisitorsExcluding(
            List.of(VisitorStatus.CHECKED_OUT, VisitorStatus.EXPIRED, VisitorStatus.BLACKLISTED)
        );
    }

    @Query("SELECT v FROM Visitor v WHERE v.createdAt >= :startDate AND v.createdAt <= :endDate")
    List<Visitor> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT v FROM Visitor v WHERE v.status = :status ORDER BY v.gateCheckedInAt ASC")
    List<Visitor> findReceptionQueueByStatus(@Param("status") VisitorStatus status);

    default List<Visitor> findReceptionQueue() {
        return findReceptionQueueByStatus(VisitorStatus.CHECKED_IN_GATE);
    }

    @Query("SELECT COUNT(v) FROM Visitor v WHERE v.createdAt >= :today")
    long countTodayVisitors(@Param("today") LocalDateTime today);

    List<Visitor> findByIdNumberAndStatusNot(String idNumber, VisitorStatus status);
}
