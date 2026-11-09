package com.jasavast.core.error;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyUsedException extends BadRequestAlertException {
    public UsernameAlreadyUsedException() {
        super(HttpStatus.CONFLICT, ErrorConstants.LOGIN_ALREADY_USED_TYPE, "Username telah digunakan", "users", "userexists");
    }
}
