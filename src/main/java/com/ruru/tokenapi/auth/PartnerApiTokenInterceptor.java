package com.ruru.tokenapi.auth;

import com.ruru.tokenapi.partner.PartnerTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class PartnerApiTokenInterceptor implements HandlerInterceptor {
    private final PartnerTokenService partnerTokenService;
    private final PartnerClientSecretAuthService partnerClientSecretAuthService;

    public PartnerApiTokenInterceptor(PartnerTokenService partnerTokenService,
                                      PartnerClientSecretAuthService partnerClientSecretAuthService) {
        this.partnerTokenService = partnerTokenService;
        this.partnerClientSecretAuthService = partnerClientSecretAuthService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        AuthenticatedPartnerToken secretAuth = partnerClientSecretAuthService.authenticate(
            request.getHeader("X-Client-Id"),
            request.getHeader("X-Client-Secret")
        );
        if (secretAuth != null) {
            request.setAttribute(AuthContext.REQUEST_ATTRIBUTE, secretAuth);
            return true;
        }

        String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            writeUnauthorized(response, "Missing Bearer token or client secret");
            return false;
        }

        AuthenticatedPartnerToken authenticatedToken = partnerTokenService.authenticate(token);
        if (authenticatedToken == null) {
            writeUnauthorized(response, "Invalid or expired token");
            return false;
        }

        request.setAttribute(AuthContext.REQUEST_ATTRIBUTE, authenticatedToken);
        return true;
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isBlank() ? null : token;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
