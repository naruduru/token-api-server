package com.ruru.tokenapi.api;

import com.ruru.tokenapi.auth.AuthContext;
import com.ruru.tokenapi.auth.AuthenticatedPartnerToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PartnerApiController {
    @GetMapping("/public/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "timestamp", Instant.now()
        );
    }

    @GetMapping("/internal/ping")
    public Map<String, Object> internalPing(HttpServletRequest request) {
        AuthenticatedPartnerToken auth = current(request);
        return Map.of(
            "message", "success",
            "clientId", auth.clientId(),
            "systemCode", auth.systemCode(),
            "callSource", auth.callSource(),
            "scopes", auth.scopes(),
            "tokenId", auth.tokenId()
        );
    }

    private AuthenticatedPartnerToken current(HttpServletRequest request) {
        return (AuthenticatedPartnerToken) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
    }
}
