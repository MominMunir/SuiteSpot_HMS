package com.suitespot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_name")
    private String hotelName;

    @Column(name = "hotel_email")
    private String hotelEmail;

    @Column(name = "hotel_phone")
    private String hotelPhone;

    @Column(name = "hotel_address")
    private String hotelAddress;

    @Column(name = "tax_rate")
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.valueOf(10.00);

    @Column(name = "service_charge_rate")
    @Builder.Default
    private BigDecimal serviceChargeRate = BigDecimal.valueOf(5.00);

    @Column(name = "currency")
    @Builder.Default
    private String currency = "USD";

    @Column(name = "checkin_time")
    @Builder.Default
    private LocalTime checkInTime = LocalTime.of(14, 0); // 2 PM

    @Column(name = "checkout_time")
    @Builder.Default
    private LocalTime checkOutTime = LocalTime.of(11, 0); // 11 AM

    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;
}
