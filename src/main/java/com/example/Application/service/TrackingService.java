package com.example.Application.service;

import com.example.Application.entity.LocationEvent;
import com.example.Application.entity.Visitor;
import com.example.Application.enums.VisitorLocation;
import com.example.Application.enums.VisitorStatus;
import com.example.Application.repository.LocationEventRepository;
import com.example.Application.repository.VisitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrackingService {

    @Autowired private LocationEventRepository locationEventRepository;
    @Autowired private VisitorRepository visitorRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private AuditService auditService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    private static final Set<VisitorLocation> RESTRICTED_ZONES = Set.of(VisitorLocation.RESTRICTED_ZONE);
    private static final long MAX_DWELL_TIME_MINUTES = 30;

    /**
     * Record visitor location event
     */
    public LocationEvent recordLocation(Long visitorId, VisitorLocation location,
                                         String zoneId, String floor,
                                         Double posX, Double posY) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        // Calculate dwell time from previous location
        Long dwellTime = null;
        Optional<LocationEvent> lastEvent = locationEventRepository.findFirstByVisitorIdOrderByTimestampDesc(visitorId);
        if (lastEvent.isPresent()) {
            dwellTime = Duration.between(lastEvent.get().getTimestamp(), LocalDateTime.now()).getSeconds();
        }

        boolean isRestricted = RESTRICTED_ZONES.contains(location);

        LocationEvent event = LocationEvent.builder()
                .visitorId(visitorId)
                .visitorName(visitor.getFullName())
                .location(location)
                .zoneId(zoneId)
                .floor(floor)
                .positionX(posX)
                .positionY(posY)
                .restrictedZone(isRestricted)
                .alertTriggered(isRestricted)
                .dwellTimeSeconds(dwellTime)
                .timestamp(LocalDateTime.now())
                .build();

        event = locationEventRepository.save(event);

        // Alert if restricted zone
        if (isRestricted) {
            notificationService.notifyRestrictedZone(visitorId, visitor.getFullName());
            auditService.log("RESTRICTED_ZONE", "VISITOR", visitorId,
                    "SYSTEM", "SYSTEM", "Visitor entered restricted zone: " + location);
        }

        // Broadcast location update via WebSocket
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "LOCATION_UPDATE");
        wsPayload.put("visitorId", visitorId);
        wsPayload.put("visitorName", visitor.getFullName());
        wsPayload.put("location", location.name());
        wsPayload.put("floor", floor);
        wsPayload.put("posX", posX);
        wsPayload.put("posY", posY);
        wsPayload.put("restricted", isRestricted);
        wsPayload.put("timestamp", LocalDateTime.now().toString());
        messagingTemplate.convertAndSend("/topic/tracking", wsPayload);

        return event;
    }

    /**
     * Get current locations of all active visitors
     */
    public List<Map<String, Object>> getAllVisitorLocations() {
        List<Visitor> activeVisitors = visitorRepository.findActiveVisitors();
        List<Map<String, Object>> locations = new ArrayList<>();

        for (Visitor visitor : activeVisitors) {
            Map<String, Object> loc = new HashMap<>();
            loc.put("visitorId", visitor.getId());
            loc.put("visitorName", visitor.getFullName());
            loc.put("status", visitor.getStatus().name());
            loc.put("company", visitor.getCompany());
            loc.put("hostName", visitor.getHostName());
            loc.put("assignedRoom", visitor.getAssignedRoomId());

            Optional<LocationEvent> lastEvent = locationEventRepository.findFirstByVisitorIdOrderByTimestampDesc(visitor.getId());
            if (lastEvent.isPresent()) {
                LocationEvent le = lastEvent.get();
                loc.put("location", le.getLocation().name());
                loc.put("floor", le.getFloor());
                loc.put("posX", le.getPositionX());
                loc.put("posY", le.getPositionY());
                loc.put("lastUpdate", le.getTimestamp().toString());
                loc.put("restricted", le.isRestrictedZone());
            } else {
                loc.put("location", mapStatusToLocation(visitor.getStatus()));
                loc.put("floor", "1");
                loc.put("posX", getDefaultX(visitor.getStatus()));
                loc.put("posY", getDefaultY(visitor.getStatus()));
                loc.put("lastUpdate", visitor.getUpdatedAt() != null ? visitor.getUpdatedAt().toString() : "");
                loc.put("restricted", false);
            }

            locations.add(loc);
        }

        return locations;
    }

    /**
     * Get visitor movement history
     */
    public List<LocationEvent> getVisitorHistory(Long visitorId) {
        return locationEventRepository.findByVisitorIdOrderByTimestampDesc(visitorId);
    }

    /**
     * Get location heatmap data
     */
    public List<Map<String, Object>> getHeatmapData() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        List<Object[]> rawData = locationEventRepository.findLocationHeatmap(today);

        return rawData.stream().map(row -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("location", ((VisitorLocation) row[0]).name());
            entry.put("count", row[1]);
            return entry;
        }).collect(Collectors.toList());
    }

    /**
     * Check for timeout violations - runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000)
    public void checkTimeoutViolations() {
        List<Visitor> activeVisitors = visitorRepository.findActiveVisitors();
        for (Visitor visitor : activeVisitors) {
            Optional<LocationEvent> lastEvent = locationEventRepository.findFirstByVisitorIdOrderByTimestampDesc(visitor.getId());
            if (lastEvent.isPresent()) {
                long minutesSinceUpdate = Duration.between(lastEvent.get().getTimestamp(), LocalDateTime.now()).toMinutes();
                if (minutesSinceUpdate > MAX_DWELL_TIME_MINUTES) {
                    notificationService.notifyTimeout(visitor.getId(), visitor.getFullName(),
                            lastEvent.get().getLocation().name());
                }
            }
        }
    }

    /**
     * Emergency: Get all visitor locations for evacuation
     */
    public List<Map<String, Object>> getEmergencyLocations() {
        notificationService.notifyEmergency("Emergency protocol activated! All visitors being tracked.");
        return getAllVisitorLocations();
    }

    private String mapStatusToLocation(VisitorStatus status) {
        return switch (status) {
            case CHECKED_IN_GATE -> "GATE";
            case AT_RECEPTION -> "RECEPTION";
            case EN_ROUTE -> "CORRIDOR_A";
            case IN_MEETING -> "MEETING_ROOM";
            default -> "LOBBY";
        };
    }

    private double getDefaultX(VisitorStatus status) {
        return switch (status) {
            case CHECKED_IN_GATE -> 100;
            case AT_RECEPTION -> 250;
            case EN_ROUTE -> 450;
            case IN_MEETING -> 650;
            default -> 50;
        };
    }

    private double getDefaultY(VisitorStatus status) {
        return switch (status) {
            case CHECKED_IN_GATE -> 350;
            case AT_RECEPTION -> 280;
            case EN_ROUTE -> 200;
            case IN_MEETING -> 180;
            default -> 400;
        };
    }
}
