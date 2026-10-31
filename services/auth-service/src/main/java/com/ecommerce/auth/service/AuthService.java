
package com.ecommerce.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.auth.client.UserProfileClient;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.LoginResponse;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserResponse;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.event.UserRegisteredEvent;
import com.ecommerce.auth.exception.InvalidCredentialsException;
import com.ecommerce.auth.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserProfileClient userProfileClient;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserProfileClient userProfileClient,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userProfileClient = userProfileClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String password = request.password();
        String fullName = request.fullName().trim();

        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes > 72) {
            throw new IllegalArgumentException(
                    "Password must not exceed 72 UTF-8 bytes");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists");
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(email, hashedPassword);
        User savedUser = userRepository.save(user);

        userProfileClient.createProfile(
                savedUser.getId(),
                fullName,
                request.phoneNumber());

        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                "UserRegistered",
                Instant.now(),
                savedUser.getId(),
                savedUser.getEmail());

        eventPublisher.publishEvent(event);

        return new UserResponse(savedUser.getId(), savedUser.getEmail());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(token, "Bearer");
    }
}
