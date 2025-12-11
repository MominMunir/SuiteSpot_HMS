package com.suitespot.controller;

import com.suitespot.entity.Booking;
import com.suitespot.entity.Guest;
import com.suitespot.entity.Room;
import com.suitespot.service.BookingService;
import com.suitespot.service.GuestService;
import com.suitespot.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private GuestService guestService;

    @Autowired
    private RoomService roomService;

    @GetMapping
    public String listBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        try {
            List<Booking> bookings;
            
            // If search query provided, use advanced search
            if (search != null && !search.trim().isEmpty()) {
                try {
                    bookings = bookingService.searchBookings(search, startDate, endDate);
                    model.addAttribute("searchQuery", search);
                } catch (Exception e) {
                    System.err.println("Error in searchBookings: " + e.getMessage());
                    e.printStackTrace();
                    bookings = bookingService.getAllBookings();
                }
            } else if (status != null && !status.isEmpty()) {
                try {
                    bookings = bookingService.getBookingsByStatus(Booking.BookingStatus.valueOf(status));
                    model.addAttribute("selectedStatus", status);
                } catch (IllegalArgumentException e) {
                    bookings = bookingService.getAllBookings();
                } catch (Exception e) {
                    System.err.println("Error getting bookings by status: " + e.getMessage());
                    e.printStackTrace();
                    bookings = bookingService.getAllBookings();
                }
            } else {
                bookings = bookingService.getAllBookings();
            }
            
            // Ensure bookings list is not null
            if (bookings == null) {
                bookings = List.of();
            }
            
            model.addAttribute("bookings", bookings);
            model.addAttribute("bookingStatuses", Booking.BookingStatus.values());
            return "bookings/list";
        } catch (Exception e) {
            System.err.println("Error in listBookings: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("bookings", List.<Booking>of());
            model.addAttribute("bookingStatuses", Booking.BookingStatus.values());
            model.addAttribute("error", "Error loading bookings: " + e.getMessage());
            return "bookings/list";
        }
    }

    @GetMapping("/{id}")
    public String viewBooking(@PathVariable Long id, Model model) {
        Booking booking = bookingService.getBookingById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        model.addAttribute("booking", booking);
        return "bookings/view";
    }

    @GetMapping("/new")
    public String newBookingForm(Model model) {
        model.addAttribute("guests", guestService.getAllGuests());
        model.addAttribute("roomTypes", Room.RoomType.values());
        return "bookings/search";
    }

    @PostMapping("/search")
    public String searchAvailableRooms(
            @RequestParam Long guestId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam(required = false) String roomType,
            Model model) {

        Guest guest = guestService.getGuestById(guestId)
                .orElseThrow(() -> new RuntimeException("Guest not found"));

        Room.RoomType type = null;
        if (roomType != null && !roomType.isEmpty()) {
            try {
                type = Room.RoomType.valueOf(roomType);
            } catch (IllegalArgumentException e) {
                // Ignore invalid room type
            }
        }

        List<Room> availableRooms = bookingService.searchAvailableRooms(checkInDate, checkOutDate, type);

        model.addAttribute("guest", guest);
        model.addAttribute("checkInDate", checkInDate);
        model.addAttribute("checkOutDate", checkOutDate);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("roomTypes", Room.RoomType.values());
        model.addAttribute("selectedType", roomType);

        return "bookings/search-results";
    }

    @PostMapping
    public String createBooking(
            @RequestParam Long guestId,
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam(required = false) String specialRequests) {

        Guest guest = guestService.getGuestById(guestId)
                .orElseThrow(() -> new RuntimeException("Guest not found"));
        Room room = roomService.getRoomById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        Booking booking = Booking.builder()
                .guest(guest)
                .room(room)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .specialRequests(specialRequests)
                .build();

        Booking savedBooking = bookingService.createBooking(booking);
        return "redirect:/bookings/" + savedBooking.getId();
    }

    @PostMapping("/{id}/confirm")
    public String confirmBooking(@PathVariable Long id) {
        bookingService.confirmBooking(id);
        return "redirect:/bookings/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return "redirect:/bookings/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return "redirect:/bookings";
    }
}
