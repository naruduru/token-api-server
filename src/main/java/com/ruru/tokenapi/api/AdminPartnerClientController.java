package com.ruru.tokenapi.api;

import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.client.RegisterPartnerClientRequest;
import com.ruru.tokenapi.config.TokenApiProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/partner-clients")
public class AdminPartnerClientController extends AdminProtectedController {
    private final PartnerClientService partnerClientService;

    public AdminPartnerClientController(PartnerClientService partnerClientService, TokenApiProperties properties) {
        super(properties);
        this.partnerClientService = partnerClientService;
    }

    @PostMapping
    public Map<String, String> registerClient(@RequestBody RegisterPartnerClientRequest request) {
        String clientId = partnerClientService.register(request);
        return Map.of(
            "message", "client registered",
            "clientId", clientId
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
