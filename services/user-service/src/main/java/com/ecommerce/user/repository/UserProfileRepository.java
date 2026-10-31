
package com.ecommerce.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.user.entity.UserProfile;

public interface UserProfileRepository
        extends JpaRepository<UserProfile, Long> {
}
