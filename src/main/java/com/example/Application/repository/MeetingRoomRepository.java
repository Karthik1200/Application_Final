package com.example.Application.repository;

import com.example.Application.entity.MeetingRoom;
import com.example.Application.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {
    Optional<MeetingRoom> findByRoomCode(String roomCode);
    List<MeetingRoom> findByStatus(RoomStatus status);
    List<MeetingRoom> findByFloor(String floor);
    List<MeetingRoom> findByStatusAndCapacityGreaterThanEqual(RoomStatus status, int capacity);
}
