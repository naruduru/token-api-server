package com.ruru.tokenapi.api;

import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.token.IssueTokenRequest;
import com.ruru.tokenapi.token.IssueTokenResponse;
import com.ruru.tokenapi.token.IssuedToken;
import com.ruru.tokenapi.token.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/tokens")
public class AdminTokenController {
    private static final String ADMIN_SECRET_HEADER = "X-Admin-Secret";

    private final TokenService tokenService;
    private final TokenApiProperties properties;

    public AdminTokenController(TokenService tokenService, TokenApiProperties properties) {
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @PostMapping
    public IssueTokenResponse issueToken(@RequestHeader(name = ADMIN_SECRET_HEADER, required = false) String adminSecret,
                                         @RequestBody IssueTokenRequest request) {
        validateAdminSecret(adminSecret);
        IssuedToken issuedToken = tokenService.issueToken(request);
        return new IssueTokenResponse(
            "Bearer",
            issuedToken.token(),
            issuedToken.tokenId(),
            issuedToken.clientId(),
            issuedToken.scopes(),
            issuedToken.issuedAt(),
            issuedToken.expiresAt()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private void validateAdminSecret(String adminSecret) {
        String expected = properties.getAdminSecret();
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Admin secret is not configured");
        }
        if (!expected.equals(adminSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin secret");
        }
    }
}
