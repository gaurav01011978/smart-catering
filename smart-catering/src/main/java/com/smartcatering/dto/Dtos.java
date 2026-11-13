package com.smartcatering.dto;

import com.smartcatering.entity.MenuItem;
import com.smartcatering.entity.User;
import com.smartcatering.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public class Dtos {

    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password,
            String phone,
            @NotNull Role role,
            Double basePrice,
            Integer waiterCount,
            Integer captainCount) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, User user) {}

    public record MenuRequest(@NotBlank String name, @NotNull Double price) {}

    public record ProfileRequest(Double basePrice, Integer waiterCount, Integer captainCount) {}

    public record ItemRequest(@NotNull Long vendorId, Integer waiters, Integer captains) {}

    public record BookingRequest(
            @NotNull LocalDate eventDate,
            @NotNull Shift shift,
            Integer plates,
            @NotEmpty List<@Valid ItemRequest> items) {}

    // Vendor list me dikhane ke liye (menu + availability)
    public record VendorView(Long id, String name, String phone, Double basePrice,
                             boolean available, long waitersFree, long captainsFree,
                             List<MenuItem> menu) {}

    // Vendor ko aayi hui booking requests
    public record RequestView(Long itemId, Long bookingId, LocalDate eventDate, Shift shift,
                              Integer plates, int waiters, int captains, double amount,
                              ItemStatus status, String requestedBy, boolean subBooking) {}
}