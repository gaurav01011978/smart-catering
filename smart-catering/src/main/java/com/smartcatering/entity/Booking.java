package com.smartcatering.entity;

import com.smartcatering.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User customer;

    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    private Shift shift;

    private Integer plates = 0;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    private double totalAmount;      // customer ka total (sub-booking ka cost isme nahi, wo caterer ke plate price me hai)
    private double advancePaid;
    private boolean balancePaid;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingItem> items = new ArrayList<>();
}