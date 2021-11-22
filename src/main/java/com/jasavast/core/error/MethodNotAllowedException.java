package com.jasavast.core.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
public class MethodNotAllowedException extends BadRequestAlertException{
    public MethodNotAllowedException(){
        super(ErrorConstants.METHOD_NOT_ALLOWED_TYPE,"Http request tidak diperkenankan","bean","400error");
    }
}
