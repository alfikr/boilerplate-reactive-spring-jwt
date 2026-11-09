package com.jasavast.core.util;

import org.springframework.http.HttpHeaders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class HeaderUtil {

    private HeaderUtil() {
    }

    public static HttpHeaders createAlert(String applicationName, String message, String param) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-" + applicationName + "-alert", message);
        headers.add("X-" + applicationName + "-params", URLEncoder.encode(param, StandardCharsets.UTF_8));
        return headers;
    }

    public static HttpHeaders createEntityCreationAlert(String applicationName, boolean enableTranslation, String entityName, String param) {
        String message = enableTranslation ? applicationName + "." + entityName + ".created"
                : "A new " + entityName + " is created with identifier " + param;
        return createAlert(applicationName, message, param);
    }

    public static HttpHeaders createEntityUpdateAlert(String applicationName, boolean enableTranslation, String entityName, String param) {
        String message = enableTranslation ? applicationName + "." + entityName + ".updated"
                : "A " + entityName + " is updated with identifier " + param;
        return createAlert(applicationName, message, param);
    }

    public static HttpHeaders createEntityDeletionAlert(String applicationName, boolean enableTranslation, String entityName, String param) {
        String message = enableTranslation ? applicationName + "." + entityName + ".deleted"
                : "A " + entityName + " is deleted with identifier " + param;
        return createAlert(applicationName, message, param);
    }

    /** Pure function; logging is the caller's (exception handler's) responsibility. */
    public static HttpHeaders createFailureAlert(String applicationName, boolean enableTranslation, String entityName,
                                                 String errorKey, String defaultMessage) {
        String message = enableTranslation ? "error." + errorKey : defaultMessage;
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-" + applicationName + "-error", URLEncoder.encode(message, StandardCharsets.UTF_8));
        headers.add("X-" + applicationName + "-params", entityName);
        return headers;
    }
}
