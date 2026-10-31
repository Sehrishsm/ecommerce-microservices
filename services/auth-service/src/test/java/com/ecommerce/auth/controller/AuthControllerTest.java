
package com.ecommerce.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserResponse;
import com.ecommerce.auth.exception.GlobalExceptionHandler;
import com.ecommerce.auth.exception.InvalidCredentialsException;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;
        @MockitoBean
        private JwtService jwtService;
        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        @Test
        void registerShouldReturn201ForValidRequest() throws Exception {
                RegisterRequest request = new RegisterRequest(
                                "student@example.com",
                                "MyStrongPass123!",
                                "Test Student",
                                null);

                when(authService.register(any(RegisterRequest.class)))
                                .thenReturn(new UserResponse(1L, "student@example.com"));

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.email").value("student@example.com"))
                                .andExpect(jsonPath("$.password").doesNotExist())
                                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        void registerShouldReturn400ForInvalidEmail() throws Exception {
                String request = """
                                {
                                    "email": "not-an-email",
                                    "password": "MyStrongPass123!",
                                        "fullName": "Test Student"
                                }
                                """;

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        void registerShouldReturn409ForDuplicateEmail() throws Exception {
                RegisterRequest request = new RegisterRequest(
                                "student@example.com",
                                "MyStrongPass123!",
                                "Test Student",
                                null);

                when(authService.register(any(RegisterRequest.class)))
                                .thenThrow(new IllegalArgumentException(
                                                "An account with this email already exists"));

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.error")
                                                .value("An account with this email already exists"));
        }

        @Test
        void loginShouldReturn401ForInvalidCredentials() throws Exception {
                LoginRequest request = new LoginRequest(
                                "student@example.com",
                                "WrongPassword123!");

                when(authService.login(any(LoginRequest.class)))
                                .thenThrow(new InvalidCredentialsException());

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error")
                                                .value("Invalid email or password"));
        }

}
