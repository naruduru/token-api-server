package com.ruru.tokenapi.partner;

import com.ruru.tokenapi.auth.AuthenticatedPartnerToken;
import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.client.PartnerClientStore;
import com.ruru.tokenapi.client.RegisterPartnerClientRequest;
import com.ruru.tokenapi.config.TokenApiProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartnerTokenServiceTest {
    @Test
    void issuesAndAuthenticatesDmzBackendToken() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "dmz-backend-client",
            "dmz-backend-secret",
            true,
            PartnerChannel.DMZ_BACKEND,
            "dmz-backend",
            java.util.List.of("order.read", "order.write")
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            new IssuePartnerTokenRequest("dmz-backend-client", "dmz-backend-secret", PartnerChannel.DMZ_BACKEND)
        );

        AuthenticatedPartnerToken authenticatedToken = tokenService.authenticate(issuedToken.accessToken());
        assertNotNull(authenticatedToken);
        assertEquals("dmz-backend-client", authenticatedToken.clientId());
        assertEquals(PartnerChannel.DMZ_BACKEND, authenticatedToken.channel());
        assertEquals("dmz-backend", authenticatedToken.systemName());
    }

    @Test
    void rejectsMissingChannelOnRegister() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> clientService.register(new RegisterPartnerClientRequest(
                "system-a-client",
                "system-a-secret",
                true,
                null,
                "internal-a",
                java.util.List.of("batch.read")
            ))
        );
        assertEquals("channel is required", exception.getMessage());
    }

    @Test
    void rejectsMissingChannelOnIssueToken() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "system-a-client",
            "system-a-secret",
            true,
            PartnerChannel.A,
            "internal-a",
            java.util.List.of("batch.read")
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.issueToken(new IssuePartnerTokenRequest("system-a-client", "system-a-secret", null))
        );
        assertEquals("channel is required", exception.getMessage());
    }

    @Test
    void rejectsChannelMismatchCredentials() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "system-a-client",
            "system-a-secret",
            true,
            PartnerChannel.A,
            "internal-a",
            java.util.List.of("batch.read")
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.issueToken(new IssuePartnerTokenRequest("system-a-client", "system-a-secret", PartnerChannel.B))
        );
        assertEquals("Invalid client credentials", exception.getMessage());
    }

    @Test
    void rejectsInvalidTokenType() {
        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        String nonSupportedTypeToken = io.jsonwebtoken.Jwts.builder()
            .setSubject("system-a-client")
            .setIssuer("token-api-server")
            .setId("token-1")
            .setExpiration(Date.from(Instant.now().plusSeconds(60)))
            .claim("type", "UNKNOWN")
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8)))
            .compact();

        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        assertNull(tokenService.authenticate(nonSupportedTypeToken));
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
