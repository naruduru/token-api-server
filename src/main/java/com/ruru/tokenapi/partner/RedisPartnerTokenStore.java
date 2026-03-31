package com.ruru.tokenapi.partner;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Repository
public class RedisPartnerTokenStore implements PartnerTokenStore {
    private static final String REVOKED_INDEX_KEY = "partner:revoke:ids";
    private final StringRedisTemplate redisTemplate;

    public RedisPartnerTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveActiveToken(String tokenId, ActivePartnerToken token, Duration ttl) {
        String value = token.clientId()
            + "|"
            + token.systemCode().name()
            + "|"
            + token.callSource().name()
            + "|"
            + token.issuedAt().toString()
            + "|"
            + token.expiresAt().toString();
        redisTemplate.opsForValue().set(tokenKey(tokenId), value, ttl);
    }

    @Override
    public ActivePartnerToken findActiveToken(String tokenId) {
        String value = redisTemplate.opsForValue().get(tokenKey(tokenId));
        if (value == null || value.isBlank()) {
            return null;
        }

        String[] parts = value.split("\\|", -1);
        if (parts.length < 5) {
            throw new IllegalStateException("Invalid active token payload for " + tokenId);
        }

        return new ActivePartnerToken(
            parts[0],
            SystemCode.valueOf(parts[1]),
            CallSource.valueOf(parts[2]),
            Instant.parse(parts[3]),
            Instant.parse(parts[4])
        );
    }

    @Override
    public void revoke(RevokedPartnerToken token, Duration ttl) {
        String value = token.clientId()
            + "|"
            + token.systemCode().name()
            + "|"
            + token.callSource().name()
            + "|"
            + token.revokedAt().toString()
            + "|"
            + token.expiresAt().toString();
        redisTemplate.opsForValue().set(revokeKey(token.tokenId()), value, ttl);
        redisTemplate.opsForList().leftPush(REVOKED_INDEX_KEY, token.tokenId());
        redisTemplate.delete(tokenKey(token.tokenId()));
    }

    @Override
    public boolean isRevoked(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(revokeKey(tokenId)));
    }

    @Override
    public List<RevokedPartnerToken> findRevokedTokens(int limit) {
        List<String> tokenIds = redisTemplate.opsForList().range(REVOKED_INDEX_KEY, 0, Math.max(limit - 1, 0));
        if (tokenIds == null || tokenIds.isEmpty()) {
            return List.of();
        }
        return tokenIds.stream()
            .map(this::findRevokedToken)
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    private String tokenKey(String tokenId) {
        return "partner:token:" + tokenId;
    }

    private String revokeKey(String tokenId) {
        return "partner:revoke:" + tokenId;
    }

    private RevokedPartnerToken findRevokedToken(String tokenId) {
        String value = redisTemplate.opsForValue().get(revokeKey(tokenId));
        if (value == null || value.isBlank()) {
            return null;
        }

        String[] parts = value.split("\\|", -1);
        if (parts.length < 5) {
            throw new IllegalStateException("Invalid revoked token payload for " + tokenId);
        }

        return new RevokedPartnerToken(
            tokenId,
            parts[0],
            SystemCode.valueOf(parts[1]),
            CallSource.valueOf(parts[2]),
            Instant.parse(parts[3]),
            Instant.parse(parts[4])
        );
    }
}
