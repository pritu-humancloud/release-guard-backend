package com.release_guard.user_service.controller;

import com.release_guard.user_service.dto.AuthRequest;
import com.release_guard.user_service.dto.AuthResponse;
import com.release_guard.user_service.entity.User;
import com.release_guard.user_service.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        if (userRepository.findByUsername("user").isEmpty()) {
            User defaultUser = new User(UUID.randomUUID(), "user", "1234");
            userRepository.save(defaultUser);
        }
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        if ("user".equals(request.getUsername()) && "1234".equals(request.getPassword())) {
            User user = userRepository.findByUsername("user").orElseGet(() -> {
                User newUser = new User(UUID.randomUUID(), "user", "1234");
                return userRepository.save(newUser);
            });
            String token = Base64.getEncoder().encodeToString((user.getUsername() + ":" + System.currentTimeMillis()).getBytes());
            return new AuthResponse(token, user);
        }
        
        User user = userRepository.findByUsername(request.getUsername())
                .filter(u -> u.getPassword().equals(request.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        
        String token = Base64.getEncoder().encodeToString((user.getUsername() + ":" + System.currentTimeMillis()).getBytes());
        return new AuthResponse(token, user);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // No-op for mock stateless auth
    }

    @GetMapping("/me")
    public User me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        
        String token = authHeader.substring(7);
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            String username = decoded.split(":")[0];
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token format");
        }
    }
}
