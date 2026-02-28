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

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .provider(user.getProvider())
                .passwordSet(user.isPasswordSet())
                .build();
    }
}
