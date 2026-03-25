package com.ruru.tokenapi.auth;

import com.ruru.tokenapi.token.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class BearerTokenInterceptor implements HandlerInterceptor {
    private final TokenService tokenService;

    public BearerTokenInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = extractBearerToken(authorization);
        if (token == null) {
            writeUnauthorized(response, "Missing Bearer token");
            return false;
        }

        AuthenticatedToken authenticatedToken = tokenService.authenticate(token);
        if (authenticatedToken == null) {
            writeUnauthorized(response, "Invalid or expired token");
            return false;
        }

        request.setAttribute(AuthContext.REQUEST_ATTRIBUTE, authenticatedToken);
        return true;
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) return null;
        if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        String value = authorization.substring(7).trim();
        return value.isBlank() ? null : value;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
