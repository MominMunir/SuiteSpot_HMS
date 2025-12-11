package com.suitespot.service;

import com.suitespot.entity.Booking;
import com.suitespot.entity.Guest;
import com.suitespot.entity.Room;
import com.suitespot.repository.BookingRepository;
import com.suitespot.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    public Booking createBooking(Booking booking) {
        // Calculate total amount
        long numberOfNights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        BigDecimal totalAmount = booking.getRoom().getPricePerNight()
                .multiply(BigDecimal.valueOf(numberOfNights));
        booking.setTotalAmount(totalAmount);
        booking.setStatus(Booking.BookingStatus.PENDING);
        return bookingRepository.save(booking);
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    /**
     * Get booking by ID with guest and room relationships eagerly loaded
     * This prevents LazyInitializationException when accessing relationships
     */
    public Optional<Booking> getBookingByIdWithRelations(Long id) {
        return bookingRepository.findByIdWithRelations(id);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /**
     * Get all bookings with guest and room relationships eagerly loaded
     * This prevents LazyInitializationException when accessing relationships
     */
    public List<Booking> getAllBookingsWithRelations() {
        return bookingRepository.findAllWithRelations();
    }

    public List<Booking> getBookingsByGuest(Long guestId) {
        return bookingRepository.findByGuestId(guestId);
    }

    public List<Booking> getBookingsByStatus(Booking.BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    /**
     * Get bookings by status with guest and room relationships eagerly loaded
     * This prevents LazyInitializationException when accessing relationships
     */
    public List<Booking> getBookingsByStatusWithRelations(Booking.BookingStatus status) {
        return bookingRepository.findByStatusWithRelations(status);
    }

    /**
     * Get bookings by multiple statuses with guest and room relationships eagerly loaded
     * This prevents LazyInitializationException when accessing relationships
     */
    public List<Booking> getBookingsByStatusesWithRelations(List<Booking.BookingStatus> statuses) {
        return bookingRepository.findByStatusesWithRelations(statuses);
    }

    public List<Booking> getBookingsByDateRange(LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findByCheckInDateBetween(startDate, endDate);
    }

    /**
     * Search bookings by various criteria (name, ID number, booking ID, date)
     */
    public List<Booking> searchBookings(String query, LocalDate startDate, LocalDate endDate) {
        List<Booking> allBookings = bookingRepository.findAll();
        
        return allBookings.stream()
                .filter(booking -> {
                    if (booking == null) {
                        return false;
                    }
                    
                    // Search by query if provided
                    if (query != null && !query.trim().isEmpty()) {
                        String queryLower = query.toLowerCase().trim();
                        boolean matchesQuery = booking.getId() != null && 
                                         booking.getId().toString().contains(query);
                        
                        // Check guest properties if guest exists
                        if (booking.getGuest() != null) {
                            matchesQuery = matchesQuery ||
                                (booking.getGuest().getFirstName() != null && 
                                 booking.getGuest().getFirstName().toLowerCase().contains(queryLower)) ||
                                (booking.getGuest().getLastName() != null && 
                                 booking.getGuest().getLastName().toLowerCase().contains(queryLower)) ||
                                (booking.getGuest().getIdNumber() != null && 
                                 booking.getGuest().getIdNumber().contains(query)) ||
                                (booking.getGuest().getEmail() != null && 
                                 booking.getGuest().getEmail().toLowerCase().contains(queryLower)) ||
                                (booking.getGuest().getPhone() != null && 
                                 booking.getGuest().getPhone().contains(query));
                        }
                        
                        if (!matchesQuery) {
                            return false;
                        }
                    }
                    
                    // Filter by date range if provided
                    if (startDate != null && booking.getCheckInDate() != null && 
                        booking.getCheckInDate().isBefore(startDate)) {
                        return false;
                    }
                    if (endDate != null && booking.getCheckOutDate() != null && 
                        booking.getCheckOutDate().isAfter(endDate)) {
                        return false;
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }

    public List<Room> searchAvailableRooms(LocalDate checkInDate, LocalDate checkOutDate, Room.RoomType roomType) {
        if (checkInDate == null || checkOutDate == null) {
            return List.of();
        }
        
        List<Booking> conflictingBookings = bookingRepository.findAll().stream()
                .filter(b -> b != null && b.getStatus() != null && b.getStatus() != Booking.BookingStatus.CANCELLED)
                .filter(b -> b.getCheckInDate() != null && b.getCheckOutDate() != null)
                .filter(b -> !isDateRangeAvailable(b.getCheckInDate(), b.getCheckOutDate(), checkInDate, checkOutDate))
                .collect(Collectors.toList());

        List<Long> bookedRoomIds = conflictingBookings.stream()
                .filter(b -> b.getRoom() != null && b.getRoom().getId() != null)
                .map(b -> b.getRoom().getId())
                .collect(Collectors.toList());

        return roomRepository.findAll().stream()
                .filter(r -> r != null && r.getId() != null)
                .filter(r -> !bookedRoomIds.contains(r.getId()))
                .filter(r -> r.getStatus() != null && r.getStatus() == Room.RoomStatus.AVAILABLE)
                .filter(r -> roomType == null || r.getType() == roomType)
                .collect(Collectors.toList());
    }

    public Booking confirmBooking(Long id) {
        return bookingRepository.findById(id).map(booking -> {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            return bookingRepository.save(booking);
        }).orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    public Booking updateBooking(Long id, Booking bookingDetails) {
        return bookingRepository.findById(id).map(booking -> {
            if (bookingDetails.getGuest() != null) {
                booking.setGuest(bookingDetails.getGuest());
            }
            if (bookingDetails.getRoom() != null) {
                booking.setRoom(bookingDetails.getRoom());
            }
            if (bookingDetails.getCheckInDate() != null) {
                booking.setCheckInDate(bookingDetails.getCheckInDate());
            }
            if (bookingDetails.getCheckOutDate() != null) {
                booking.setCheckOutDate(bookingDetails.getCheckOutDate());
            }
            if (bookingDetails.getSpecialRequests() != null) {
                booking.setSpecialRequests(bookingDetails.getSpecialRequests());
            }
            if (bookingDetails.getStatus() != null) {
                booking.setStatus(bookingDetails.getStatus());
            }
            // Recalculate total amount if dates or room changed
            if (bookingDetails.getCheckInDate() != null || bookingDetails.getCheckOutDate() != null || bookingDetails.getRoom() != null) {
                long numberOfNights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
                if (booking.getRoom() != null && booking.getRoom().getPricePerNight() != null) {
                    booking.setTotalAmount(booking.getRoom().getPricePerNight()
                            .multiply(BigDecimal.valueOf(numberOfNights)));
                }
            }
            return bookingRepository.save(booking);
        }).orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    /**
     * Update only the booking status
     */
    public Booking updateBookingStatus(Long id, Booking.BookingStatus status) {
        return bookingRepository.findById(id).map(booking -> {
            booking.setStatus(status);
            return bookingRepository.save(booking);
        }).orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    public Booking cancelBooking(Long id) {
        return bookingRepository.findById(id).map(booking -> {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            return bookingRepository.save(booking);
        }).orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    private boolean isDateRangeAvailable(LocalDate existingStart, LocalDate existingEnd, 
                                         LocalDate newStart, LocalDate newEnd) {
        return newEnd.isBefore(existingStart) || newStart.isAfter(existingEnd);
    }
}
