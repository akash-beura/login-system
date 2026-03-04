package com.akash.loginsystem.service;

import com.akash.loginsystem.dto.request.UpdateProfileRequest;
import com.akash.loginsystem.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    /** Returns the full profile of the authenticated user. */
    UserResponse getMe(UUID userId);

    /**
     * Updates editable profile fields (name, phone, address).
     * Null fields in the request leave existing values unchanged.
     */
    UserResponse updateMe(UUID userId, UpdateProfileRequest request);
}
