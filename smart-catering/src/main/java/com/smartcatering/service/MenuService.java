package com.smartcatering.service;

import com.smartcatering.dto.Dtos.MenuRequest;
import com.smartcatering.entity.*;
import com.smartcatering.exception.ApiException;
import com.smartcatering.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuItemRepository menuRepo;

    public MenuItem add(User owner, MenuRequest r) {
        MenuItem m = new MenuItem();
        m.setName(r.name());
        m.setPrice(r.price());
        m.setOwner(owner);
        return menuRepo.save(m);
    }

    public List<MenuItem> mine(User owner) { return menuRepo.findByOwnerId(owner.getId()); }

    public void delete(User owner, Long id) {
        MenuItem m = menuRepo.findById(id).orElseThrow(() -> new ApiException("Menu item nahi mila"));
        if (!m.getOwner().getId().equals(owner.getId())) throw new ApiException("Ye item aapka nahi hai");
        menuRepo.delete(m);
    }
}