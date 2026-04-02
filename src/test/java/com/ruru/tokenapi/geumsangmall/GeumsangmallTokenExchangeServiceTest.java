package com.ruru.tokenapi.geumsangmall;

import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.client.PartnerClientStore;
import com.ruru.tokenapi.client.RegisterPartnerClientRequest;
import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.partner.ActivePartnerToken;
import com.ruru.tokenapi.partner.ActiveRefreshToken;
import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.IssuedPartnerToken;
import com.ruru.tokenapi.partner.PartnerJwtService;
import com.ruru.tokenapi.partner.PartnerTokenService;
import com.ruru.tokenapi.partner.PartnerTokenStore;
import com.ruru.tokenapi.partner.RevokedPartnerToken;
import com.ruru.tokenapi.partner.SystemCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeumsangmallTokenExchangeServiceTest {
    @Test
    void exchangesTokenAfterVerification() {
        TokenApiProperties properties = properties();
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "geumsangmall-front",
            "front-secret",
            SystemCode.GEUMSANGMALL,
            CallSource.DMZ_FRONT,
            true,
            List.of("api.read"),
            "금상몰 프론트 호출용"
        ));
        PartnerTokenService tokenService = new PartnerTokenService(
            clientService,
            new InMemoryPartnerTokenStore(),
            new PartnerJwtService(properties),
            properties
        );

        GeumsangmallTokenExchangeService exchangeService = new GeumsangmallTokenExchangeService(
            request -> new GeumsangmallVerificationResponse(true, request.mallUserId(), "ok"),
            tokenService
        );

        HttpServletRequest httpRequest = new MockHttpServletRequest();
        IssuedPartnerToken token = exchangeService.exchange(
            new GeumsangmallTokenExchangeRequest("mall-user-1", "session-1"),
            httpRequest
        );

        assertEquals(SystemCode.GEUMSANGMALL, token.systemCode());
        assertEquals(CallSource.DMZ_FRONT, token.callSource());
        assertEquals(300, token.expiresIn());
    }

    @Test
    void rejectsWhenVerificationFails() {
        TokenApiProperties properties = properties();
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "geumsangmall-front",
            "front-secret",
            SystemCode.GEUMSANGMALL,
            CallSource.DMZ_FRONT,
            true,
            List.of("api.read"),
            "금상몰 프론트 호출용"
        ));
        PartnerTokenService tokenService = new PartnerTokenService(
            clientService,
            new InMemoryPartnerTokenStore(),
            new PartnerJwtService(properties),
            properties
        );

        GeumsangmallTokenExchangeService exchangeService = new GeumsangmallTokenExchangeService(
            request -> new GeumsangmallVerificationResponse(false, request.mallUserId(), "invalid session"),
            tokenService
        );

        assertThrows(IllegalArgumentException.class, () -> exchangeService.exchange(
            new GeumsangmallTokenExchangeRequest("mall-user-1", "session-1"),
            new MockHttpServletRequest()
        ));
    }

    private TokenApiProperties properties() {
        TokenApiProperties properties = new TokenApiProperties();
        properties.setAdminSecret("admin-secret");
        properties.setIssuer("token-api-server");
        properties.setAccessTokenTtlSeconds(1800);
        properties.setRefreshTokenTtlSeconds(1209600);
        properties.setJwtSecret("change-me-jwt-secret-change-me-jwt-secret");
        properties.getGeumsangmall().setExchangeEnabled(true);
        properties.getGeumsangmall().setExchangeClientId("geumsangmall-front");
        properties.getGeumsangmall().setExchangeTokenTtlSeconds(300);
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
        public List<PartnerClient> findAll() {
            return clients.values().stream().toList();
        }
    }

    private static class InMemoryPartnerTokenStore implements PartnerTokenStore {
        private final Map<String, ActivePartnerToken> activeTokens = new HashMap<>();
        private final Map<String, ActiveRefreshToken> refreshTokens = new HashMap<>();
        private final Map<String, String> refreshByAccessTokenId = new HashMap<>();

        @Override
        public void saveActiveToken(String tokenId, ActivePartnerToken token, Duration ttl) {
            activeTokens.put(tokenId, token);
        }

        @Override
        public ActivePartnerToken findActiveToken(String tokenId) {
            return activeTokens.get(tokenId);
        }

        @Override
        public void saveRefreshToken(String refreshToken, ActiveRefreshToken token, Duration ttl) {
            refreshTokens.put(refreshToken, token);
            refreshByAccessTokenId.put(token.accessTokenId(), refreshToken);
        }

        @Override
        public ActiveRefreshToken findRefreshToken(String refreshToken) {
            return refreshTokens.get(refreshToken);
        }

        @Override
        public ActiveRefreshToken findRefreshTokenByAccessTokenId(String accessTokenId) {
            String refreshToken = refreshByAccessTokenId.get(accessTokenId);
            return refreshToken == null ? null : refreshTokens.get(refreshToken);
        }

        @Override
        public void deleteRefreshToken(String refreshToken) {
            ActiveRefreshToken token = refreshTokens.remove(refreshToken);
            if (token != null) {
                refreshByAccessTokenId.remove(token.accessTokenId());
            }
        }

        @Override
        public void revoke(RevokedPartnerToken token, Duration ttl) {
            activeTokens.remove(token.tokenId());
            ActiveRefreshToken refreshToken = findRefreshTokenByAccessTokenId(token.tokenId());
            if (refreshToken != null) {
                deleteRefreshToken(refreshToken.refreshToken());
            }
        }

        @Override
        public boolean isRevoked(String tokenId) {
            return false;
        }

        @Override
        public List<RevokedPartnerToken> findRevokedTokens(int limit) {
            return List.of();
        }
    }
}
