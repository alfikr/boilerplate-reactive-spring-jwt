package com.jasavast.core.error;

import org.springframework.http.HttpStatus;

public class LoginNotAlreadyRegisteredException extends BadRequestAlertException {
    public LoginNotAlreadyRegisteredException() {
        super(HttpStatus.NOT_FOUND, ErrorConstants.LOGIN_NOT_REGISTERED_TYPE, "Member login not found", "users", "usernotfound");
    }
}
