package com.example.Application.config;

import com.example.Application.entity.AppUser;
import com.example.Application.enums.UserRole;
import com.example.Application.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create only the initial system founder account if the database is completely empty.
        // All other accounts must be created through the application (registration page or admin panel).
        if (appUserRepository.count() == 0) {
            appUserRepository.save(AppUser.builder()
                    .username("founder")
                    .password(passwordEncoder.encode("ChangeMe@123"))
                    .fullName("System Founder")
                    .email("founder@hotel.local")
                    .phone("")
                    .department("Executive")
                    .role(UserRole.FOUNDER)
                    .active(true)
                    .build());
        }
    }
}
