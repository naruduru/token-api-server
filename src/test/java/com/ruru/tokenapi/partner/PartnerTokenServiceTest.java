package com.ruru.tokenapi.partner;

import com.ruru.tokenapi.auth.AuthenticatedPartnerToken;
import com.ruru.tokenapi.auth.PartnerSharedSecretService;
import com.ruru.tokenapi.auth.PartnerSharedSecretStore;
import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.client.PartnerClientStore;
import com.ruru.tokenapi.client.RegisterPartnerClientRequest;
import com.ruru.tokenapi.config.TokenApiProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerTokenServiceTest {
    @Test
    void issuesAndAuthenticatesGeumsangmallFrontToken() {
        PartnerSharedSecretService sharedSecretService = sharedSecretService("shared-secret");
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore(), sharedSecretService);
        clientService.register(new RegisterPartnerClientRequest(
            "geumsangmall-front",
            null,
            SystemCode.GEUMSANGMALL,
            CallSource.DMZ_FRONT,
            true,
            List.of("orders.read", "orders.write"),
            "금상몰 프론트 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, sharedSecretService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            new IssuePartnerTokenRequest("geumsangmall-front", "shared-secret")
        );

        AuthenticatedPartnerToken authenticatedToken = tokenService.authenticate(issuedToken.accessToken());
        assertNotNull(authenticatedToken);
        assertEquals("geumsangmall-front", authenticatedToken.clientId());
        assertEquals(SystemCode.GEUMSANGMALL, authenticatedToken.systemCode());
        assertEquals(CallSource.DMZ_FRONT, authenticatedToken.callSource());
    }

    @Test
    void rejectsInternalBackendTokenIssue() {
        PartnerSharedSecretService sharedSecretService = sharedSecretService("shared-secret");
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore(), sharedSecretService);
        clientService.register(new RegisterPartnerClientRequest(
            "statistics-backend",
            null,
            SystemCode.STATISTICS,
            CallSource.INTERNAL_BACKEND,
            true,
            List.of("stats.read"),
            "통계시스템 백엔드 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, sharedSecretService, tokenStore, jwtService, properties);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.issueToken(new IssuePartnerTokenRequest("statistics-backend", "shared-secret"))
        );
        assertEquals("Client uses secret key authentication", exception.getMessage());
    }

    @Test
    void rejectsMissingSystemCode() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore(), sharedSecretService("shared-secret"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> clientService.register(new RegisterPartnerClientRequest(
                "intranet-batch",
                null,
                null,
                CallSource.DMZ_FRONT,
                true,
                List.of("batch.read"),
                null
            ))
        );
        assertEquals("systemCode is required", exception.getMessage());
    }

    @Test
    void rejectsDisallowedSystemCodeAndCallSourceCombination() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore(), sharedSecretService("shared-secret"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> clientService.register(new RegisterPartnerClientRequest(
                "sign-counsel-front",
                null,
                SystemCode.SIGN_COUNSEL,
                CallSource.DMZ_FRONT,
                true,
                List.of("api.read"),
                null
            ))
        );
        assertEquals("systemCode and callSource combination is not allowed", exception.getMessage());
    }

    @Test
    void generatesSharedSecretAndClientIdWhenMissing() {
        PartnerSharedSecretService sharedSecretService = sharedSecretService(null);
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore(), sharedSecretService);

        PartnerClient client = clientService.register(new RegisterPartnerClientRequest(
            null,
            null,
            SystemCode.STATISTICS,
            CallSource.INTERNAL_BACKEND,
            true,
            List.of("api.read"),
            "통계시스템 백엔드 호출용"
        ));

        assertNotNull(client.clientId());
        assertTrue(client.clientId().startsWith("statistics-internal-backend-"));
        assertNotNull(client.clientSecret());
        assertFalse(client.clientSecret().isBlank());
    }

    @Test
    void rejectsInvalidClientCredentials() {
        PartnerSharedSecretService sharedSecretService = sharedSecretService("shared-secret");
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore(), sharedSecretService);
        clientService.register(new RegisterPartnerClientRequest(
            "sign-counsel-backend",
            null,
            SystemCode.SIGN_COUNSEL,
            CallSource.INTERNAL_BACKEND,
            true,
            List.of("api.read"),
            "수어상담 백엔드 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, sharedSecretService, tokenStore, jwtService, properties);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tokenService.issueToken(new IssuePartnerTokenRequest("sign-counsel-backend", "wrong-secret"))
        );
        assertEquals("Invalid client credentials", exception.getMessage());
    }

    @Test
    void rejectsUnknownClaimValues() {
        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        String nonPartnerToken = io.jsonwebtoken.Jwts.builder()
            .setSubject("intranet-batch")
            .setIssuer("token-api-server")
            .setId("token-1")
            .setExpiration(java.util.Date.from(java.time.Instant.now().plusSeconds(60)))
            .claim("systemCode", "UNKNOWN")
            .claim("callSource", "EXTERNAL_USER")
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .compact();

        PartnerSharedSecretService sharedSecretService = sharedSecretService("shared-secret");
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore(), sharedSecretService);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, sharedSecretService, tokenStore, jwtService, properties);

        assertNull(tokenService.authenticate(nonPartnerToken));
    }

    @Test
    void revokesActiveToken() {
        PartnerSharedSecretService sharedSecretService = sharedSecretService("shared-secret");
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore(), sharedSecretService);
        clientService.register(new RegisterPartnerClientRequest(
            "geumsangmall-front",
            null,
            SystemCode.GEUMSANGMALL,
            CallSource.DMZ_FRONT,
            true,
            List.of("api.read"),
            "금상몰 프론트 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, sharedSecretService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            new IssuePartnerTokenRequest("geumsangmall-front", "shared-secret")
        );

        RevokePartnerTokenResponse revoked = tokenService.revoke(issuedToken.accessToken());

        assertEquals("token revoked", revoked.message());
        assertEquals("geumsangmall-front", revoked.clientId());
        assertNotNull(revoked.revokedAt());
        assertNull(tokenService.authenticate(issuedToken.accessToken()));
        assertFalse(tokenService.getRevocationHistory(10).isEmpty());
    }

    @Test
    void returnsActiveTokensByClientId() {
        PartnerSharedSecretService sharedSecretService = sharedSecretService("shared-secret");
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore(), sharedSecretService);
        clientService.register(new RegisterPartnerClientRequest(
            "geumsangmall-front",
            null,
            SystemCode.GEUMSANGMALL,
            CallSource.DMZ_FRONT,
            true,
            List.of("api.read"),
            "금상몰 프론트 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, sharedSecretService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            new IssuePartnerTokenRequest("geumsangmall-front", "shared-secret")
        );

        List<ActivePartnerTokenWithId> activeTokens = tokenService.getActiveTokens("geumsangmall-front");

        assertEquals(1, activeTokens.size());
        assertEquals("geumsangmall-front", activeTokens.get(0).token().clientId());
        assertNotNull(tokenService.authenticate(issuedToken.accessToken()));
    }

    @Test
    void deletesActiveTokensByClientId() {
        PartnerSharedSecretService sharedSecretService = sharedSecretService("shared-secret");
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore(), sharedSecretService);
        clientService.register(new RegisterPartnerClientRequest(
            "geumsangmall-front",
            null,
            SystemCode.GEUMSANGMALL,
            CallSource.DMZ_FRONT,
            true,
            List.of("api.read"),
            "금상몰 프론트 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, sharedSecretService, tokenStore, jwtService, properties);

        tokenService.issueToken(new IssuePartnerTokenRequest("geumsangmall-front", "shared-secret"));

        tokenService.deleteTokensByClientId("geumsangmall-front");

        assertTrue(tokenService.getActiveTokens("geumsangmall-front").isEmpty());
    }

    private TokenApiProperties properties() {
        TokenApiProperties properties = new TokenApiProperties();
        properties.setAdminSecret("admin-secret");
        properties.setIssuer("token-api-server");
        properties.setAccessTokenTtlSeconds(1800);
        properties.setJwtSecret("change-me-jwt-secret-change-me-jwt-secret");
        return properties;
    }

    private PartnerSharedSecretService sharedSecretService(String secret) {
        return new PartnerSharedSecretService(new InMemoryPartnerSharedSecretStore(secret));
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

        @Override
        public List<PartnerClient> findAll() {
            return clients.values().stream().toList();
        }

        @Override
        public void delete(String clientId) {
            clients.remove(clientId);
        }
    }

    private static class InMemoryPartnerTokenStore implements PartnerTokenStore {
        private final Map<String, ActivePartnerToken> activeTokens = new HashMap<>();
        private final Map<String, RevokedPartnerToken> revoked = new HashMap<>();

        @Override
        public void saveActiveToken(String tokenId, ActivePartnerToken token, Duration ttl) {
            activeTokens.put(tokenId, token);
        }

        @Override
        public ActivePartnerToken findActiveToken(String tokenId) {
            return activeTokens.get(tokenId);
        }

        @Override
        public List<ActivePartnerTokenWithId> findActiveTokensByClientId(String clientId) {
            return activeTokens.entrySet().stream()
                .filter(entry -> entry.getValue().clientId().equals(clientId))
                .map(entry -> new ActivePartnerTokenWithId(entry.getKey(), entry.getValue()))
                .toList();
        }

        @Override
        public void deleteActiveToken(String tokenId) {
            activeTokens.remove(tokenId);
        }

        @Override
        public void revoke(RevokedPartnerToken token, Duration ttl) {
            revoked.put(token.tokenId(), token);
            activeTokens.remove(token.tokenId());
        }

        @Override
        public boolean isRevoked(String tokenId) {
            return revoked.containsKey(tokenId);
        }

        @Override
        public List<RevokedPartnerToken> findRevokedTokens(int limit) {
            return revoked.values().stream().limit(limit).toList();
        }
    }

    private static class InMemoryPartnerSharedSecretStore implements PartnerSharedSecretStore {
        private String secret;

        private InMemoryPartnerSharedSecretStore(String secret) {
            this.secret = secret;
        }

        @Override
        public String get() {
            return secret;
        }

        @Override
        public void save(String secret) {
            this.secret = secret;
        }
    }
}
