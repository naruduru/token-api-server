package com.ruru.tokenapi.geumsangmall;

import com.ruru.tokenapi.auth.PartnerClientSecretAuthService;
import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.partner.SystemCode;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class GeumsangmallWssSecretService {
    private final PartnerClientSecretAuthService partnerClientSecretAuthService;
    private final GeumsangmallWssSecretStore secretStore;
    private final TokenApiProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public GeumsangmallWssSecretService(PartnerClientSecretAuthService partnerClientSecretAuthService,
                                        GeumsangmallWssSecretStore secretStore,
                                        TokenApiProperties properties) {
        this.partnerClientSecretAuthService = partnerClientSecretAuthService;
        this.secretStore = secretStore;
        this.properties = properties;
    }

    public GeumsangmallWssSecretResponse issue(String clientId, String clientSecret) {
        PartnerClient client = partnerClientSecretAuthService.validateClientSecret(clientId, clientSecret);
        if (client.systemCode() != SystemCode.GEUMSANGMALL) {
            throw new IllegalArgumentException("Only Geumsangmall client can issue WSS secret");
        }

        long expiresIn = Math.max(1, properties.getGeumsangmall().getWssSecretTtlSeconds());
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expiresIn);
        String secret = randomSecret();

        secretStore.save(
            new GeumsangmallWssSecret(
                secret,
                client.clientId(),
                issuedAt,
                expiresAt
            ),
            Duration.ofSeconds(expiresIn)
        );
        return new GeumsangmallWssSecretResponse(secret, expiresIn, expiresAt);
    }

    public GeumsangmallWssSecret consume(String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        GeumsangmallWssSecret storedSecret = secretStore.find(secret.trim());
        if (storedSecret == null || !storedSecret.expiresAt().isAfter(Instant.now())) {
            return null;
        }
        secretStore.delete(storedSecret.secret());
        return storedSecret;
    }

    private String randomSecret() {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
}
