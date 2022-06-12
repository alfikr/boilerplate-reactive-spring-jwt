package com.jasavast.core.error;

public class EmailNotRegisteredException extends BadRequestAlertException{
    public EmailNotRegisteredException(){
        super(ErrorConstants.EMAIL_NOT_REGISTERED_TYPE,"email tidak terdaftar","users","404key");
    }
}
