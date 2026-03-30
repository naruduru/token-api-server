package com.ruru.tokenapi.client;

import com.ruru.tokenapi.partner.PartnerChannel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class RedisPartnerClientStore implements PartnerClientStore {
    private static final String KEY_PREFIX = "partner:client:";

    private final StringRedisTemplate redisTemplate;

    public RedisPartnerClientStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(PartnerClient client) {
        String value = client.clientSecret()
            + "|"
            + (client.active() ? "Y" : "N")
            + "|"
            + client.channel().name()
            + "|"
            + nullToEmpty(client.systemName())
            + "|"
            + String.join(",", client.scopes());
        redisTemplate.opsForValue().set(key(client.clientId()), value);
    }

    @Override
    public PartnerClient findByClientId(String clientId) {
        String value = redisTemplate.opsForValue().get(key(clientId));
        if (value == null || value.isBlank()) {
            return null;
        }

        String[] parts = value.split("\\|", -1);
        if (parts.length < 5) {
            throw new IllegalStateException("Invalid partner client payload for " + clientId);
        }

        List<String> scopes = parts[4].isBlank()
            ? List.of()
            : Arrays.stream(parts[4].split(","))
                .filter(scope -> !scope.isBlank())
                .map(String::trim)
                .toList();
        return new PartnerClient(
            clientId,
            parts[0],
            "Y".equalsIgnoreCase(parts[1]),
            PartnerChannel.valueOf(parts[2]),
            parts[3].isBlank() ? null : parts[3],
            scopes
        );
    }

    private String key(String clientId) {
        return KEY_PREFIX + clientId;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
