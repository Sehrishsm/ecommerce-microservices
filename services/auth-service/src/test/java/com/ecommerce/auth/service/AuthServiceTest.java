
package com.ecommerce.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerce.auth.client.UserProfileClient;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.LoginResponse;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserResponse;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.event.UserRegisteredEvent;
import com.ecommerce.auth.exception.InvalidCredentialsException;
import com.ecommerce.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private JwtService jwtService;

        @Mock
        private ApplicationEventPublisher eventPublisher;

        @Mock
        private UserProfileClient userProfileClient;

        private PasswordEncoder passwordEncoder;
        private AuthService authService;

        @BeforeEach
        void setUp() {
                passwordEncoder = new BCryptPasswordEncoder();
                authService = new AuthService(userRepository, passwordEncoder, jwtService, userProfileClient,
                                eventPublisher);
        }

        @Test
        void registerShouldHashPasswordAndNormalizeEmail() {
                RegisterRequest request = new RegisterRequest(
                                "  STUDENT@example.com  ",
                                "MyStrongPass123!",
                                "Test Student",
                                null);

                when(userRepository.existsByEmail("student@example.com"))
                                .thenReturn(false);

                when(userRepository.save(any(User.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                UserResponse response = authService.register(request);

                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(captor.capture());

                User savedUser = captor.getValue();

                assertEquals("student@example.com", savedUser.getEmail());
                assertNotEquals("MyStrongPass123!", savedUser.getPasswordHash());
                assertTrue(passwordEncoder.matches(
                                "MyStrongPass123!",
                                savedUser.getPasswordHash()));

                assertEquals("student@example.com", response.email());
                verify(userProfileClient).createProfile(
                                savedUser.getId(),
                                "Test Student",
                                null);
                ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);

                verify(eventPublisher).publishEvent(eventCaptor.capture());

                UserRegisteredEvent event = eventCaptor.getValue();

                assertEquals("UserRegistered", event.eventType());
                assertEquals(savedUser.getId(), event.userId());
                assertEquals("student@example.com", event.email());
                assertTrue(event.eventId() != null && !event.eventId().isBlank());
                assertTrue(event.occurredAt() != null);
        }

        @Test
        void registerShouldRejectDuplicateEmail() {
                RegisterRequest request = new RegisterRequest(
                                "student@example.com",
                                "MyStrongPass123!",
                                "Test Student",
                                null);

                when(userRepository.existsByEmail("student@example.com"))
                                .thenReturn(true);

                assertThrows(
                                IllegalArgumentException.class,
                                () -> authService.register(request));
                verifyNoInteractions(userProfileClient);
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void registerShouldRejectPasswordOver72Utf8Bytes() {
                String password = "a".repeat(73);

                RegisterRequest request = new RegisterRequest(
                                "student@example.com",
                                password,
                                "Test Student",
                                null);

                assertTrue(
                                password.getBytes(StandardCharsets.UTF_8).length > 72);

                assertThrows(
                                IllegalArgumentException.class,
                                () -> authService.register(request));

                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void loginShouldReturnBearerTokenForValidCredentials() {
                User user = new User(
                                "student@example.com",
                                passwordEncoder.encode("MyStrongPass123!"));

                // Simulate a user already saved in the database.
                try {
                        var idField = User.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(user, 1L);
                } catch (ReflectiveOperationException exception) {
                        throw new RuntimeException(exception);
                }

                when(userRepository.findByEmail("student@example.com"))
                                .thenReturn(java.util.Optional.of(user));
                when(jwtService.generateToken(user)).thenReturn("test-jwt-token");

                LoginResponse response = authService.login(
                                new LoginRequest("STUDENT@example.com", "MyStrongPass123!"));

                assertEquals("test-jwt-token", response.accessToken());
                assertEquals("Bearer", response.tokenType());
                verify(jwtService).generateToken(user);
        }

        @Test
        void loginShouldRejectIncorrectPassword() {
                User user = new User(
                                "student@example.com",
                                passwordEncoder.encode("MyStrongPass123!"));

                when(userRepository.findByEmail("student@example.com"))
                                .thenReturn(java.util.Optional.of(user));

                assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.login(
                                                new LoginRequest(
                                                                "student@example.com",
                                                                "WrongPassword123!")));

                verify(jwtService, never()).generateToken(any(User.class));
        }

        @Test
        void loginShouldRejectUnknownEmail() {
                when(userRepository.findByEmail("unknown@example.com"))
                                .thenReturn(java.util.Optional.empty());

                assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.login(
                                                new LoginRequest(
                                                                "unknown@example.com",
                                                                "MyStrongPass123!")));

                verify(jwtService, never()).generateToken(any(User.class));
        }

}
