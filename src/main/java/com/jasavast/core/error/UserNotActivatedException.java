package com.jasavast.core.error;

public class UserNotActivatedException extends BadRequestAlertException {
    public UserNotActivatedException(String s){
        super(ErrorConstants.USER_NOT_ACTIVATED_TYPE,s,"user","userNotActivated");
    }
}
