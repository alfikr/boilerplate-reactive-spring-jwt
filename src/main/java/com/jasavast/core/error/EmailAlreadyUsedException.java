package com.jasavast.core.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EmailAlreadyUsedException extends BadRequestAlertException{
    public EmailAlreadyUsedException(){
        super(ErrorConstants.EMAIL_ALREADY_USED_TYPE,"email used by another account","users","400Error");
    }
}
