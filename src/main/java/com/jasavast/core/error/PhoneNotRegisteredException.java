package com.jasavast.core.error;

public class PhoneNotRegisteredException extends BadRequestAlertException{
    public PhoneNotRegisteredException(){
        super(ErrorConstants.PHONE_NOT_REGISTERED_TYPE,"Phone number not registered","users","404");
    }
}
