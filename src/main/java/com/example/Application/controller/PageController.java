package com.example.Application.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/gate")
    public String gateKiosk() {
        return "gate-kiosk";
    }

    @GetMapping("/reception")
    public String reception() {
        return "reception";
    }

    @GetMapping("/tracking")
    public String tracking() {
        return "tracking";
    }

    @GetMapping("/indoor-map")
    public String indoorMap() {
        return "indoor-map";
    }

    @GetMapping("/rooms")
    public String meetingRooms() {
        return "meeting-rooms";
    }

    @GetMapping("/admin")
    public String adminDashboard() {
        return "admin";
    }

    @GetMapping("/notifications")
    public String notificationsPage() {
        return "notifications";
    }

    @GetMapping("/emergency")
    public String emergencyDashboard() {
        return "emergency";
    }

    // ── Role-specific dashboards ───────────────────────────────────────────────

    @GetMapping("/founder-dashboard")
    public String founderDashboard() { return "founder-dashboard"; }

    @GetMapping("/manager-dashboard")
    public String managerDashboard() { return "manager-dashboard"; }

    @GetMapping("/front-office")
    public String frontOfficeDashboard() { return "front-office"; }

    @GetMapping("/room-service")
    public String roomServiceDashboard() { return "room-service"; }

    @GetMapping("/chef-dashboard")
    public String chefDashboard() { return "chef-dashboard"; }

    @GetMapping("/client-portal")
    public String clientPortal() { return "client-portal"; }
}
