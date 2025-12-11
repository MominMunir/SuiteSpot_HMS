package com.suitespot.service;

import com.suitespot.entity.Guest;
import com.suitespot.repository.GuestRepository;
import com.suitespot.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class GuestService {

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private BookingRepository bookingRepository;

    public Guest createGuest(Guest guest) {
        // Set default values if not provided
        if (guest.getActive() == null) {
            guest.setActive(true);
        }
        return guestRepository.save(guest);
    }

    public Optional<Guest> getGuestById(Long id) {
        return guestRepository.findById(id);
    }

    public List<Guest> getAllGuests() {
        return guestRepository.findAll();
    }

    public List<Guest> searchGuests(String name) {
        return guestRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(name, name);
    }

    public List<Guest> searchByIdNumber(String idNumber) {
        return guestRepository.findByIdNumber(idNumber);
    }

    public Guest updateGuest(Long id, Guest guestDetails) {
        return guestRepository.findById(id).map(guest -> {
            guest.setFirstName(guestDetails.getFirstName());
            guest.setLastName(guestDetails.getLastName());
            guest.setEmail(guestDetails.getEmail());
            guest.setPhone(guestDetails.getPhone());
            guest.setIdNumber(guestDetails.getIdNumber());
            guest.setIdType(guestDetails.getIdType());
            guest.setDateOfBirth(guestDetails.getDateOfBirth());
            guest.setAddress(guestDetails.getAddress());
            guest.setCity(guestDetails.getCity());
            guest.setCountry(guestDetails.getCountry());
            guest.setPreferences(guestDetails.getPreferences());
            return guestRepository.save(guest);
        }).orElseThrow(() -> new RuntimeException("Guest not found"));
    }

    @Transactional
    public void deleteGuest(Long id) {
        // Check if guest exists
        if (!guestRepository.existsById(id)) {
            throw new RuntimeException("Guest not found");
        }
        
        // Check if guest has any bookings
        List<com.suitespot.entity.Booking> bookings = bookingRepository.findByGuestId(id);
        if (bookings != null && !bookings.isEmpty()) {
            throw new RuntimeException(
                "Cannot delete guest. Guest has " + bookings.size() + 
                " associated booking(s). Please delete or cancel the bookings first."
            );
        }
        
        // If no bookings, safe to delete
        guestRepository.deleteById(id);
    }
}
