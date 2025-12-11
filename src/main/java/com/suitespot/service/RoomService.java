package com.suitespot.service;

import com.suitespot.entity.Room;
import com.suitespot.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    public Room createRoom(Room room) {
        // Set default values if not provided
        if (room.getActive() == null) {
            room.setActive(true);
        }
        if (room.getStatus() == null) {
            room.setStatus(Room.RoomStatus.AVAILABLE);
        }
        return roomRepository.save(room);
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public Room getRoomByNumber(String roomNumber) {
        return roomRepository.findAll().stream()
                .filter(r -> r.getRoomNumber().equals(roomNumber))
                .findFirst()
                .orElse(null);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatus(Room.RoomStatus.AVAILABLE);
    }

    public List<Room> getRoomsByType(Room.RoomType type) {
        return roomRepository.findByType(type);
    }

    public Room updateRoom(Long id, Room roomDetails) {
        return roomRepository.findById(id).map(room -> {
            room.setRoomNumber(roomDetails.getRoomNumber());
            room.setType(roomDetails.getType());
            room.setStatus(roomDetails.getStatus());
            room.setPricePerNight(roomDetails.getPricePerNight());
            room.setCapacity(roomDetails.getCapacity());
            room.setAmenities(roomDetails.getAmenities());
            room.setDescription(roomDetails.getDescription());
            room.setFloor(roomDetails.getFloor());
            return roomRepository.save(room);
        }).orElseThrow(() -> new RuntimeException("Room not found"));
    }

    /**
     * Update room status with validation of valid transitions
     * Valid transitions:
     * - AVAILABLE -> OCCUPIED (check-in)
     * - OCCUPIED -> AVAILABLE (check-out)
     * - AVAILABLE -> MAINTENANCE
     * - MAINTENANCE -> AVAILABLE
     * - AVAILABLE -> RESERVED
     * - RESERVED -> AVAILABLE or OCCUPIED
     */
    public Room updateRoomStatus(Long id, Room.RoomStatus newStatus) {
        return roomRepository.findById(id).map(room -> {
            Room.RoomStatus currentStatus = room.getStatus();
            
            // Validate status transition
            if (!isValidStatusTransition(currentStatus, newStatus)) {
                throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
                );
            }
            
            // Log status change (in a real system, this would go to an audit log)
            System.out.println(String.format(
                "Room %s status changed: %s -> %s at %s",
                room.getRoomNumber(),
                currentStatus,
                newStatus,
                java.time.LocalDateTime.now()
            ));
            
            room.setStatus(newStatus);
            return roomRepository.save(room);
        }).orElseThrow(() -> new RuntimeException("Room not found"));
    }
    
    /**
     * Validate if a room status transition is allowed
     */
    private boolean isValidStatusTransition(Room.RoomStatus from, Room.RoomStatus to) {
        // Same status is always valid
        if (from == to) {
            return true;
        }
        
        // Define valid transitions
        return switch (from) {
            case AVAILABLE -> to == Room.RoomStatus.OCCUPIED || 
                            to == Room.RoomStatus.MAINTENANCE || 
                            to == Room.RoomStatus.RESERVED;
            case OCCUPIED -> to == Room.RoomStatus.AVAILABLE || to == Room.RoomStatus.MAINTENANCE;
            case MAINTENANCE -> to == Room.RoomStatus.AVAILABLE;
            case RESERVED -> to == Room.RoomStatus.AVAILABLE || to == Room.RoomStatus.OCCUPIED;
        };
    }
    
    /**
     * Update room status without validation (for admin overrides or special cases)
     */
    public Room forceUpdateRoomStatus(Long id, Room.RoomStatus newStatus) {
        return roomRepository.findById(id).map(room -> {
            room.setStatus(newStatus);
            return roomRepository.save(room);
        }).orElseThrow(() -> new RuntimeException("Room not found"));
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }
}
