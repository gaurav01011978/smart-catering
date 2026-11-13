package com.smartcatering.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartcatering.enums.ItemStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class BookingItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    private Booking booking;

    @ManyToOne
    private User vendor;          // jisko book kiya

    @ManyToOne
    private User requestedBy;     // kisne book kiya (customer ya caterer)

    private int waiters;          // sirf thekedar ke liye
    private int captains;         // sirf thekedar ke liye
    private double amount;

    private boolean chargedToCustomer = true;  // false = caterer ne sub-book kiya

    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.PENDING;
}