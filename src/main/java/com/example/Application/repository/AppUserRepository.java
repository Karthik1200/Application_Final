package com.example.Application.repository;

import com.example.Application.entity.AppUser;
import com.example.Application.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByEmail(String email);
    List<AppUser> findByRole(UserRole role);
    List<AppUser> findByActiveTrue();
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
