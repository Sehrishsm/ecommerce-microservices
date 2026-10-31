
package com.ecommerce.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.user.dto.CreateUserProfileRequest;
import com.ecommerce.user.dto.UpdateUserProfileRequest;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.repository.UserProfileRepository;

@Service
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserProfileResponse create(
            Long authUserId,
            CreateUserProfileRequest request) {

        if (repository.existsById(authUserId)) {
            throw new IllegalArgumentException(
                    "A profile already exists for this user");
        }

        UserProfile profile = new UserProfile(
                authUserId,
                request.fullName().trim(),
                normalizePhone(request.phoneNumber()));

        return toResponse(repository.save(profile));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getByAuthUserId(Long authUserId) {
        UserProfile profile = repository.findById(authUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(authUserId));

        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse update(
            Long authUserId,
            UpdateUserProfileRequest request) {

        UserProfile profile = repository.findById(authUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(authUserId));

        profile.setFullName(request.fullName().trim());
        profile.setPhoneNumber(normalizePhone(request.phoneNumber()));

        return toResponse(repository.save(profile));
    }

    private String normalizePhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        return phoneNumber.trim();
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getAuthUserId(),
                profile.getFullName(),
                profile.getPhoneNumber(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
