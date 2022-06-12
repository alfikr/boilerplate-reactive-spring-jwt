package com.jasavast.core.error;

public class LoginNotAlreadyRegisteredException extends BadRequestAlertException{
    public LoginNotAlreadyRegisteredException(){
        super(ErrorConstants.LOGIN_NOT_REGISTERED_TYPE,"Member login not found","users","404");
    }
}
