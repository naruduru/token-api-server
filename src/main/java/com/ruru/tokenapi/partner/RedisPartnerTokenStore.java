package com.ruru.tokenapi.partner;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class RedisPartnerTokenStore implements PartnerTokenStore {
    private final StringRedisTemplate redisTemplate;

    public RedisPartnerTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveActiveToken(PartnerChannel channel, String tokenId, String clientId, Duration ttl) {
        redisTemplate.opsForValue().set(tokenKey(channel, tokenId), clientId, ttl);
    }

    @Override
    public String findActiveTokenClientId(PartnerChannel channel, String tokenId) {
        return redisTemplate.opsForValue().get(tokenKey(channel, tokenId));
    }

    @Override
    public boolean isRevoked(PartnerChannel channel, String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(revokeKey(channel, tokenId)));
    }

    private String tokenKey(PartnerChannel channel, String tokenId) {
        return "partner:" + channel.name().toLowerCase() + ":token:" + tokenId;
    }

    private String revokeKey(PartnerChannel channel, String tokenId) {
        return "partner:" + channel.name().toLowerCase() + ":revoke:" + tokenId;
    }
}
