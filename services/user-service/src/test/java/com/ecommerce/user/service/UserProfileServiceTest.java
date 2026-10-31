
package com.ecommerce.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.user.dto.CreateUserProfileRequest;
import com.ecommerce.user.dto.UpdateUserProfileRequest;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.repository.UserProfileRepository;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository repository;

    @InjectMocks
    private UserProfileService service;

    private UserProfile profile;

    @BeforeEach
    void setUp() {
        profile = new UserProfile(34L, "Sehrish", "03001234567");
    }

    @Test
    void createShouldSaveProfile() {
        when(repository.existsById(34L)).thenReturn(false);
        when(repository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateUserProfileRequest request = new CreateUserProfileRequest("  Sehrish  ", " 03001234567 ");

        UserProfileResponse response = service.create(34L, request);

        assertEquals(34L, response.authUserId());
        assertEquals("Sehrish", response.fullName());
        assertEquals("03001234567", response.phoneNumber());
        verify(repository).save(any(UserProfile.class));
    }

    @Test
    void createShouldRejectDuplicateProfile() {
        when(repository.existsById(34L)).thenReturn(true);

        CreateUserProfileRequest request = new CreateUserProfileRequest("Sehrish", null);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(34L, request));

        verify(repository, never()).save(any(UserProfile.class));
    }

    @Test
    void getShouldReturnExistingProfile() {
        when(repository.findById(34L)).thenReturn(Optional.of(profile));

        UserProfileResponse response = service.getByAuthUserId(34L);

        assertEquals(34L, response.authUserId());
        assertEquals("Sehrish", response.fullName());
        assertEquals("03001234567", response.phoneNumber());
    }

    @Test
    void getShouldThrowWhenProfileDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                UserProfileNotFoundException.class,
                () -> service.getByAuthUserId(99L));
    }

    @Test
    void updateShouldChangeProfileFields() {
        when(repository.findById(34L)).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("  New Name  ", "  ");

        UserProfileResponse response = service.update(34L, request);

        assertEquals("New Name", response.fullName());
        assertNull(response.phoneNumber());
        verify(repository).save(profile);
    }

    @Test
    void updateShouldThrowWhenProfileDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        UpdateUserProfileRequest request = new UpdateUserProfileRequest("New Name", null);

        assertThrows(
                UserProfileNotFoundException.class,
                () -> service.update(99L, request));

        verify(repository, never()).save(any(UserProfile.class));
    }
}
