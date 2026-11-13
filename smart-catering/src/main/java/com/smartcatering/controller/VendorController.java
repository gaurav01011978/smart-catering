package com.smartcatering.controller;

import com.smartcatering.config.CurrentUser;
import com.smartcatering.dto.Dtos.*;
import com.smartcatering.entity.User;
import com.smartcatering.enums.*;
import com.smartcatering.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {
    private final VendorService vendorService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<VendorView> list(@RequestParam Role role,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam Shift shift) {
        return vendorService.list(role, date, shift);
    }

    @PutMapping("/me")
    @PreAuthorize("!hasRole('CUSTOMER')")
    public User updateProfile(@RequestBody ProfileRequest r) {
        return vendorService.updateProfile(currentUser.get(), r);
    }
}