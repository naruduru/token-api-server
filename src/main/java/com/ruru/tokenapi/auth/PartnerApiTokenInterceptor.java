package com.ruru.tokenapi.auth;

import com.ruru.tokenapi.geumsangmall.GeumsangmallAccessKeyService;
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
    private final GeumsangmallAccessKeyService geumsangmallAccessKeyService;

    public PartnerApiTokenInterceptor(PartnerTokenService partnerTokenService,
                                      GeumsangmallAccessKeyService geumsangmallAccessKeyService) {
        this.partnerTokenService = partnerTokenService;
        this.geumsangmallAccessKeyService = geumsangmallAccessKeyService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        AuthenticatedPartnerToken accessKeyAuth = geumsangmallAccessKeyService.authenticate(extractAccessKey(request));
        if (accessKeyAuth != null) {
            request.setAttribute(AuthContext.REQUEST_ATTRIBUTE, accessKeyAuth);
            return true;
        }

        String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            writeUnauthorized(response, "Missing Bearer token or access key");
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

    private String extractAccessKey(HttpServletRequest request) {
        String accessKey = request.getHeader("X-Geumsangmall-Access-Key");
        if (accessKey == null || accessKey.isBlank()) {
            accessKey = request.getHeader("X-Access-Key");
        }
        return accessKey == null || accessKey.isBlank() ? null : accessKey.trim();
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
