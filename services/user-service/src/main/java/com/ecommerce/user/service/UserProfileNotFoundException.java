
package com.ecommerce.user.service;

public class UserProfileNotFoundException extends RuntimeException {

    public UserProfileNotFoundException(Long authUserId) {
        super("Profile not found for user ID: " + authUserId);
    }
}
