package com.ruru.tokenapi.token;

import com.ruru.tokenapi.auth.AuthenticatedToken;
import com.ruru.tokenapi.config.TokenApiProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class TokenService {
    private final TokenStore tokenStore;
    private final TokenApiProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(TokenStore tokenStore, TokenApiProperties properties) {
        this.tokenStore = tokenStore;
        this.properties = properties;
    }

    public IssuedToken issueToken(IssueTokenRequest request) {
        String clientId = normalizeClientId(request.clientId());
        long ttlSeconds = resolveTtlSeconds(request.ttlSeconds());
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);

        String tokenId = randomId(12);
        String secret = randomId(32);
        String rawToken = properties.getTokenPrefix() + "_" + tokenId + "_" + secret;
        String tokenHash = hash(rawToken);
        List<String> scopes = request.scopes() == null ? List.of() : request.scopes().stream()
            .filter(scope -> scope != null && !scope.isBlank())
            .map(String::trim)
            .distinct()
            .toList();

        StoredToken storedToken = new StoredToken(tokenId, clientId, scopes, issuedAt, expiresAt);
        tokenStore.save(tokenHash, storedToken, Duration.ofSeconds(ttlSeconds));

        return new IssuedToken(rawToken, tokenId, clientId, scopes, issuedAt, expiresAt);
    }

    public AuthenticatedToken authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        StoredToken storedToken = tokenStore.findByHash(hash(rawToken));
        if (storedToken == null) return null;
        if (storedToken.expiresAt().isBefore(Instant.now())) return null;
        return new AuthenticatedToken(
            storedToken.tokenId(),
            storedToken.clientId(),
            storedToken.scopes(),
            storedToken.issuedAt(),
            storedToken.expiresAt()
        );
    }

    private String normalizeClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required");
        }
        return clientId.trim();
    }

    private long resolveTtlSeconds(Long requestedTtlSeconds) {
        long fallback = properties.getDefaultTtlSeconds();
        long value = requestedTtlSeconds == null ? fallback : requestedTtlSeconds;
        if (value <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be positive");
        }
        return value;
    }

    private String randomId(int bytes) {
        byte[] random = new byte[bytes];
        secureRandom.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
