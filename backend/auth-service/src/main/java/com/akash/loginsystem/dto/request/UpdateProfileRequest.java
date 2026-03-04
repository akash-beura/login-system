package com.akash.loginsystem.dto.request;

/**
 * Request body for PUT /api/v1/users/me.
 * All fields are optional — null values leave the existing data unchanged.
 */
public record UpdateProfileRequest(
    String name,
    String phoneCountryCode,
    String phoneNumber,
    String addressLine1,
    String city,
    String state,
    String zipCode,
    String country
) {}
