package com.ruru.tokenapi.partner;

import com.ruru.tokenapi.auth.AuthenticatedPartnerToken;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.client.PartnerClientStore;
import com.ruru.tokenapi.client.RegisterPartnerClientRequest;
import com.ruru.tokenapi.config.TokenApiProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerTokenServiceTest {
    @Test
    void issuesAndAuthenticatesGeumsangmallFrontToken() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "geumsangmall-front",
            "front-secret",
            SystemCode.GEUMSANGMALL,
            CallSource.DMZ_FRONT,
            true,
            java.util.List.of("orders.read", "orders.write"),
            "금상몰 프론트 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            new IssuePartnerTokenRequest("geumsangmall-front", "front-secret")
        );

        AuthenticatedPartnerToken authenticatedToken = tokenService.authenticate(issuedToken.accessToken());
        assertNotNull(authenticatedToken);
        assertEquals("geumsangmall-front", authenticatedToken.clientId());
        assertEquals(SystemCode.GEUMSANGMALL, authenticatedToken.systemCode());
        assertEquals(CallSource.DMZ_FRONT, authenticatedToken.callSource());
        assertEquals(SystemCode.GEUMSANGMALL, issuedToken.systemCode());
        assertEquals(CallSource.DMZ_FRONT, issuedToken.callSource());
    }

    @Test
    void issuesAndAuthenticatesInternalBackendToken() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "statistics-backend",
            "backend-secret",
            SystemCode.STATISTICS,
            CallSource.INTERNAL_BACKEND,
            true,
            java.util.List.of("stats.read"),
            "통계시스템 백엔드 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            new IssuePartnerTokenRequest("statistics-backend", "backend-secret")
        );

        AuthenticatedPartnerToken authenticatedToken = tokenService.authenticate(issuedToken.accessToken());
        assertNotNull(authenticatedToken);
        assertEquals(SystemCode.STATISTICS, authenticatedToken.systemCode());
        assertEquals(CallSource.INTERNAL_BACKEND, authenticatedToken.callSource());
    }

    @Test
    void rejectsMissingSystemCode() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> clientService.register(new RegisterPartnerClientRequest(
                "intranet-batch",
                "internal-secret",
                null,
                CallSource.DMZ_FRONT,
                true,
                java.util.List.of("batch.read"),
                null
            ))
        );
        assertEquals("systemCode is required", exception.getMessage());
    }

    @Test
    void rejectsDisallowedSystemCodeAndCallSourceCombination() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> clientService.register(new RegisterPartnerClientRequest(
                "sign-counsel-front",
                "front-secret",
                SystemCode.SIGN_COUNSEL,
                CallSource.DMZ_FRONT,
                true,
                java.util.List.of("api.read"),
                null
            ))
        );
        assertEquals("systemCode and callSource combination is not allowed", exception.getMessage());
    }

    @Test
    void rejectsInvalidClientCredentials() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "sign-counsel-backend",
            "internal-secret",
            SystemCode.SIGN_COUNSEL,
            CallSource.INTERNAL_BACKEND,
            true,
            java.util.List.of("api.read"),
            "수어상담 백엔드 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

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

        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        assertNull(tokenService.authenticate(nonPartnerToken));
    }

    @Test
    void revokesActiveToken() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "counsel-app-backend",
            "app-secret",
            SystemCode.COUNSEL_APP,
            CallSource.INTERNAL_BACKEND,
            true,
            java.util.List.of("api.read"),
            "상담APP 백엔드 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            new IssuePartnerTokenRequest("counsel-app-backend", "app-secret")
        );

        RevokePartnerTokenResponse revoked = tokenService.revoke(issuedToken.accessToken());

        assertEquals("token revoked", revoked.message());
        assertEquals("counsel-app-backend", revoked.clientId());
        assertNotNull(revoked.revokedAt());
        assertNull(tokenService.authenticate(issuedToken.accessToken()));
        assertFalse(tokenService.getRevocationHistory(10).isEmpty());
    }

    @Test
    void returnsActiveTokensByClientId() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "statistics-backend",
            "backend-secret",
            SystemCode.STATISTICS,
            CallSource.INTERNAL_BACKEND,
            true,
            java.util.List.of("stats.read"),
            "통계시스템 백엔드 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        IssuedPartnerToken issuedToken = tokenService.issueToken(
            new IssuePartnerTokenRequest("statistics-backend", "backend-secret")
        );

        List<ActivePartnerTokenWithId> activeTokens = tokenService.getActiveTokens("statistics-backend");

        assertEquals(1, activeTokens.size());
        assertEquals("statistics-backend", activeTokens.get(0).token().clientId());
        assertNotNull(tokenService.authenticate(issuedToken.accessToken()));
    }

    @Test
    void deletesActiveTokensByClientId() {
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "statistics-backend",
            "backend-secret",
            SystemCode.STATISTICS,
            CallSource.INTERNAL_BACKEND,
            true,
            java.util.List.of("stats.read"),
            "통계시스템 백엔드 호출용"
        ));

        InMemoryPartnerTokenStore tokenStore = new InMemoryPartnerTokenStore();
        TokenApiProperties properties = properties();
        PartnerJwtService jwtService = new PartnerJwtService(properties);
        PartnerTokenService tokenService = new PartnerTokenService(clientService, tokenStore, jwtService, properties);

        tokenService.issueToken(new IssuePartnerTokenRequest("statistics-backend", "backend-secret"));

        tokenService.deleteTokensByClientId("statistics-backend");

        assertTrue(tokenService.getActiveTokens("statistics-backend").isEmpty());
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

        @Override
        public java.util.List<PartnerClient> findAll() {
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
        public java.util.List<RevokedPartnerToken> findRevokedTokens(int limit) {
            return revoked.values().stream().limit(limit).toList();
        }
    }
}
