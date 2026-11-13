package com.smartcatering.controller;

import com.smartcatering.config.CurrentUser;
import com.smartcatering.dto.Dtos.*;
import com.smartcatering.entity.*;
import com.smartcatering.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final CurrentUser currentUser;

    // ----- CUSTOMER -----
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public Booking create(@Valid @RequestBody BookingRequest r) {
        return bookingService.create(currentUser.get(), r);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<Booking> my() { return bookingService.myBookings(currentUser.get()); }

    @PostMapping("/{id}/pay-advance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Booking payAdvance(@PathVariable Long id) { return bookingService.payAdvance(currentUser.get(), id); }

    @PostMapping("/{id}/pay-balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Booking payBalance(@PathVariable Long id) { return bookingService.payBalance(currentUser.get(), id); }

    // ----- VENDORS -----
    @GetMapping("/requests")
    @PreAuthorize("!hasRole('CUSTOMER')")
    public List<RequestView> requests() { return bookingService.requestsFor(currentUser.get()); }

    @PutMapping("/items/{itemId}/respond")
    @PreAuthorize("!hasRole('CUSTOMER')")
    public BookingItem respond(@PathVariable Long itemId, @RequestParam boolean accept) {
        return bookingService.respond(currentUser.get(), itemId, accept);
    }

    // ----- CATERER only -----
    @PostMapping("/items/{itemId}/sub-book")
    @PreAuthorize("hasRole('CATERER')")
    public BookingItem subBook(@PathVariable Long itemId, @Valid @RequestBody ItemRequest r) {
        return bookingService.subBook(currentUser.get(), itemId, r);
    }
}