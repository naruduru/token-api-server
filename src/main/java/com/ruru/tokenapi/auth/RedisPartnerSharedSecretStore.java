package com.ruru.tokenapi.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisPartnerSharedSecretStore implements PartnerSharedSecretStore {
    private static final String KEY = "partner:shared-secret";
    private final StringRedisTemplate redisTemplate;

    public RedisPartnerSharedSecretStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String get() {
        return redisTemplate.opsForValue().get(KEY);
    }

    @Override
    public void save(String secret) {
        redisTemplate.opsForValue().set(KEY, secret);
    }
}
