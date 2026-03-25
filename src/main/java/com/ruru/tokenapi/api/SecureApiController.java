package com.ruru.tokenapi.api;

import com.ruru.tokenapi.auth.AuthContext;
import com.ruru.tokenapi.auth.AuthenticatedToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SecureApiController {
    @GetMapping("/public/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "timestamp", Instant.now()
        );
    }

    @GetMapping("/secure/ping")
    public Map<String, Object> ping(HttpServletRequest request) {
        AuthenticatedToken auth = current(request);
        return Map.of(
            "message", "pong",
            "clientId", auth.clientId(),
            "tokenId", auth.tokenId(),
            "scopes", auth.scopes()
        );
    }

    @GetMapping("/secure/me")
    public Map<String, Object> me(HttpServletRequest request) {
        AuthenticatedToken auth = current(request);
        return Map.of(
            "clientId", auth.clientId(),
            "tokenId", auth.tokenId(),
            "scopes", auth.scopes(),
            "issuedAt", auth.issuedAt(),
            "expiresAt", auth.expiresAt()
        );
    }

    private AuthenticatedToken current(HttpServletRequest request) {
        return (AuthenticatedToken) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
    }
}
