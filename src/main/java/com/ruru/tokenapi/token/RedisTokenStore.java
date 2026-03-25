package com.ruru.tokenapi.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class RedisTokenStore implements TokenStore {
    private static final String KEY_PREFIX = "api:token:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTokenStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String tokenHash, StoredToken token, Duration ttl) {
        try {
            String payload = objectMapper.writeValueAsString(token);
            redisTemplate.opsForValue().set(key(tokenHash), payload, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize token", e);
        }
    }

    @Override
    public StoredToken findByHash(String tokenHash) {
        String payload = redisTemplate.opsForValue().get(key(tokenHash));
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, StoredToken.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize token", e);
        }
    }

    private String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }
}
