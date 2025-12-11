package com.suitespot.controller;

import com.suitespot.entity.Booking;
import com.suitespot.entity.TaxiRequest;
import com.suitespot.service.TaxiService;
import com.suitespot.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/taxi")
public class TaxiController {

    @Autowired
    private TaxiService taxiService;

    @Autowired
    private BookingService bookingService;

    @GetMapping
    public String listTaxiRequests(@RequestParam(required = false) String status, Model model) {
        if (status != null && !status.isEmpty()) {
            try {
                model.addAttribute("taxiRequests", 
                    taxiService.getAllTaxiRequests().stream()
                        .filter(r -> r.getStatus().toString().equals(status))
                        .toList());
                model.addAttribute("selectedStatus", status);
            } catch (Exception e) {
                model.addAttribute("taxiRequests", taxiService.getAllTaxiRequests());
            }
        } else {
            model.addAttribute("taxiRequests", taxiService.getAllTaxiRequests());
        }
        model.addAttribute("requestStatuses", TaxiRequest.RequestStatus.values());
        return "taxi/list";
    }

    @GetMapping("/{id}")
    public String viewTaxiRequest(@PathVariable Long id, Model model) {
        TaxiRequest taxiRequest = taxiService.getTaxiRequestById(id)
                .orElseThrow(() -> new RuntimeException("Taxi request not found"));
        model.addAttribute("taxiRequest", taxiRequest);
        return "taxi/view";
    }

    @GetMapping("/booking/{bookingId}/new")
    public String newTaxiRequestForm(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        model.addAttribute("booking", booking);
        return "taxi/form";
    }

    @PostMapping
    @Transactional
    public String createTaxiRequest(
            @RequestParam Long bookingId,
            @RequestParam String destination,
            @RequestParam(required = false) String notes,
            Model model) {
        try {
            Booking booking = bookingService.getBookingByIdWithRelations(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        TaxiRequest taxiRequest = TaxiRequest.builder()
                .booking(booking)
                    .pickupLocation("Hotel")
                .destination(destination)
                .notes(notes)
                .build();

        TaxiRequest savedRequest = taxiService.createTaxiRequest(taxiRequest);
        return "redirect:/taxi/" + savedRequest.getId();
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error creating taxi request: " + e.getMessage());
            try {
                Booking booking = bookingService.getBookingByIdWithRelations(bookingId)
                        .orElse(null);
                if (booking != null) {
                    model.addAttribute("booking", booking);
                }
            } catch (Exception ex) {
                // Ignore
            }
            return "taxi/form";
        }
    }

    @PostMapping("/{id}/confirm")
    public String confirmTaxiRequest(
            @PathVariable Long id,
            @RequestParam String driverName,
            @RequestParam String vehicleNumber,
            @RequestParam String phoneNumber,
            @RequestParam Double estimatedCost) {

        taxiService.confirmTaxiRequest(id, driverName, vehicleNumber, phoneNumber, estimatedCost);
        return "redirect:/taxi/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateTaxiStatus(
            @PathVariable Long id,
            @RequestParam TaxiRequest.RequestStatus status) {

        taxiService.updateStatus(id, status);
        return "redirect:/taxi/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelTaxiRequest(@PathVariable Long id) {
        taxiService.cancelTaxiRequest(id);
        return "redirect:/taxi/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteTaxiRequest(@PathVariable Long id) {
        taxiService.deleteTaxiRequest(id);
        return "redirect:/taxi";
    }
}
