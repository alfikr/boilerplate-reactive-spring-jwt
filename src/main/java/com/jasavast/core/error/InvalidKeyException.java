package com.jasavast.core.error;

/** Activation or password-reset key is unknown, already used, or expired. */
public class InvalidKeyException extends BadRequestAlertException {
    public InvalidKeyException(String what) {
        super(ErrorConstants.INVALID_KEY_TYPE, what + " key is invalid or expired", "users", "invalidkey");
    }
}
