package com.jasavast.core.error;

import org.springframework.http.HttpStatus;

public class MethodNotAllowedException extends BadRequestAlertException {
    public MethodNotAllowedException() {
        super(HttpStatus.METHOD_NOT_ALLOWED, ErrorConstants.METHOD_NOT_ALLOWED_TYPE,
                "Http request tidak diperkenankan", "method", "methodnotallowed");
    }
}
