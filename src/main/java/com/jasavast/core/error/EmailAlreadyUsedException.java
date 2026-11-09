package com.jasavast.core.error;

import org.springframework.http.HttpStatus;

public class EmailAlreadyUsedException extends BadRequestAlertException {
    public EmailAlreadyUsedException() {
        super(HttpStatus.CONFLICT, ErrorConstants.EMAIL_ALREADY_USED_TYPE, "Email used by another account", "users", "emailexists");
    }
}
