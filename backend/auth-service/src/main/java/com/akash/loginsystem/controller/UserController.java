package com.akash.loginsystem.controller;

import com.akash.loginsystem.dto.request.UpdateProfileRequest;
import com.akash.loginsystem.dto.response.UserResponse;
import com.akash.loginsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/v1/users/me
     * Returns the full profile of the authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(userService.getMe(userId));
    }

    /**
     * PUT /api/v1/users/me
     * Updates editable profile fields. Null fields are ignored (partial update).
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(userService.updateMe(userId, request));
    }
}
