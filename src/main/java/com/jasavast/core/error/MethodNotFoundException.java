package com.jasavast.core.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static com.jasavast.core.error.ErrorConstants.METHOD_NOT_FOUND_TYPE;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MethodNotFoundException extends BadRequestAlertException{
    public MethodNotFoundException(){
        super(METHOD_NOT_FOUND_TYPE,"Method tidak ditemukan","method","404error");
    }
}
