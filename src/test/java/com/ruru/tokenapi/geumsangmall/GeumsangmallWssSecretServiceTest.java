package com.ruru.tokenapi.geumsangmall;

import com.ruru.tokenapi.config.TokenApiProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GeumsangmallWssSecretServiceTest {
    @Test
    void issuesAndConsumesWssSecret() {
        TokenApiProperties properties = properties();
        InMemoryWssSecretStore store = new InMemoryWssSecretStore();
        GeumsangmallWssSecretService service = new GeumsangmallWssSecretService(
            new GeumsangmallAccessKeyService(properties),
            store,
            properties
        );

        GeumsangmallWssSecretResponse response = service.issue("access-key");
        GeumsangmallWssSecret consumed = service.consume(response.secret());

        assertEquals(30, response.expiresIn());
        assertNotNull(consumed);
        assertEquals("geumsangmall-server", consumed.clientId());
        assertNull(service.consume(response.secret()));
    }

    private TokenApiProperties properties() {
        TokenApiProperties properties = new TokenApiProperties();
        properties.getGeumsangmall().setClientId("geumsangmall-server");
        properties.getGeumsangmall().setAccessKey("access-key");
        properties.getGeumsangmall().setWssSecretTtlSeconds(30);
        return properties;
    }

    private static class InMemoryWssSecretStore implements GeumsangmallWssSecretStore {
        private final Map<String, GeumsangmallWssSecret> secrets = new HashMap<>();

        @Override
        public void save(GeumsangmallWssSecret secret, Duration ttl) {
            secrets.put(secret.secret(), secret);
        }

        @Override
        public GeumsangmallWssSecret find(String secret) {
            return secrets.get(secret);
        }

        @Override
        public void delete(String secret) {
            secrets.remove(secret);
        }
    }
}
