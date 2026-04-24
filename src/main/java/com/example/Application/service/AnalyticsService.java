package com.example.Application.service;

import com.example.Application.dto.DashboardStatsDTO;
import com.example.Application.entity.Visitor;
import com.example.Application.enums.RoomStatus;
import com.example.Application.enums.VisitorStatus;
import com.example.Application.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired private VisitorRepository visitorRepository;
    @Autowired private MeetingRoomRepository meetingRoomRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private LocationEventRepository locationEventRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private GateLogRepository gateLogRepository;

    public DashboardStatsDTO getDashboardStats() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();

        long totalToday = visitorRepository.countTodayVisitors(today);
        List<Visitor> active = visitorRepository.findActiveVisitors();

        long pendingGate = active.stream()
                .filter(v -> v.getStatus() == VisitorStatus.CHECKED_IN_GATE).count();
        long pendingReception = active.stream()
                .filter(v -> v.getStatus() == VisitorStatus.AT_RECEPTION
                          || v.getStatus() == VisitorStatus.HOST_CONFIRMED).count();
        long inMeetings = active.stream()
                .filter(v -> v.getStatus() == VisitorStatus.IN_MEETING).count();

        long availableRooms = meetingRoomRepository.findByStatus(RoomStatus.AVAILABLE).size();
        long occupiedRooms = meetingRoomRepository.findByStatus(RoomStatus.OCCUPIED).size();

        long totalAlerts = notificationRepository.findTop50ByOrderByCreatedAtDesc().size();

        // Location heatmap
        List<Object[]> heatmapRaw = locationEventRepository.findLocationHeatmap(today);
        List<Map<String, Object>> heatmap = heatmapRaw.stream().map(row -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("location", row[0].toString());
            entry.put("count", row[1]);
            return entry;
        }).collect(Collectors.toList());

        // Recent activity
        List<Map<String, Object>> recentActivity = new ArrayList<>();
        active.stream().limit(10).forEach(v -> {
            Map<String, Object> activity = new HashMap<>();
            activity.put("visitorName", v.getFullName());
            activity.put("status", v.getStatus().name());
            activity.put("company", v.getCompany());
            activity.put("hostName", v.getHostName());
            activity.put("time", v.getUpdatedAt() != null ? v.getUpdatedAt().toString() : "");
            recentActivity.add(activity);
        });

        return DashboardStatsDTO.builder()
                .totalVisitorsToday(totalToday)
                .activeVisitors(active.size())
                .pendingAtGate(pendingGate)
                .pendingAtReception(pendingReception)
                .inMeetings(inMeetings)
                .availableRooms(availableRooms)
                .occupiedRooms(occupiedRooms)
                .totalAlerts(totalAlerts)
                .restrictedZoneAlerts(0)
                .locationHeatmap(heatmap)
                .recentActivity(recentActivity)
                .build();
    }

    public List<Map<String, Object>> getVisitorAnalytics(int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days);
        List<Visitor> visitors = visitorRepository.findByDateRange(start, LocalDateTime.now());

        Map<String, Long> dailyCounts = visitors.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getCreatedAt().toLocalDate().toString(),
                        Collectors.counting()
                ));

        return dailyCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("date", e.getKey());
                    entry.put("count", e.getValue());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getMeetingAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();

        var todayMeetings = meetingRepository.findByDateRange(today, LocalDateTime.now());
        analytics.put("totalMeetingsToday", todayMeetings.size());
        analytics.put("completedMeetings", todayMeetings.stream()
                .filter(m -> "COMPLETED".equals(m.getStatus())).count());
        analytics.put("activeMeetings", todayMeetings.stream()
                .filter(m -> "IN_PROGRESS".equals(m.getStatus())).count());

        return analytics;
    }
}
