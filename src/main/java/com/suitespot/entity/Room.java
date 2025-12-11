package com.suitespot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "room_number", nullable = false, unique = true)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RoomType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "amenities")
    private String amenities; // Comma-separated or JSON

    @Column(name = "description")
    private String description;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "active")
    private Boolean active = true;

    public enum RoomType {
        SINGLE, DOUBLE, SUITE, DELUXE, PRESIDENTIAL
    }

    public enum RoomStatus {
        AVAILABLE, OCCUPIED, MAINTENANCE, RESERVED
    }
}
