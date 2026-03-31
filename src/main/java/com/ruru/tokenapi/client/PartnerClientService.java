package com.ruru.tokenapi.client;

import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.SystemCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartnerClientService {
    private final PartnerClientStore partnerClientStore;

    public PartnerClientService(PartnerClientStore partnerClientStore) {
        this.partnerClientStore = partnerClientStore;
    }

    public PartnerClient register(RegisterPartnerClientRequest request) {
        String clientId = requireText(request.clientId(), "clientId is required");
        String clientSecret = requireText(request.clientSecret(), "clientSecret is required");
        if (request.systemCode() == null) {
            throw new IllegalArgumentException("systemCode is required");
        }
        if (request.callSource() == null) {
            throw new IllegalArgumentException("callSource is required");
        }
        validateAllowedCombination(request.systemCode(), request.callSource());

        PartnerClient client = new PartnerClient(
            clientId,
            clientSecret,
            request.systemCode(),
            request.callSource(),
            request.active(),
            normalizeScopes(request.scopes()),
            normalizeDescription(request.description())
        );
        partnerClientStore.save(client);
        return client;
    }

    public PartnerClient findActiveClient(String clientId) {
        String normalizedClientId = requireText(clientId, "clientId is required");
        PartnerClient client = partnerClientStore.findByClientId(normalizedClientId);
        if (client == null || !client.active()) {
            return null;
        }
        validateAllowedCombination(client.systemCode(), client.callSource());
        return client;
    }

    public PartnerClient findClient(String clientId) {
        String normalizedClientId = requireText(clientId, "clientId is required");
        return partnerClientStore.findByClientId(normalizedClientId);
    }

    public List<PartnerClient> findAllClients() {
        return partnerClientStore.findAll();
    }

    private void validateAllowedCombination(SystemCode systemCode, CallSource callSource) {
        boolean allowed = switch (systemCode) {
            case GEUMSANGMALL -> callSource == CallSource.DMZ_FRONT;
            case SIGN_COUNSEL, STATISTICS, COUNSEL_APP -> callSource == CallSource.INTERNAL_BACKEND;
        };
        if (!allowed) {
            throw new IllegalArgumentException("systemCode and callSource combination is not allowed");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
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
