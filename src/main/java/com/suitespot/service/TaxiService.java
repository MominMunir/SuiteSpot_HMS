package com.suitespot.service;

import com.suitespot.entity.TaxiRequest;
import com.suitespot.repository.TaxiRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaxiService {

    @Autowired
    private TaxiRequestRepository taxiRequestRepository;

    public TaxiRequest createTaxiRequest(TaxiRequest taxiRequest) {
        taxiRequest.setStatus(TaxiRequest.RequestStatus.PENDING);
        taxiRequest.setRequestedTime(LocalDateTime.now());
        return taxiRequestRepository.save(taxiRequest);
    }

    public Optional<TaxiRequest> getTaxiRequestById(Long id) {
        return taxiRequestRepository.findById(id);
    }

    public List<TaxiRequest> getAllTaxiRequests() {
        return taxiRequestRepository.findAll();
    }

    public List<TaxiRequest> getPendingRequests() {
        return taxiRequestRepository.findByStatus(TaxiRequest.RequestStatus.PENDING);
    }

    public List<TaxiRequest> getTaxiRequestsByBooking(Long bookingId) {
        return taxiRequestRepository.findByBookingId(bookingId);
    }

    public TaxiRequest confirmTaxiRequest(Long id, String driverName, String vehicleNumber, String phoneNumber, Double estimatedCost) {
        return taxiRequestRepository.findById(id).map(request -> {
            request.setStatus(TaxiRequest.RequestStatus.CONFIRMED);
            request.setDriverName(driverName);
            request.setVehicleNumber(vehicleNumber);
            request.setPhoneNumber(phoneNumber);
            request.setEstimatedCost(estimatedCost);
            request.setEstimatedArrivalTime(LocalDateTime.now().plusMinutes(15));
            return taxiRequestRepository.save(request);
        }).orElseThrow(() -> new RuntimeException("Taxi request not found"));
    }

    public TaxiRequest updateStatus(Long id, TaxiRequest.RequestStatus status) {
        return taxiRequestRepository.findById(id).map(request -> {
            request.setStatus(status);
            if (status == TaxiRequest.RequestStatus.COMPLETED) {
                request.setCompletedAt(LocalDateTime.now());
            }
            return taxiRequestRepository.save(request);
        }).orElseThrow(() -> new RuntimeException("Taxi request not found"));
    }

    public TaxiRequest cancelTaxiRequest(Long id) {
        return updateStatus(id, TaxiRequest.RequestStatus.CANCELLED);
    }

    public void deleteTaxiRequest(Long id) {
        taxiRequestRepository.deleteById(id);
    }
}
