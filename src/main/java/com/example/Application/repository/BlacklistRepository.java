package com.example.Application.repository;

import com.example.Application.entity.BlacklistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BlacklistRepository extends JpaRepository<BlacklistEntry, Long> {
    List<BlacklistEntry> findByActiveTrue();

    @Query("SELECT b FROM BlacklistEntry b WHERE b.active = true AND (b.email = :email OR b.phone = :phone OR b.idNumber = :idNumber)")
    List<BlacklistEntry> findMatchingBlacklist(@Param("email") String email, @Param("phone") String phone, @Param("idNumber") String idNumber);
}
