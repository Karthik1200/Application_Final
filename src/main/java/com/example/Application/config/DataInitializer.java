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
        if (appUserRepository.count() == 0) {
            appUserRepository.save(AppUser.builder()
                    .username("admin").password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator").email("admin@vrgt.com")
                    .phone("9876543210").department("IT").role(UserRole.ADMIN).active(true).build());

            appUserRepository.save(AppUser.builder()
                    .username("guard1").password(passwordEncoder.encode("guard123"))
                    .fullName("Rajesh Kumar").email("guard1@vrgt.com")
                    .phone("9876543211").department("Security").role(UserRole.SECURITY_GUARD).active(true).build());

            appUserRepository.save(AppUser.builder()
                    .username("reception1").password(passwordEncoder.encode("reception123"))
                    .fullName("Priya Sharma").email("reception1@vrgt.com")
                    .phone("9876543212").department("Front Desk").role(UserRole.RECEPTIONIST).active(true).build());

            appUserRepository.save(AppUser.builder()
                    .username("host1").password(passwordEncoder.encode("host123"))
                    .fullName("Amit Patel").email("host1@vrgt.com")
                    .phone("9876543213").department("Engineering").role(UserRole.HOST).active(true).build());
        }
    }
}
