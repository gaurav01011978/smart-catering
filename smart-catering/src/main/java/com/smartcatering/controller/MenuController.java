package com.smartcatering.controller;

import com.smartcatering.config.CurrentUser;
import com.smartcatering.dto.Dtos.MenuRequest;
import com.smartcatering.entity.MenuItem;
import com.smartcatering.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@PreAuthorize("!hasRole('CUSTOMER')")
public class MenuController {
    private final MenuService menuService;
    private final CurrentUser currentUser;

    @PostMapping
    public MenuItem add(@Valid @RequestBody MenuRequest r) { return menuService.add(currentUser.get(), r); }

    @GetMapping("/my")
    public List<MenuItem> mine() { return menuService.mine(currentUser.get()); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { menuService.delete(currentUser.get(), id); }
}