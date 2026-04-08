package com.ruru.tokenapi.partner;

import com.ruru.tokenapi.auth.AuthenticatedPartnerToken;
import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.config.TokenApiProperties;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class PartnerTokenService {
    private final PartnerClientService partnerClientService;
    private final PartnerTokenStore partnerTokenStore;
    private final PartnerJwtService partnerJwtService;
    private final TokenApiProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public PartnerTokenService(PartnerClientService partnerClientService,
                               PartnerTokenStore partnerTokenStore,
                               PartnerJwtService partnerJwtService,
                               TokenApiProperties properties) {
        this.partnerClientService = partnerClientService;
        this.partnerTokenStore = partnerTokenStore;
        this.partnerJwtService = partnerJwtService;
        this.properties = properties;
    }

    public IssuedPartnerToken issueToken(IssuePartnerTokenRequest request) {
        String clientId = requireText(request.clientId(), "clientId is required");
        String clientSecret = requireText(request.clientSecret(), "clientSecret is required");

        PartnerClient client = partnerClientService.findActiveClient(clientId);
        if (client == null || !client.clientSecret().equals(clientSecret)) {
            throw new IllegalArgumentException("Invalid client credentials");
        }
        if (client.callSource() != CallSource.DMZ_FRONT) {
            throw new IllegalArgumentException("Client uses secret key authentication");
        }

        return issueTokenForClient(client, properties.getAccessTokenTtlSeconds(), properties.getRefreshTokenTtlSeconds());
    }

    public IssuedPartnerToken refreshToken(RefreshPartnerTokenRequest request) {
        String clientId = requireText(request.clientId(), "clientId is required");
        String refreshToken = requireText(request.refreshToken(), "refreshToken is required");

        ActiveRefreshToken activeRefreshToken = partnerTokenStore.findRefreshToken(refreshToken);
        if (activeRefreshToken == null || activeRefreshToken.expiresAt() == null || !activeRefreshToken.expiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        if (!activeRefreshToken.clientId().equals(clientId)) {
            throw new IllegalArgumentException("Refresh token does not match clientId");
        }

        PartnerClient client = partnerClientService.findActiveClient(clientId);
        if (client == null) {
            throw new IllegalArgumentException("Client is not active");
        }
        if (client.systemCode() != activeRefreshToken.systemCode() || client.callSource() != activeRefreshToken.callSource()) {
            throw new IllegalArgumentException("Refresh token does not match client");
        }

        partnerTokenStore.deleteRefreshToken(refreshToken);
        return issueTokenForClient(client, properties.getAccessTokenTtlSeconds(), properties.getRefreshTokenTtlSeconds());
    }

    public AuthenticatedPartnerToken authenticate(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }

        ParsedPartnerToken parsedToken = partnerJwtService.parse(accessToken);
        if (parsedToken == null) {
            return null;
        }
        if (!properties.getIssuer().equals(parsedToken.issuer())) {
            return null;
        }
        if (parsedToken.expiresAt() == null || parsedToken.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        if (parsedToken.tokenId() == null || parsedToken.tokenId().isBlank()) {
            return null;
        }
        if (parsedToken.systemCode() == null || parsedToken.callSource() == null) {
            return null;
        }

        ActivePartnerToken activeToken = partnerTokenStore.findActiveToken(parsedToken.tokenId());
        if (activeToken == null) {
            return null;
        }
        if (!activeToken.clientId().equals(parsedToken.clientId())
            || activeToken.systemCode() != parsedToken.systemCode()
            || activeToken.callSource() != parsedToken.callSource()) {
            return null;
        }
        if (partnerTokenStore.isRevoked(parsedToken.tokenId())) {
            return null;
        }

        return new AuthenticatedPartnerToken(
            parsedToken.tokenId(),
            parsedToken.clientId(),
            parsedToken.systemCode(),
            parsedToken.callSource(),
            parsedToken.scopes()
        );
    }

    public RevokePartnerTokenResponse revoke(String accessToken) {
        String normalizedToken = requireText(accessToken, "accessToken is required");
        ParsedPartnerToken parsedToken = partnerJwtService.parse(normalizedToken);
        if (parsedToken == null
            || parsedToken.tokenId() == null
            || parsedToken.tokenId().isBlank()
            || parsedToken.expiresAt() == null
            || !properties.getIssuer().equals(parsedToken.issuer())) {
            throw new IllegalArgumentException("Invalid token");
        }

        ActivePartnerToken activeToken = partnerTokenStore.findActiveToken(parsedToken.tokenId());
        if (activeToken == null) {
            throw new IllegalArgumentException("Token is not active");
        }
        if (!activeToken.clientId().equals(parsedToken.clientId())
            || activeToken.systemCode() != parsedToken.systemCode()
            || activeToken.callSource() != parsedToken.callSource()) {
            throw new IllegalArgumentException("Token is not active");
        }

        Duration ttl = Duration.between(Instant.now(), parsedToken.expiresAt());
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Token is already expired");
        }

        Instant revokedAt = Instant.now();
        partnerTokenStore.revoke(
            new RevokedPartnerToken(
                parsedToken.tokenId(),
                parsedToken.clientId(),
                parsedToken.systemCode(),
                parsedToken.callSource(),
                revokedAt,
                parsedToken.expiresAt()
            ),
            ttl
        );
        return new RevokePartnerTokenResponse(
            "token revoked",
            parsedToken.tokenId(),
            parsedToken.clientId(),
            parsedToken.systemCode(),
            parsedToken.callSource(),
            revokedAt
        );
    }

    public List<RevokedPartnerTokenResponse> getRevocationHistory(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return partnerTokenStore.findRevokedTokens(safeLimit).stream()
            .map(RevokedPartnerTokenResponse::from)
            .toList();
    }

    public List<ActivePartnerTokenWithId> getActiveTokens(String clientId) {
        String normalizedClientId = requireText(clientId, "clientId is required");
        return partnerTokenStore.findActiveTokensByClientId(normalizedClientId);
    }

    public void deleteTokensByClientId(String clientId) {
        String normalizedClientId = requireText(clientId, "clientId is required");
        partnerTokenStore.findActiveTokensByClientId(normalizedClientId).stream()
            .map(ActivePartnerTokenWithId::tokenId)
            .forEach(partnerTokenStore::deleteActiveToken);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private IssuedPartnerToken issueTokenForClient(PartnerClient client, long expiresIn, long refreshExpiresIn) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expiresIn);
        Instant refreshExpiresAt = issuedAt.plusSeconds(refreshExpiresIn);
        String tokenId = randomId(18);
        String refreshToken = randomId(32);
        String accessToken = partnerJwtService.issueToken(
            client.clientId(),
            tokenId,
            client.systemCode(),
            client.callSource(),
            client.scopes(),
            issuedAt,
            expiresAt
        );
        partnerTokenStore.saveActiveToken(
            tokenId,
            new ActivePartnerToken(
                client.clientId(),
                client.systemCode(),
                client.callSource(),
                issuedAt,
                expiresAt
            ),
            Duration.ofSeconds(expiresIn)
        );
        partnerTokenStore.saveRefreshToken(
            refreshToken,
            new ActiveRefreshToken(
                refreshToken,
                tokenId,
                client.clientId(),
                client.systemCode(),
                client.callSource(),
                issuedAt,
                refreshExpiresAt
            ),
            Duration.ofSeconds(refreshExpiresIn)
        );

        return new IssuedPartnerToken(
            accessToken,
            refreshToken,
            expiresIn,
            refreshExpiresIn,
            client.systemCode(),
            client.callSource()
        );
    }

    private String randomId(int bytes) {
        byte[] random = new byte[bytes];
        secureRandom.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
}
