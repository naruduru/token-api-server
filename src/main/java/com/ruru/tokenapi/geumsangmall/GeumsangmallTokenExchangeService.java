package com.ruru.tokenapi.geumsangmall;

import com.ruru.tokenapi.partner.IssuedPartnerToken;
import com.ruru.tokenapi.partner.PartnerTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class GeumsangmallTokenExchangeService {
    private final GeumsangmallVerificationClient verificationClient;
    private final PartnerTokenService partnerTokenService;

    public GeumsangmallTokenExchangeService(GeumsangmallVerificationClient verificationClient,
                                            PartnerTokenService partnerTokenService) {
        this.verificationClient = verificationClient;
        this.partnerTokenService = partnerTokenService;
    }

    public IssuedPartnerToken exchange(GeumsangmallTokenExchangeRequest request, HttpServletRequest httpRequest) {
        GeumsangmallVerificationResponse response = verificationClient.verify(new GeumsangmallVerificationRequest(
            request.mallUserId(),
            request.mallSessionId(),
            clientIp(httpRequest),
            httpRequest.getHeader("User-Agent")
        ));
        if (!response.valid()) {
            throw new IllegalArgumentException(response.message() == null || response.message().isBlank()
                ? "Geumsangmall session verification failed"
                : response.message());
        }
        if (response.mallUserId() == null || !response.mallUserId().equals(request.mallUserId())) {
            throw new IllegalArgumentException("Geumsangmall verification response does not match request");
        }
        return partnerTokenService.issueGeumsangmallExchangeToken();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
