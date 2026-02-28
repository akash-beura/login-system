package com.akash.loginsystem.exception;

/**
 * Thrown when a caller attempts to use the account-linking set-password flow
 * but the account already has a password. Use a dedicated change-password
 * endpoint (with old-password verification) for that case.
 */
public class PasswordAlreadySetException extends RuntimeException {
    public PasswordAlreadySetException() {
        super("Password is already set for this account. Use the change-password flow.");
    }
}
