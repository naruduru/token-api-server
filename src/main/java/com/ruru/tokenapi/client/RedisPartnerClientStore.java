package com.ruru.tokenapi.client;

import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.SystemCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class RedisPartnerClientStore implements PartnerClientStore {
    private static final String KEY_PREFIX = "partner:client:";
    private static final String IDS_KEY = "partner:client:ids";

    private final StringRedisTemplate redisTemplate;

    public RedisPartnerClientStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(PartnerClient client) {
        String value = client.clientSecret()
            + "|"
            + client.systemCode().name()
            + "|"
            + client.callSource().name()
            + "|"
            + (client.active() ? "Y" : "N")
            + "|"
            + String.join(",", client.scopes())
            + "|"
            + nullToEmpty(client.description());
        redisTemplate.opsForValue().set(key(client.clientId()), value);
        redisTemplate.opsForSet().add(IDS_KEY, client.clientId());
    }

    @Override
    public PartnerClient findByClientId(String clientId) {
        String value = redisTemplate.opsForValue().get(key(clientId));
        if (value == null || value.isBlank()) {
            return null;
        }

        String[] parts = value.split("\\|", -1);
        if (parts.length < 6) {
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
            SystemCode.valueOf(parts[1]),
            CallSource.valueOf(parts[2]),
            "Y".equalsIgnoreCase(parts[3]),
            scopes,
            parts[5].isBlank() ? null : parts[5]
        );
    }

    @Override
    public List<PartnerClient> findAll() {
        var ids = redisTemplate.opsForSet().members(IDS_KEY);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
            .map(this::findByClientId)
            .filter(java.util.Objects::nonNull)
            .sorted(java.util.Comparator.comparing(PartnerClient::clientId))
            .toList();
    }

    @Override
    public void delete(String clientId) {
        redisTemplate.delete(key(clientId));
        redisTemplate.opsForSet().remove(IDS_KEY, clientId);
    }

    private String key(String clientId) {
        return KEY_PREFIX + clientId;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
