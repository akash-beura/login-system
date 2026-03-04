package com.akash.loginsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
    private String password;

    private String phoneCountryCode;
    private String phoneNumber;
    private String addressLine1;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
