package com.jasavast.core.error;

import org.springframework.http.HttpStatus;

public class MethodNotFoundException extends BadRequestAlertException {
    public MethodNotFoundException() {
        super(HttpStatus.NOT_FOUND, ErrorConstants.METHOD_NOT_FOUND_TYPE, "Method tidak ditemukan", "method", "methodnotfound");
    }
}
