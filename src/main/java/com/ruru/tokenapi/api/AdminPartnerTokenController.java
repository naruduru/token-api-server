package com.ruru.tokenapi.api;

import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.partner.PartnerTokenService;
import com.ruru.tokenapi.partner.RevokePartnerTokenRequest;
import com.ruru.tokenapi.partner.RevokePartnerTokenResponse;
import com.ruru.tokenapi.partner.RevokedPartnerTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/partner-tokens")
public class AdminPartnerTokenController extends AdminProtectedController {
    private final PartnerTokenService partnerTokenService;

    public AdminPartnerTokenController(PartnerTokenService partnerTokenService, TokenApiProperties properties) {
        super(properties);
        this.partnerTokenService = partnerTokenService;
    }

    @PostMapping("/revoke")
    public RevokePartnerTokenResponse revoke(@RequestBody RevokePartnerTokenRequest request) {
        return partnerTokenService.revoke(request.accessToken());
    }

    @GetMapping("/revocations")
    public List<RevokedPartnerTokenResponse> revocations(@RequestParam(defaultValue = "20") int limit) {
        return partnerTokenService.getRevocationHistory(limit);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
