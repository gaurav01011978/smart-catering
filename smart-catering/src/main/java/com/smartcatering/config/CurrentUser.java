package com.smartcatering.config;

import com.smartcatering.entity.User;
import com.smartcatering.exception.ApiException;
import com.smartcatering.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final UserRepository userRepo;

    public User get() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email).orElseThrow(() -> new ApiException("User nahi mila"));
    }
}