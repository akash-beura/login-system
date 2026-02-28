package com.akash.loginsystem.dto.response;

import com.akash.loginsystem.entity.User;
import com.akash.loginsystem.model.AuthProvider;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String name;
    private AuthProvider provider;
    private boolean passwordSet;
    private String phoneCountryCode;
    private String phoneNumber;
    private String addressLine1;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .provider(user.getProvider())
                .passwordSet(user.isPasswordSet())
                .phoneCountryCode(user.getPhoneCountryCode())
                .phoneNumber(user.getPhoneNumber())
                .addressLine1(user.getAddressLine1())
                .city(user.getCity())
                .state(user.getState())
                .zipCode(user.getZipCode())
                .country(user.getCountry())
                .build();
    }
}
