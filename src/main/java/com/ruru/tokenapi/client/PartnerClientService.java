package com.ruru.tokenapi.client;

import com.ruru.tokenapi.partner.PartnerChannel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartnerClientService {
    private final PartnerClientStore partnerClientStore;

    public PartnerClientService(PartnerClientStore partnerClientStore) {
        this.partnerClientStore = partnerClientStore;
    }

    public String register(RegisterPartnerClientRequest request) {
        String clientId = requireText(request.clientId(), "clientId is required");
        String clientSecret = requireText(request.clientSecret(), "clientSecret is required");
        PartnerChannel channel = request.channel();
        if (channel == null) {
            throw new IllegalArgumentException("channel is required");
        }

        String systemName = requireText(request.systemName(), "systemName is required");
        List<String> scopes = normalizeScopes(request.scopes());
        partnerClientStore.save(new PartnerClient(clientId, clientSecret, request.active(), channel, systemName, scopes));
        return clientId;
    }

    public PartnerClient findActiveClient(PartnerChannel channel, String clientId) {
        PartnerClient client = findActiveClient(clientId);
        if (client == null || client.channel() != channel) {
            return null;
        }
        return client;
    }

    public PartnerClient findActiveClient(String clientId) {
        String normalizedClientId = requireText(clientId, "clientId is required");
        PartnerClient client = partnerClientStore.findByClientId(normalizedClientId);
        if (client == null || !client.active()) {
            return null;
        }
        return client;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private List<String> normalizeScopes(List<String> scopes) {
        if (scopes == null) {
            return List.of();
        }
        return scopes.stream()
            .filter(scope -> scope != null && !scope.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    }
}
