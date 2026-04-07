package com.ruru.tokenapi.geumsangmall;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;

@Repository
public class RedisGeumsangmallWssSecretStore implements GeumsangmallWssSecretStore {
    private static final String KEY_PREFIX = "geumsangmall:wss-secret:";
    private final StringRedisTemplate redisTemplate;

    public RedisGeumsangmallWssSecretStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(GeumsangmallWssSecret secret, Duration ttl) {
        String value = secret.clientId()
            + "|"
            + secret.issuedAt()
            + "|"
            + secret.expiresAt();
        redisTemplate.opsForValue().set(key(secret.secret()), value, ttl);
    }

    @Override
    public GeumsangmallWssSecret find(String secret) {
        String value = redisTemplate.opsForValue().get(key(secret));
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split("\\|", -1);
        if (parts.length < 3) {
            throw new IllegalStateException("Invalid Geumsangmall WSS secret payload");
        }
        return new GeumsangmallWssSecret(
            secret,
            parts[0],
            Instant.parse(parts[1]),
            Instant.parse(parts[2])
        );
    }

    @Override
    public void delete(String secret) {
        redisTemplate.delete(key(secret));
    }

    private String key(String secret) {
        return KEY_PREFIX + secret;
    }
}
