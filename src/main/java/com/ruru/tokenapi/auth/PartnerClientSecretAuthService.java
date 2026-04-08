package com.ruru.tokenapi.auth;

import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.partner.CallSource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class PartnerClientSecretAuthService {
    private final PartnerClientService partnerClientService;

    public PartnerClientSecretAuthService(PartnerClientService partnerClientService) {
        this.partnerClientService = partnerClientService;
    }

    public AuthenticatedPartnerToken authenticate(String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            return null;
        }

        PartnerClient client = partnerClientService.findActiveClient(clientId.trim());
        if (client == null || client.callSource() != CallSource.INTERNAL_BACKEND) {
            return null;
        }
        if (!matches(client.clientSecret(), clientSecret)) {
            return null;
        }

        return new AuthenticatedPartnerToken(
            "secret:" + client.clientId(),
            client.clientId(),
            client.systemCode(),
            client.callSource(),
            client.scopes()
        );
    }

    public PartnerClient validateClientSecret(String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("clientId and clientSecret are required");
        }
        PartnerClient client = partnerClientService.findActiveClient(clientId.trim());
        if (client == null || !matches(client.clientSecret(), clientSecret)) {
            throw new IllegalArgumentException("Invalid client credentials");
        }
        return client;
    }

    private boolean matches(String expectedValue, String actualValue) {
        if (expectedValue == null || expectedValue.isBlank() || actualValue == null || actualValue.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
            expectedValue.trim().getBytes(StandardCharsets.UTF_8),
            actualValue.trim().getBytes(StandardCharsets.UTF_8)
        );
    }
}
