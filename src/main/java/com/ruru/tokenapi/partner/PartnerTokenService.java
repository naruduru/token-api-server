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

    public IssuedPartnerToken issueToken(PartnerChannel channel, IssuePartnerTokenRequest request) {
        String clientId = requireText(request.clientId(), "clientId is required");
        String clientSecret = requireText(request.clientSecret(), "clientSecret is required");

        PartnerClient client = partnerClientService.findActiveClient(channel, clientId);
        if (client == null || !client.clientSecret().equals(clientSecret)) {
            throw new IllegalArgumentException("Invalid client credentials");
        }

        String userId = channel == PartnerChannel.EXTERNAL_USER
            ? requireText(request.userId(), "userId is required for external user tokens")
            : null;
        if (channel == PartnerChannel.INTERNAL_SYSTEM && request.userId() != null && !request.userId().isBlank()) {
            throw new IllegalArgumentException("userId is not allowed for internal system tokens");
        }

        long expiresIn = properties.getAccessTokenTtlSeconds();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expiresIn);
        String tokenId = randomId(18);
        String accessToken = partnerJwtService.issueToken(
            channel,
            client.clientId(),
            tokenId,
            userId,
            client.systemName(),
            client.scopes(),
            issuedAt,
            expiresAt
        );
        partnerTokenStore.saveActiveToken(channel, tokenId, client.clientId(), Duration.ofSeconds(expiresIn));

        return new IssuedPartnerToken(accessToken, expiresIn);
    }

    public AuthenticatedPartnerToken authenticate(PartnerChannel expectedChannel, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }

        ParsedPartnerToken parsedToken = partnerJwtService.parse(accessToken);
        if (parsedToken == null || parsedToken.channel() != expectedChannel) {
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
        if (expectedChannel == PartnerChannel.EXTERNAL_USER
            && (parsedToken.userId() == null || parsedToken.userId().isBlank())) {
            return null;
        }
        if (expectedChannel == PartnerChannel.INTERNAL_SYSTEM
            && (parsedToken.systemName() == null || parsedToken.systemName().isBlank())) {
            return null;
        }

        String activeClientId = partnerTokenStore.findActiveTokenClientId(expectedChannel, parsedToken.tokenId());
        if (activeClientId == null || !activeClientId.equals(parsedToken.clientId())) {
            return null;
        }
        if (partnerTokenStore.isRevoked(expectedChannel, parsedToken.tokenId())) {
            return null;
        }

        return new AuthenticatedPartnerToken(
            parsedToken.tokenId(),
            parsedToken.clientId(),
            parsedToken.channel(),
            parsedToken.userId(),
            parsedToken.systemName(),
            parsedToken.scopes()
        );
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String randomId(int bytes) {
        byte[] random = new byte[bytes];
        secureRandom.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
}
