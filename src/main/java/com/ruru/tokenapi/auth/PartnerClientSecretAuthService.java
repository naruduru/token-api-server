package com.ruru.tokenapi.auth;

import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.partner.CallSource;
import org.springframework.stereotype.Service;

@Service
public class PartnerClientSecretAuthService {
    private final PartnerClientService partnerClientService;
    private final PartnerSharedSecretService partnerSharedSecretService;

    public PartnerClientSecretAuthService(PartnerClientService partnerClientService,
                                          PartnerSharedSecretService partnerSharedSecretService) {
        this.partnerClientService = partnerClientService;
        this.partnerSharedSecretService = partnerSharedSecretService;
    }

    public AuthenticatedPartnerToken authenticate(String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            return null;
        }

        PartnerClient client = partnerClientService.findActiveClient(clientId.trim());
        if (client == null || client.callSource() != CallSource.INTERNAL_BACKEND) {
            return null;
        }
        if (!partnerSharedSecretService.matches(clientSecret)) {
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
        if (client == null || !partnerSharedSecretService.matches(clientSecret)) {
            throw new IllegalArgumentException("Invalid client credentials");
        }
        return client;
    }
}
