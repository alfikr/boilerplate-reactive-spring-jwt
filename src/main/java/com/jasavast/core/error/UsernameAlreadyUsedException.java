package com.jasavast.core.error;

public class UsernameAlreadyUsedException extends BadRequestAlertException {
    public UsernameAlreadyUsedException(){
        super(ErrorConstants.LOGIN_ALREADY_USED_TYPE,"Username telah digunakan","user","userUsed");
    }
}
