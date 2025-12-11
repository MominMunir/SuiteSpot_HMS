package com.suitespot.repository;

import com.suitespot.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStatus(Booking.BookingStatus status);
    List<Booking> findByGuestId(Long guestId);
    List<Booking> findByCheckInDateBetween(LocalDate start, LocalDate end);
    
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.room WHERE b.id = :id")
    Optional<Booking> findByIdWithRelations(@Param("id") Long id);
    
    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.room WHERE b.status = :status")
    List<Booking> findByStatusWithRelations(@Param("status") Booking.BookingStatus status);
    
    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.room WHERE b.status IN (:statuses)")
    List<Booking> findByStatusesWithRelations(@Param("statuses") List<Booking.BookingStatus> statuses);
    
    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.guest LEFT JOIN FETCH b.room ORDER BY b.createdAt DESC")
    List<Booking> findAllWithRelations();
}
