package com.suitespot.repository;

import com.suitespot.entity.TaxiRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaxiRequestRepository extends JpaRepository<TaxiRequest, Long> {
    List<TaxiRequest> findByStatus(TaxiRequest.RequestStatus status);
    List<TaxiRequest> findByBookingId(Long bookingId);
}
