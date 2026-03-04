package com.akash.loginsystem.exception;

/**
 * Thrown when the one-time OAuth2 opaque code is unknown, already consumed,
 * or has exceeded its 30-second TTL in the OAuthTokenStore.
 */
public class OAuthCodeExpiredException extends RuntimeException {
    public OAuthCodeExpiredException() {
        super("Invalid or expired OAuth2 code");
    }
}
