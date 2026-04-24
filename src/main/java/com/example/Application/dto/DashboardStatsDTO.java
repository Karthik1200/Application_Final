package com.example.Application.dto;

import lombok.*;
import java.util.Map;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardStatsDTO {
    private long totalVisitorsToday;
    private long activeVisitors;
    private long pendingAtGate;
    private long pendingAtReception;
    private long inMeetings;
    private long availableRooms;
    private long occupiedRooms;
    private long totalAlerts;
    private long restrictedZoneAlerts;
    private List<Map<String, Object>> recentActivity;
    private List<Map<String, Object>> locationHeatmap;
    private List<Map<String, Object>> hourlyTraffic;
}
