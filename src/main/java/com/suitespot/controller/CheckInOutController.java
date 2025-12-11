package com.suitespot.controller;

import com.suitespot.entity.Bill;
import com.suitespot.entity.Booking;
import com.suitespot.entity.Room;
import com.suitespot.service.BillingService;
import com.suitespot.service.BookingService;
import com.suitespot.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/checkin-checkout")
public class CheckInOutController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private BillingService billingService;

    // Check-in endpoints
    @GetMapping("/checkin")
    public String checkInPage() {
        return "checkin-checkout/checkin";
    }

    @PostMapping("/checkin/search")
    @Transactional
    public String searchForCheckIn(
            @RequestParam String query,
            Model model) {
        try {
        // Search by booking ID, guest name, or ID number
            // Include PENDING, CONFIRMED, and CHECKED_IN bookings
            List<Booking.BookingStatus> searchStatuses = java.util.Arrays.asList(
                Booking.BookingStatus.PENDING, 
                Booking.BookingStatus.CONFIRMED,
                Booking.BookingStatus.CHECKED_IN
            );
            List<Booking> confirmedBookings = bookingService.getBookingsByStatusesWithRelations(searchStatuses);
            
            // Replace + with space (URL encoding) and normalize
            String normalizedQuery = query.replace("+", " ").trim();
            String queryLower = normalizedQuery.toLowerCase();
            String[] queryParts = queryLower.split("\\s+"); // Split by whitespace
            
        List<Booking> results = confirmedBookings.stream()
                    .filter(b -> {
                        if (b == null) {
                            return false;
                        }
                        
                        // Search by booking ID (exact match or contains)
                        if (b.getId() != null) {
                            String bookingIdStr = b.getId().toString();
                            if (bookingIdStr.equals(normalizedQuery) || bookingIdStr.contains(normalizedQuery)) {
                                return true;
                            }
                        }
                        
                        // Search by guest ID (exact match or contains)
                        if (b.getGuest() != null && b.getGuest().getId() != null) {
                            String guestIdStr = b.getGuest().getId().toString();
                            if (guestIdStr.equals(normalizedQuery) || guestIdStr.contains(normalizedQuery)) {
                                return true;
                            }
                        }
                        
                        // Search by room ID (exact match or contains)
                        if (b.getRoom() != null && b.getRoom().getId() != null) {
                            String roomIdStr = b.getRoom().getId().toString();
                            if (roomIdStr.equals(normalizedQuery) || roomIdStr.contains(normalizedQuery)) {
                                return true;
                            }
                        }
                        
                        // Search by room number
                        if (b.getRoom() != null && b.getRoom().getRoomNumber() != null) {
                            String roomNumber = b.getRoom().getRoomNumber().toLowerCase();
                            if (roomNumber.contains(queryLower)) {
                                return true;
                            }
                        }
                        
                        if (b.getGuest() != null) {
                            String firstName = b.getGuest().getFirstName() != null 
                                ? b.getGuest().getFirstName().toLowerCase() : "";
                            String lastName = b.getGuest().getLastName() != null 
                                ? b.getGuest().getLastName().toLowerCase() : "";
                            String fullName = (firstName + " " + lastName).trim();
                            String idNumber = b.getGuest().getIdNumber() != null 
                                ? b.getGuest().getIdNumber() : "";
                            
                            // Search by ID number
                            if (!idNumber.isEmpty() && idNumber.contains(normalizedQuery)) {
                                return true;
                            }
                            
                            // Search by full name (contains query)
                            if (!fullName.isEmpty() && fullName.contains(queryLower)) {
                                return true;
                            }
                            
                            // Search by individual name parts (if query has multiple words)
                            if (queryParts.length > 1) {
                                // Check if first part matches first name and second part matches last name
                                // Or if any part matches either name
                                boolean firstPartMatches = queryParts[0].isEmpty() || 
                                    firstName.contains(queryParts[0]) || lastName.contains(queryParts[0]);
                                boolean secondPartMatches = queryParts.length < 2 || queryParts[1].isEmpty() || 
                                    firstName.contains(queryParts[1]) || lastName.contains(queryParts[1]);
                                
                                if (firstPartMatches && secondPartMatches) {
                                    return true;
                                }
                            }
                            
                            // Search by first name or last name individually
                            if ((!firstName.isEmpty() && firstName.contains(queryLower)) ||
                                (!lastName.isEmpty() && lastName.contains(queryLower))) {
                                return true;
                            }
                        }
                        return false;
                    })
                .toList();
            
            if (confirmedBookings.isEmpty()) {
                model.addAttribute("error", "No pending, confirmed, or checked-in bookings found in the system. Please create or confirm some bookings first.");
            } else if (results.isEmpty()) {
                model.addAttribute("error", "No bookings match your search. Try searching by booking ID, guest name, guest ID, room ID, or room number.");
            }

        model.addAttribute("searchResults", results);
            model.addAttribute("query", normalizedQuery);
            return "checkin-checkout/checkin-results";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error searching for bookings: " + e.getMessage());
            model.addAttribute("searchResults", List.<Booking>of());
        model.addAttribute("query", query);
        return "checkin-checkout/checkin-results";
        }
    }

    @PostMapping("/{id}/checkin")
    @Transactional
    public String performCheckIn(
            @PathVariable Long id,
            @RequestParam(required = false) String idNumber,
            Model model) {
        try {
            Booking booking = bookingService.getBookingByIdWithRelations(id)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Validate booking has required data
            if (booking.getGuest() == null) {
                model.addAttribute("error", "Booking is missing guest information.");
                List<Booking> availableBookings = bookingService.getBookingsByStatusesWithRelations(
                    List.of(Booking.BookingStatus.PENDING, Booking.BookingStatus.CONFIRMED)
                );
                model.addAttribute("searchResults", availableBookings);
                return "checkin-checkout/checkin-results";
            }

            if (booking.getRoom() == null) {
                model.addAttribute("error", "Booking is missing room information.");
                List<Booking> availableBookings = bookingService.getBookingsByStatusesWithRelations(
                    List.of(Booking.BookingStatus.PENDING, Booking.BookingStatus.CONFIRMED)
                );
                model.addAttribute("searchResults", availableBookings);
                return "checkin-checkout/checkin-results";
            }

            // Verify identity if ID number provided
            if (idNumber != null && !idNumber.trim().isEmpty()) {
                if (booking.getGuest().getIdNumber() == null || 
                    !booking.getGuest().getIdNumber().equalsIgnoreCase(idNumber.trim())) {
                    model.addAttribute("error", "ID number does not match booking. Please verify guest identity.");
                    List<Booking> availableBookings = bookingService.getBookingsByStatusesWithRelations(
                        List.of(Booking.BookingStatus.PENDING, Booking.BookingStatus.CONFIRMED)
                    );
                    model.addAttribute("searchResults", availableBookings);
                    return "checkin-checkout/checkin-results";
                }
            }

            // Validate booking status - allow PENDING (auto-confirm) or CONFIRMED
            if (booking.getStatus() == Booking.BookingStatus.PENDING) {
                // Auto-confirm PENDING bookings during check-in
                bookingService.confirmBooking(booking.getId());
            } else if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
                model.addAttribute("error", "Only pending or confirmed bookings can be checked in. Current status: " + booking.getStatus());
                List<Booking> availableBookings = bookingService.getBookingsByStatusesWithRelations(
                    List.of(Booking.BookingStatus.PENDING, Booking.BookingStatus.CONFIRMED)
                );
                model.addAttribute("searchResults", availableBookings);
                return "checkin-checkout/checkin-results";
            }

            // Get room before updating (to avoid lazy loading issues)
            Room room = booking.getRoom();
            Long roomId = room != null ? room.getId() : null;
            
            // Update booking status
            Booking updatedBooking = bookingService.updateBookingStatus(id, Booking.BookingStatus.CHECKED_IN);

            // Update room status - allow transition from RESERVED or AVAILABLE to OCCUPIED
            if (roomId != null) {
                try {
                    Room currentRoom = roomService.getRoomById(roomId)
                            .orElseThrow(() -> new RuntimeException("Room not found"));
                    Room.RoomStatus currentStatus = currentRoom.getStatus();
                    
                    // If room is already OCCUPIED, that's fine (might be a re-check-in)
                    if (currentStatus == Room.RoomStatus.OCCUPIED) {
                        System.out.println("Room " + currentRoom.getRoomNumber() + " is already OCCUPIED");
                    } else {
                        roomService.updateRoomStatus(roomId, Room.RoomStatus.OCCUPIED);
                    }
                } catch (IllegalStateException e) {
                    // If room status transition is invalid, try to force it (for edge cases)
                    System.err.println("Warning: Standard room status update failed: " + e.getMessage());
                    try {
                        roomService.forceUpdateRoomStatus(roomId, Room.RoomStatus.OCCUPIED);
                        System.out.println("Forced room status update to OCCUPIED");
                    } catch (Exception ex) {
                        System.err.println("Error forcing room status update: " + ex.getMessage());
                        ex.printStackTrace();
                        // Don't fail the check-in if room status update fails
                    }
                } catch (Exception e) {
                    System.err.println("Error updating room status: " + e.getMessage());
                    e.printStackTrace();
                    // Don't fail the check-in if room status update fails
                }
            } else {
                System.err.println("Warning: Booking has no room assigned, skipping room status update");
            }

            model.addAttribute("booking", updatedBooking);
            model.addAttribute("success", "Guest checked in successfully! Room key assigned.");
            return "checkin-checkout/checkin-confirmation";
        } catch (Exception e) {
            e.printStackTrace(); // Log the full stack trace
            model.addAttribute("error", "Error during check-in: " + e.getMessage());
            // Re-fetch search results for the error page
            try {
                List<Booking> availableBookings = bookingService.getBookingsByStatusesWithRelations(
                    List.of(Booking.BookingStatus.PENDING, Booking.BookingStatus.CONFIRMED)
                );
                model.addAttribute("searchResults", availableBookings);
            } catch (Exception ex) {
                model.addAttribute("searchResults", List.<Booking>of());
            }
            return "checkin-checkout/checkin-results";
        }
    }

    // Check-out endpoints
    @GetMapping("/checkout")
    public String checkOutPage() {
        return "checkin-checkout/checkout";
    }

    @PostMapping("/checkout/search")
    @Transactional
    public String searchForCheckOut(
            @RequestParam String query,
            Model model) {
        try {
        // Search by booking ID, guest name, or ID number
            List<Booking> checkedInBookings = bookingService.getBookingsByStatusWithRelations(Booking.BookingStatus.CHECKED_IN);
            // Replace + with space (URL encoding) and normalize
            String normalizedQuery = query.replace("+", " ").trim();
            String queryLower = normalizedQuery.toLowerCase();
            String[] queryParts = queryLower.split("\\s+"); // Split by whitespace
            
        List<Booking> results = checkedInBookings.stream()
                    .filter(b -> {
                        if (b == null) return false;
                        
                        // Search by booking ID
                        if (b.getId() != null && b.getId().toString().contains(normalizedQuery)) {
                            return true;
                        }
                        
                        // Search by guest ID
                        if (b.getGuest() != null && b.getGuest().getId() != null 
                            && b.getGuest().getId().toString().contains(normalizedQuery)) {
                            return true;
                        }
                        
                        // Search by room ID
                        if (b.getRoom() != null && b.getRoom().getId() != null 
                            && b.getRoom().getId().toString().contains(normalizedQuery)) {
                            return true;
                        }
                        
                        // Search by room number
                        if (b.getRoom() != null && b.getRoom().getRoomNumber() != null 
                            && b.getRoom().getRoomNumber().toLowerCase().contains(queryLower)) {
                            return true;
                        }
                        
                        if (b.getGuest() != null) {
                            String firstName = b.getGuest().getFirstName() != null 
                                ? b.getGuest().getFirstName().toLowerCase() : "";
                            String lastName = b.getGuest().getLastName() != null 
                                ? b.getGuest().getLastName().toLowerCase() : "";
                            String fullName = (firstName + " " + lastName).trim();
                            String idNumber = b.getGuest().getIdNumber() != null 
                                ? b.getGuest().getIdNumber() : "";
                            
                            // Search by ID number
                            if (!idNumber.isEmpty() && idNumber.contains(normalizedQuery)) {
                                return true;
                            }
                            
                            // Search by full name (contains query)
                            if (!fullName.isEmpty() && fullName.contains(queryLower)) {
                                return true;
                            }
                            
                            // Search by individual name parts (if query has multiple words)
                            if (queryParts.length > 1) {
                                // Check if first part matches first name and second part matches last name
                                // Or if any part matches either name
                                boolean firstPartMatches = queryParts[0].isEmpty() || 
                                    firstName.contains(queryParts[0]) || lastName.contains(queryParts[0]);
                                boolean secondPartMatches = queryParts.length < 2 || queryParts[1].isEmpty() || 
                                    firstName.contains(queryParts[1]) || lastName.contains(queryParts[1]);
                                
                                if (firstPartMatches && secondPartMatches) {
                                    return true;
                                }
                            }
                            
                            // Search by first name or last name individually
                            if ((!firstName.isEmpty() && firstName.contains(queryLower)) ||
                                (!lastName.isEmpty() && lastName.contains(queryLower))) {
                                return true;
                            }
                        }
                        return false;
                    })
                .toList();

        model.addAttribute("searchResults", results);
            model.addAttribute("query", normalizedQuery);
            return "checkin-checkout/checkout-results";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error searching for bookings: " + e.getMessage());
            model.addAttribute("searchResults", List.<Booking>of());
        model.addAttribute("query", query);
        return "checkin-checkout/checkout-results";
        }
    }

    @PostMapping("/{id}/checkout")
    @Transactional
    public String performCheckOut(@PathVariable Long id, Model model) {
        try {
            Booking booking = bookingService.getBookingByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Validate booking has required data
            if (booking.getGuest() == null) {
                model.addAttribute("error", "Booking is missing guest information.");
                List<Booking> checkedInBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.CHECKED_IN);
                model.addAttribute("searchResults", checkedInBookings);
                return "checkin-checkout/checkout-results";
            }

            if (booking.getRoom() == null) {
                model.addAttribute("error", "Booking is missing room information.");
                List<Booking> checkedInBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.CHECKED_IN);
                model.addAttribute("searchResults", checkedInBookings);
                return "checkin-checkout/checkout-results";
            }

            // Validate booking status
            if (booking.getStatus() != Booking.BookingStatus.CHECKED_IN) {
                model.addAttribute("error", "Only checked-in bookings can be checked out. Current status: " + booking.getStatus());
                List<Booking> checkedInBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.CHECKED_IN);
                model.addAttribute("searchResults", checkedInBookings);
                return "checkin-checkout/checkout-results";
            }

        // Generate bill
            Bill bill;
            try {
                bill = billingService.generateBill(booking);
            } catch (Exception e) {
                System.err.println("Error generating bill: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("error", "Error generating bill: " + e.getMessage());
                List<Booking> checkedInBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.CHECKED_IN);
                model.addAttribute("searchResults", checkedInBookings);
                return "checkin-checkout/checkout-results";
            }

        // Update booking status
        bookingService.updateBookingStatus(id, Booking.BookingStatus.CHECKED_OUT);

        // Update room status back to available
            Long roomId = booking.getRoom().getId();
            if (roomId != null) {
                try {
                    roomService.updateRoomStatus(roomId, Room.RoomStatus.AVAILABLE);
                } catch (IllegalStateException e) {
                    // If room status transition is invalid, try to force it
                    System.err.println("Warning: Standard room status update failed: " + e.getMessage());
                    try {
                        roomService.forceUpdateRoomStatus(roomId, Room.RoomStatus.AVAILABLE);
                        System.out.println("Forced room status update to AVAILABLE");
                    } catch (Exception ex) {
                        System.err.println("Error forcing room status update: " + ex.getMessage());
                        ex.printStackTrace();
                        // Don't fail the check-out if room status update fails
                    }
                } catch (Exception e) {
                    System.err.println("Error updating room status: " + e.getMessage());
                    e.printStackTrace();
                    // Don't fail the check-out if room status update fails
                }
            }

        model.addAttribute("booking", booking);
        model.addAttribute("bill", bill);
        return "checkin-checkout/checkout-confirmation";
        } catch (Exception e) {
            e.printStackTrace(); // Log the full stack trace
            model.addAttribute("error", "Error during check-out: " + e.getMessage());
            // Re-fetch search results for the error page
            try {
                List<Booking> checkedInBookings = bookingService.getBookingsByStatus(Booking.BookingStatus.CHECKED_IN);
                model.addAttribute("searchResults", checkedInBookings);
            } catch (Exception ex) {
                model.addAttribute("searchResults", List.<Booking>of());
            }
            return "checkin-checkout/checkout-results";
        }
    }
}
