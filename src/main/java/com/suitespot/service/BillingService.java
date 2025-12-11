package com.suitespot.service;

import com.suitespot.entity.Bill;
import com.suitespot.entity.Booking;
import com.suitespot.entity.SystemSettings;
import com.suitespot.repository.BillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BillingService {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private SystemSettingsService systemSettingsService;

    public Bill generateBill(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        
        // Check if bill already exists for this booking
        Optional<Bill> existingBill = billRepository.findByBookingId(booking.getId());
        if (existingBill.isPresent()) {
            return existingBill.get(); // Return existing bill
        }
        
        SystemSettings settings = systemSettingsService.getSettings();
        
        BigDecimal taxRate = settings.getTaxRate() != null 
            ? settings.getTaxRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(0.10); // Default 10%
        
        BigDecimal serviceChargeRate = settings.getServiceChargeRate() != null
            ? settings.getServiceChargeRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(0.05); // Default 5%
        
        // Ensure roomCharges is not null - use booking total amount or calculate from room price
        BigDecimal roomCharges = booking.getTotalAmount();
        if (roomCharges == null || roomCharges.compareTo(BigDecimal.ZERO) <= 0) {
            // Calculate from room price if total amount is missing
            if (booking.getRoom() != null && booking.getRoom().getPricePerNight() != null 
                && booking.getCheckInDate() != null && booking.getCheckOutDate() != null) {
                long numberOfNights = java.time.temporal.ChronoUnit.DAYS.between(
                    booking.getCheckInDate(), booking.getCheckOutDate());
                if (numberOfNights > 0) {
                    roomCharges = booking.getRoom().getPricePerNight()
                        .multiply(BigDecimal.valueOf(numberOfNights));
                } else {
                    roomCharges = booking.getRoom().getPricePerNight(); // At least one night
                }
            } else {
                throw new IllegalArgumentException("Cannot generate bill: booking total amount is missing and cannot be calculated from room price");
            }
        }
        
        BigDecimal serviceCharges = roomCharges.multiply(serviceChargeRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = roomCharges.add(serviceCharges);
        BigDecimal taxes = subtotal.multiply(taxRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = booking.getDiscount() != null ? booking.getDiscount() : BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(taxes).subtract(discount)
                .setScale(2, RoundingMode.HALF_UP);

        Bill bill = Bill.builder()
                .booking(booking)
                .roomCharges(roomCharges)
                .serviceCharges(serviceCharges)
                .taxes(taxes)
                .discount(discount)
                .totalAmount(totalAmount)
                .paymentStatus(Bill.PaymentStatus.PENDING)
                .build();

        return billRepository.save(bill);
    }

    /**
     * Apply discount with eligibility checking
     * Eligibility rules:
     * - Discount cannot exceed 50% of room charges
     * - Discount cannot make total negative
     * - Returns the best applicable discount
     */
    public Bill applyDiscount(Bill bill, BigDecimal discountAmount) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return bill;
        }
        
        BigDecimal maxDiscount = bill.getRoomCharges().multiply(BigDecimal.valueOf(0.50))
                .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal currentDiscount = bill.getDiscount() != null ? bill.getDiscount() : BigDecimal.ZERO;
        BigDecimal newDiscount = currentDiscount.add(discountAmount);
        
        // Apply the best available discount (capped at 50% of room charges)
        BigDecimal applicableDiscount = newDiscount.min(maxDiscount);
        
        bill.setDiscount(applicableDiscount);
        
        // Recalculate total
        BigDecimal subtotal = bill.getRoomCharges().add(bill.getServiceCharges());
        BigDecimal totalBeforeDiscount = subtotal.add(bill.getTaxes());
        BigDecimal finalTotal = totalBeforeDiscount.subtract(applicableDiscount)
                .max(BigDecimal.ZERO) // Ensure total is not negative
                .setScale(2, RoundingMode.HALF_UP);
        
        bill.setTotalAmount(finalTotal);
        return billRepository.save(bill);
    }
    
    /**
     * Check if guest is eligible for discount based on various criteria
     */
    public boolean isEligibleForDiscount(Booking booking, BigDecimal discountAmount) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Check if discount doesn't exceed 50% of room charges
        BigDecimal maxDiscount = booking.getTotalAmount().multiply(BigDecimal.valueOf(0.50));
        return discountAmount.compareTo(maxDiscount) <= 0;
    }

    public Bill markAsPaid(Bill bill) {
        bill.setPaymentStatus(Bill.PaymentStatus.PAID);
        bill.setPaidAt(LocalDateTime.now());
        return bill;
    }

    public Bill markAsPartialPaid(Bill bill) {
        bill.setPaymentStatus(Bill.PaymentStatus.PARTIAL);
        return bill;
    }
}
