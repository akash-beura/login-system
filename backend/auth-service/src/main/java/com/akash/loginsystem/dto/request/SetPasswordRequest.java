package com.akash.loginsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetPasswordRequest {

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
    private String confirmPassword;
}
