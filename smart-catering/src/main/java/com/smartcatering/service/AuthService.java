package com.smartcatering.service;

import com.smartcatering.config.JwtUtil;
import com.smartcatering.dto.Dtos.*;
import com.smartcatering.entity.User;
import com.smartcatering.exception.ApiException;
import com.smartcatering.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest r) {
        if (userRepo.existsByEmail(r.email())) throw new ApiException("Ye email pehle se registered hai");
        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email());
        u.setPassword(encoder.encode(r.password()));
        u.setPhone(r.phone());
        u.setRole(r.role());
        u.setBasePrice(r.basePrice());
        if (r.waiterCount() != null) u.setWaiterCount(r.waiterCount());
        if (r.captainCount() != null) u.setCaptainCount(r.captainCount());
        userRepo.save(u);
        return new AuthResponse(jwtUtil.generate(u.getEmail()), u);
    }

    public AuthResponse login(LoginRequest r) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(r.email(), r.password()));
        User u = userRepo.findByEmail(r.email()).orElseThrow(() -> new ApiException("User nahi mila"));
        return new AuthResponse(jwtUtil.generate(u.getEmail()), u);
    }
}