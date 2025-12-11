package com.suitespot.controller;

import com.suitespot.entity.Booking;
import com.suitespot.entity.Room;
import com.suitespot.repository.RoomRepository;
import com.suitespot.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public String dashboard(Model model) {
        // Minimal test - set all values to defaults first
        model.addAttribute("totalRooms", 0);
        model.addAttribute("occupiedRooms", 0);
        model.addAttribute("availableRooms", 0);
        model.addAttribute("currentGuests", 0);
        model.addAttribute("recentBookings", List.<Booking>of());
        
        // Try to get data, but don't fail if it errors
        try {
            List<Room> allRooms = roomRepository.findAll();
            if (allRooms != null && !allRooms.isEmpty()) {
                int total = allRooms.size();
                long occupied = allRooms.stream()
                        .filter(r -> r != null && r.getStatus() != null && r.getStatus() == Room.RoomStatus.OCCUPIED)
                        .count();
                long available = allRooms.stream()
                        .filter(r -> r != null && r.getStatus() != null && r.getStatus() == Room.RoomStatus.AVAILABLE)
                        .count();
                
                model.addAttribute("totalRooms", total);
                model.addAttribute("occupiedRooms", occupied);
                model.addAttribute("availableRooms", available);
                model.addAttribute("currentGuests", occupied);
            }
        } catch (Exception e) {
            System.err.println("Error getting rooms: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            // Get all bookings with relations and show the most recent ones
            List<Booking> allBookings = bookingService.getAllBookingsWithRelations();
            if (allBookings != null && !allBookings.isEmpty()) {
                List<Booking> recent = allBookings.stream()
                        .filter(b -> b != null)
                        .sorted((b1, b2) -> {
                            // Sort by created date (most recent first)
                            if (b1.getCreatedAt() != null && b2.getCreatedAt() != null) {
                                return b2.getCreatedAt().compareTo(b1.getCreatedAt());
                            }
                            return 0;
                        })
                        .limit(5)
                        .collect(Collectors.toList());
                model.addAttribute("recentBookings", recent);
            }
        } catch (Exception e) {
            System.err.println("Error getting bookings: " + e.getMessage());
            e.printStackTrace();
        }

        return "dashboard";
    }
}
