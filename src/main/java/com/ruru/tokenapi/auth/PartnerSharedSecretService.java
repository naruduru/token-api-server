package com.ruru.tokenapi.auth;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PartnerSharedSecretService {
    private final PartnerSharedSecretStore partnerSharedSecretStore;
    private final SecureRandom secureRandom = new SecureRandom();

    public PartnerSharedSecretService(PartnerSharedSecretStore partnerSharedSecretStore) {
        this.partnerSharedSecretStore = partnerSharedSecretStore;
    }

    public String getOrCreate() {
        String existingSecret = partnerSharedSecretStore.get();
        if (existingSecret != null && !existingSecret.isBlank()) {
            return existingSecret;
        }
        String generatedSecret = generateSecret();
        partnerSharedSecretStore.save(generatedSecret);
        return generatedSecret;
    }

    public boolean matches(String providedSecret) {
        String storedSecret = partnerSharedSecretStore.get();
        if (storedSecret == null || storedSecret.isBlank() || providedSecret == null || providedSecret.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
            storedSecret.trim().getBytes(StandardCharsets.UTF_8),
            providedSecret.trim().getBytes(StandardCharsets.UTF_8)
        );
    }

    private String generateSecret() {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
}
