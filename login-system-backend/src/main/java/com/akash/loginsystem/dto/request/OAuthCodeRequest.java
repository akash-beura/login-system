package com.akash.loginsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Body for POST /api/v1/auth/oauth2/token — exchanges one-time OAuth code for tokens. */
@Getter
@Setter
public class OAuthCodeRequest {

    @NotBlank(message = "Code is required")
    private String code;
}
