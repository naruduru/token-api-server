package com.ruru.tokenapi.geumsangmall;

import com.ruru.tokenapi.config.TokenApiProperties;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class GeumsangmallWssSecretService {
    private final GeumsangmallAccessKeyService accessKeyService;
    private final GeumsangmallWssSecretStore secretStore;
    private final TokenApiProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public GeumsangmallWssSecretService(GeumsangmallAccessKeyService accessKeyService,
                                        GeumsangmallWssSecretStore secretStore,
                                        TokenApiProperties properties) {
        this.accessKeyService = accessKeyService;
        this.secretStore = secretStore;
        this.properties = properties;
    }

    public GeumsangmallWssSecretResponse issue(String accessKey) {
        accessKeyService.validate(accessKey);

        long expiresIn = Math.max(1, properties.getGeumsangmall().getWssSecretTtlSeconds());
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expiresIn);
        String secret = randomSecret();

        secretStore.save(
            new GeumsangmallWssSecret(
                secret,
                properties.getGeumsangmall().getClientId(),
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
        if (storedSecret == null || storedSecret.expiresAt().isBefore(Instant.now())) {
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
