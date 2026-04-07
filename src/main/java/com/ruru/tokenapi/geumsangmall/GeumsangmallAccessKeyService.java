package com.ruru.tokenapi.geumsangmall;

import com.ruru.tokenapi.auth.AuthenticatedPartnerToken;
import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.SystemCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
public class GeumsangmallAccessKeyService {
    private final TokenApiProperties properties;

    public GeumsangmallAccessKeyService(TokenApiProperties properties) {
        this.properties = properties;
    }

    public AuthenticatedPartnerToken authenticate(String accessKey) {
        if (!matchesConfiguredAccessKey(accessKey)) {
            return null;
        }
        return new AuthenticatedPartnerToken(
            "geumsangmall-access-key",
            properties.getGeumsangmall().getClientId(),
            SystemCode.GEUMSANGMALL,
            CallSource.INTERNAL_BACKEND,
            List.of("api.read")
        );
    }

    public void validate(String accessKey) {
        if (!matchesConfiguredAccessKey(accessKey)) {
            throw new IllegalArgumentException("Invalid Geumsangmall access key");
        }
    }

    private boolean matchesConfiguredAccessKey(String accessKey) {
        String configuredAccessKey = properties.getGeumsangmall().getAccessKey();
        if (accessKey == null || accessKey.isBlank() || configuredAccessKey == null || configuredAccessKey.isBlank()) {
            return false;
        }
        byte[] actual = accessKey.trim().getBytes(StandardCharsets.UTF_8);
        byte[] expected = configuredAccessKey.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(actual, expected);
    }
}
