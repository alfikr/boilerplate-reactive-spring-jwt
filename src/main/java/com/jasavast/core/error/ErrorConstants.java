package com.jasavast.core.error;

import java.net.URI;

public final class ErrorConstants {
    public static final String APPLICATION_NAME = "Jasavast";
    public static final String ERR_CONCURRENCY_FAILURE = "error.concurrencyFailure";
    public static final String ERR_VALIDATION = "error.validation";
    public static final String PROBLEM_BASE_URL = "https://www.jasavast.com/problem";
    public static final URI DEFAULT_TYPE = URI.create(PROBLEM_BASE_URL + "/problem-with-message");
    public static final URI CONSTRAINT_VIOLATION_TYPE = URI.create(PROBLEM_BASE_URL + "/constraint-violation");
    public static final URI INVALID_PASSWORD_TYPE = URI.create(PROBLEM_BASE_URL + "/invalid-password");
    public static final URI EMAIL_ALREADY_USED_TYPE = URI.create(PROBLEM_BASE_URL + "/email-already-used");
    public static final URI LOGIN_ALREADY_USED_TYPE = URI.create(PROBLEM_BASE_URL + "/login-already-used");
    public static final URI INVALID_KEY_TYPE = URI.create(PROBLEM_BASE_URL + "/invalid-key");
    public static final URI UNAUTHORIZED_TYPE = URI.create(PROBLEM_BASE_URL + "/unauthorized");
    public static final URI FORBIDDEN_TYPE = URI.create(PROBLEM_BASE_URL + "/forbidden");
    public static final URI METHOD_NOT_FOUND_TYPE = URI.create(PROBLEM_BASE_URL + "/method-tidak-ditemukan");
    public static final URI METHOD_NOT_ALLOWED_TYPE = URI.create(PROBLEM_BASE_URL + "/http-method-tidak-diperkenankan");
    public static final URI LOGIN_NOT_REGISTERED_TYPE = URI.create(PROBLEM_BASE_URL + "/login-not-registered");

    private ErrorConstants() {
    }
}
