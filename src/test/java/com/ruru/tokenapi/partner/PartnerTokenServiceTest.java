package com.ruru.tokenapi.partner;

import com.ruru.tokenapi.auth.AuthenticatedPartnerToken;
import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.client.PartnerClientStore;
import com.ruru.tokenapi.client.RegisterPartnerClientRequest;
import com.ruru.tokenapi.config.TokenApiProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartnerTokenServiceTest {
    @Test
    void issuesAndAuthenticatesExternalUserToken() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "partner-a",
            "secret123",
            true,
            PartnerChannel.EXTERNAL_USER,
            null,
            java.util.List.of("member.read", "order.read")
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            PartnerChannel.EXTERNAL_USER,
            new IssuePartnerTokenRequest("partner-a", "secret123", "user-100")
        );

        AuthenticatedPartnerToken authenticatedToken = tokenService.authenticate(
            PartnerChannel.EXTERNAL_USER,
            issuedToken.accessToken()
        );
        assertNotNull(authenticatedToken);
        assertEquals("partner-a", authenticatedToken.clientId());
        assertEquals("user-100", authenticatedToken.userId());
        assertEquals(PartnerChannel.EXTERNAL_USER, authenticatedToken.channel());
    }

    @Test
    void issuesAndAuthenticatesInternalSystemToken() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "intranet-batch",
            "internal-secret",
            true,
            PartnerChannel.INTERNAL_SYSTEM,
            "order-sync-server",
            java.util.List.of("batch.read", "batch.write")
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            PartnerChannel.INTERNAL_SYSTEM,
            new IssuePartnerTokenRequest("intranet-batch", "internal-secret", null)
        );

        AuthenticatedPartnerToken authenticatedToken = tokenService.authenticate(
            PartnerChannel.INTERNAL_SYSTEM,
            issuedToken.accessToken()
        );
        assertNotNull(authenticatedToken);
        assertEquals("intranet-batch", authenticatedToken.clientId());
        assertEquals("order-sync-server", authenticatedToken.systemName());
        assertEquals(PartnerChannel.INTERNAL_SYSTEM, authenticatedToken.channel());
    }

    @Test
    void rejectsExternalTokenWithoutUserId() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "partner-a",
            "secret123",
            true,
            PartnerChannel.EXTERNAL_USER,
            null,
            java.util.List.of("member.read")
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.issueToken(
                PartnerChannel.EXTERNAL_USER,
                new IssuePartnerTokenRequest("partner-a", "secret123", null)
            )
        );
        assertEquals("userId is required for external user tokens", exception.getMessage());
    }

    @Test
    void rejectsInternalTokenWithUserId() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "intranet-batch",
            "internal-secret",
            true,
            PartnerChannel.INTERNAL_SYSTEM,
            "order-sync-server",
            java.util.List.of("batch.read")
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.issueToken(
                PartnerChannel.INTERNAL_SYSTEM,
                new IssuePartnerTokenRequest("intranet-batch", "internal-secret", "user-100")
            )
        );
        assertEquals("userId is not allowed for internal system tokens", exception.getMessage());
    }

    @Test
    void rejectsChannelMismatch() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "partner-a",
            "secret123",
            true,
            PartnerChannel.EXTERNAL_USER,
            null,
            java.util.List.of("member.read")
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            PartnerChannel.EXTERNAL_USER,
            new IssuePartnerTokenRequest("partner-a", "secret123", "user-100")
        );

        assertNull(tokenService.authenticate(PartnerChannel.INTERNAL_SYSTEM, issuedToken.accessToken()));
    }

    private TokenApiProperties properties() {
        TokenApiProperties properties = new TokenApiProperties();
        properties.setAdminSecret("admin-secret");
        properties.setIssuer("token-api-server");
        properties.setAccessTokenTtlSeconds(1800);
        properties.setJwtSecret("change-me-jwt-secret-change-me-jwt-secret");
        return properties;
    }

    private static class InMemoryPartnerClientStore implements PartnerClientStore {
        private final Map<String, PartnerClient> clients = new HashMap<>();

        @Override
        public void save(PartnerClient client) {
            clients.put(client.clientId(), client);
        }

        @Override
        public PartnerClient findByClientId(String clientId) {
            return clients.get(clientId);
        }
    }

    private static class InMemoryPartnerTokenStore implements PartnerTokenStore {
        private final Map<String, String> activeTokens = new HashMap<>();
        private final Map<String, Boolean> revoked = new HashMap<>();

        @Override
        public void saveActiveToken(PartnerChannel channel, String tokenId, String clientId, Duration ttl) {
            activeTokens.put(channel.name() + ":" + tokenId, clientId);
        }

        @Override
        public String findActiveTokenClientId(PartnerChannel channel, String tokenId) {
            return activeTokens.get(channel.name() + ":" + tokenId);
        }

        @Override
        public boolean isRevoked(PartnerChannel channel, String tokenId) {
            return Boolean.TRUE.equals(revoked.get(channel.name() + ":" + tokenId));
        }
    }
}
