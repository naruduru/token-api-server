package com.ruru.tokenapi.api;

import com.ruru.tokenapi.partner.IssuePartnerTokenRequest;
import com.ruru.tokenapi.partner.IssuePartnerTokenResponse;
import com.ruru.tokenapi.partner.IssuedPartnerToken;
import com.ruru.tokenapi.partner.PartnerTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/internal")
public class PartnerTokenController {
    private final PartnerTokenService partnerTokenService;

    public PartnerTokenController(PartnerTokenService partnerTokenService) {
        this.partnerTokenService = partnerTokenService;
    }

    @PostMapping("/token")
    public IssuePartnerTokenResponse issueInternalToken(@RequestBody IssuePartnerTokenRequest request) {
        return toResponse(partnerTokenService.issueToken(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private IssuePartnerTokenResponse toResponse(IssuedPartnerToken issuedToken) {
        return new IssuePartnerTokenResponse(
            issuedToken.accessToken(),
            "Bearer",
            issuedToken.expiresIn()
        );
    }
}
