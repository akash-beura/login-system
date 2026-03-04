package com.akash.loginsystem.service.impl;

import com.akash.loginsystem.dto.request.UpdateProfileRequest;
import com.akash.loginsystem.dto.response.UserResponse;
import com.akash.loginsystem.entity.User;
import com.akash.loginsystem.repository.UserRepository;
import com.akash.loginsystem.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse updateMe(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (request.name() != null)            user.setName(request.name());
        if (request.phoneCountryCode() != null) user.setPhoneCountryCode(request.phoneCountryCode());
        if (request.phoneNumber() != null)      user.setPhoneNumber(request.phoneNumber());
        if (request.addressLine1() != null)     user.setAddressLine1(request.addressLine1());
        if (request.city() != null)             user.setCity(request.city());
        if (request.state() != null)            user.setState(request.state());
        if (request.zipCode() != null)          user.setZipCode(request.zipCode());
        if (request.country() != null)          user.setCountry(request.country());

        userRepository.save(user);
        log.info("Updated profile for user: {}", userId);
        return UserResponse.from(user);
    }
}
