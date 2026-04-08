package com.ruru.tokenapi.geumsangmall;

import com.ruru.tokenapi.auth.PartnerClientSecretAuthService;
import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.client.PartnerClientStore;
import com.ruru.tokenapi.client.RegisterPartnerClientRequest;
import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.SystemCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GeumsangmallWssSecretServiceTest {
    @Test
    void issuesAndConsumesWssSecret() {
        TokenApiProperties properties = properties();
        InMemoryWssSecretStore store = new InMemoryWssSecretStore();
        PartnerClientService clientService = new PartnerClientService(new InMemoryPartnerClientStore());
        clientService.register(new RegisterPartnerClientRequest(
            "geumsangmall-front",
            "front-secret",
            SystemCode.GEUMSANGMALL,
            CallSource.DMZ_FRONT,
            true,
            List.of("api.read"),
            "금상몰 프론트 호출용"
        ));
        GeumsangmallWssSecretService service = new GeumsangmallWssSecretService(
            new PartnerClientSecretAuthService(clientService),
            store,
            properties
        );

        GeumsangmallWssSecretResponse response = service.issue("geumsangmall-front", "front-secret");
        GeumsangmallWssSecret consumed = service.consume(response.secret());

        assertEquals(30, response.expiresIn());
        assertNotNull(consumed);
        assertEquals("geumsangmall-front", consumed.clientId());
        assertNull(service.consume(response.secret()));
    }

    private TokenApiProperties properties() {
        TokenApiProperties properties = new TokenApiProperties();
        properties.getGeumsangmall().setClientId("geumsangmall-front");
        properties.getGeumsangmall().setWssSecretTtlSeconds(30);
        return properties;
    }

    private static class InMemoryPartnerClientStore implements PartnerClientStore {
        private final Map<String, PartnerClient> clients = new HashMap<>();

        @Override
        public void save(PartnerClient client) {
            clients.put(client.clientId(), client);
        }

        @Override
        public PartnerClient findByClientId(String clientId) {
            return clients.get(clientId);
        }

        @Override
        public List<PartnerClient> findAll() {
            return clients.values().stream().toList();
        }

        @Override
        public void delete(String clientId) {
            clients.remove(clientId);
        }
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
