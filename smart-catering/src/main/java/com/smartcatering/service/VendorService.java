package com.smartcatering.service;

import com.smartcatering.dto.Dtos.*;
import com.smartcatering.entity.User;
import com.smartcatering.enums.*;
import com.smartcatering.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorService {
    private final UserRepository userRepo;
    private final BookingItemRepository itemRepo;
    private final MenuItemRepository menuRepo;

    public List<VendorView> list(Role role, LocalDate date, Shift shift) {
        return userRepo.findByRole(role).stream().map(v -> {
            long wFree = 0, cFree = 0;
            boolean available;
            if (v.getRole() == Role.THEKEDAR) {
                wFree = v.getWaiterCount() - itemRepo.waitersBooked(v.getId(), date, shift, ItemStatus.ACTIVE);
                cFree = v.getCaptainCount() - itemRepo.captainsBooked(v.getId(), date, shift, ItemStatus.ACTIVE);
                available = wFree > 0 || cFree > 0;
            } else {
                available = !itemRepo.isBooked(v.getId(), date, shift, ItemStatus.ACTIVE);
            }
            return new VendorView(v.getId(), v.getName(), v.getPhone(), v.getBasePrice(),
                    available, wFree, cFree, menuRepo.findByOwnerId(v.getId()));
        }).toList();
    }

    public User updateProfile(User me, ProfileRequest r) {
        if (r.basePrice() != null) me.setBasePrice(r.basePrice());
        if (r.waiterCount() != null) me.setWaiterCount(r.waiterCount());
        if (r.captainCount() != null) me.setCaptainCount(r.captainCount());
        return userRepo.save(me);
    }
}