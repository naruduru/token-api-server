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

    public IssuedPartnerToken issueToken(IssuePartnerTokenRequest request) {
        String clientId = requireText(request.clientId(), "clientId is required");
        String clientSecret = requireText(request.clientSecret(), "clientSecret is required");
        PartnerChannel requestedChannel = request.channel();
        if (requestedChannel == null) {
            throw new IllegalArgumentException("channel is required");
        }

        PartnerClient client = partnerClientService.findActiveClient(requestedChannel, clientId);
        if (client == null || !client.clientSecret().equals(clientSecret)) {
            throw new IllegalArgumentException("Invalid client credentials");
        }

        long expiresIn = properties.getAccessTokenTtlSeconds();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expiresIn);
        String tokenId = randomId(18);
        String accessToken = partnerJwtService.issueToken(
            client.clientId(),
            tokenId,
            client.channel(),
            client.systemName(),
            client.scopes(),
            issuedAt,
            expiresAt
        );
        partnerTokenStore.saveActiveToken(client.channel(), tokenId, client.clientId(), Duration.ofSeconds(expiresIn));

        return new IssuedPartnerToken(accessToken, expiresIn);
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
        if (parsedToken.systemName() == null || parsedToken.systemName().isBlank()) {
            return null;
        }

        PartnerClient client = partnerClientService.findActiveClient(parsedToken.channel(), parsedToken.clientId());
        if (client == null) {
            return null;
        }

        String activeClientId = partnerTokenStore.findActiveTokenClientId(parsedToken.channel(), parsedToken.tokenId());
        if (activeClientId == null || !activeClientId.equals(parsedToken.clientId())) {
            return null;
        }
        if (partnerTokenStore.isRevoked(parsedToken.channel(), parsedToken.tokenId())) {
            return null;
        }

        return new AuthenticatedPartnerToken(
            parsedToken.tokenId(),
            parsedToken.clientId(),
            parsedToken.channel(),
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
