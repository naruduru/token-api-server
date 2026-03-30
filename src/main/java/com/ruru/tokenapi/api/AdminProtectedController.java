package com.ruru.tokenapi.api;

import com.ruru.tokenapi.config.TokenApiProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;

public abstract class AdminProtectedController {
    private static final String ADMIN_SECRET_HEADER = "X-Admin-Secret";

    private final TokenApiProperties properties;

    protected AdminProtectedController(TokenApiProperties properties) {
        this.properties = properties;
    }

    @ModelAttribute
    public void validateAdminSecret(@RequestHeader(name = ADMIN_SECRET_HEADER, required = false) String adminSecret) {
        String expected = properties.getAdminSecret();
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Admin secret is not configured");
        }
        if (!expected.equals(adminSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin secret");
        }
    }
}
