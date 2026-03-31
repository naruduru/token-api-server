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
            new IssuePartnerTokenRequest("intranet-batch", "internal-secret")
        );

        AuthenticatedPartnerToken authenticatedToken = tokenService.authenticate(issuedToken.accessToken());
        assertNotNull(authenticatedToken);
        assertEquals("intranet-batch", authenticatedToken.clientId());
        assertEquals("order-sync-server", authenticatedToken.systemName());
    }

    @Test
    void rejectsMissingSystemName() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> clientService.register(new RegisterPartnerClientRequest(
                "intranet-batch",
                "internal-secret",
                true,
                PartnerChannel.INTERNAL_SYSTEM,
                "  ",
                java.util.List.of("batch.read")
            ))
        );
        assertEquals("systemName is required for internal clients", exception.getMessage());
    }

    @Test
    void rejectsInvalidClientCredentials() {
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
            () -> tokenService.issueToken(new IssuePartnerTokenRequest("intranet-batch", "wrong-secret"))
        );
        assertEquals("Invalid client credentials", exception.getMessage());
    }

    @Test
    void rejectsNonInternalTokenType() {
        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        String nonInternalToken = io.jsonwebtoken.Jwts.builder()
            .setSubject("intranet-batch")
            .setIssuer("token-api-server")
            .setId("token-1")
            .setExpiration(java.util.Date.from(java.time.Instant.now().plusSeconds(60)))
            .claim("type", "EXTERNAL_USER")
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .compact();

        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        assertNull(tokenService.authenticate(nonInternalToken));
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
