package com.smartcatering.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartcatering.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    private String password;

    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Double basePrice;          // Caterer: per plate | Baaki vendors: per shift
    private Integer waiterCount = 0;   // sirf THEKEDAR ke liye
    private Integer captainCount = 0;  // sirf THEKEDAR ke liye
}