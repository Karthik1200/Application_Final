package com.example.Application.controller;

import com.example.Application.dto.ApiResponseDTO;
import com.example.Application.entity.BlacklistEntry;
import com.example.Application.repository.BlacklistRepository;
import com.example.Application.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blacklist")
public class BlacklistController {

    @Autowired
    private BlacklistRepository blacklistRepository;

    @Autowired
    private AuditService auditService;

    @GetMapping
    public ApiResponseDTO<List<BlacklistEntry>> getAll() {
        return ApiResponseDTO.success("OK", blacklistRepository.findByActiveTrue());
    }

    @PostMapping
    public ApiResponseDTO<BlacklistEntry> add(@RequestBody Map<String, String> body) {
        String fullName = body.getOrDefault("fullName", "").trim();
        if (fullName.isEmpty()) {
            return ApiResponseDTO.error("Full name is required");
        }

        BlacklistEntry entry = BlacklistEntry.builder()
                .fullName(fullName)
                .email(body.get("email"))
                .phone(body.get("phone"))
                .idNumber(body.get("idNumber"))
                .reason(body.get("reason"))
                .addedBy(body.getOrDefault("addedBy", "admin"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        String expStr = body.get("expiresAt");
        if (expStr != null && !expStr.isBlank()) {
            try { entry.setExpiresAt(LocalDateTime.parse(expStr)); } catch (Exception ignored) {}
        }

        BlacklistEntry saved = blacklistRepository.save(entry);
        Long savedId = saved.getId() != null ? saved.getId() : 0L;
        auditService.log("BLACKLIST_ADD", "BLACKLIST", savedId, entry.getAddedBy(), "ADMIN", "Blacklisted: " + fullName);
        return ApiResponseDTO.success("Entry added to blacklist", saved);
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<Void> remove(@PathVariable Long id) {
        return blacklistRepository.findById(id).map(entry -> {
            entry.setActive(false);
            blacklistRepository.save(entry);
            Long entryId = entry.getId() != null ? entry.getId() : 0L;
            auditService.log("BLACKLIST_REMOVE", "BLACKLIST", entryId, "admin", "ADMIN", "Removed from blacklist: " + entry.getFullName());
            return ApiResponseDTO.<Void>success("Entry removed from blacklist", null);
        }).orElse(ApiResponseDTO.error("Entry not found"));
    }
}
