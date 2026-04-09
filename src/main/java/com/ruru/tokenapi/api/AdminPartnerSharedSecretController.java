package com.ruru.tokenapi.api;

import com.ruru.tokenapi.auth.PartnerSharedSecretService;
import com.ruru.tokenapi.config.TokenApiProperties;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/partner-shared-secret")
public class AdminPartnerSharedSecretController extends AdminProtectedController {
    private final PartnerSharedSecretService partnerSharedSecretService;

    public AdminPartnerSharedSecretController(PartnerSharedSecretService partnerSharedSecretService,
                                              TokenApiProperties properties) {
        super(properties);
        this.partnerSharedSecretService = partnerSharedSecretService;
    }

    @PostMapping
    public Map<String, Object> issueSharedSecret() {
        return Map.of(
            "message", "shared secret ready",
            "sharedSecret", partnerSharedSecretService.getOrCreate()
        );
    }
}
