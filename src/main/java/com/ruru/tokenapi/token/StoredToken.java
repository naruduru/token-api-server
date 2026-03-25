package com.ruru.tokenapi.token;

import java.time.Instant;
import java.util.List;

public record StoredToken(
    String tokenId,
    String clientId,
    List<String> scopes,
    Instant issuedAt,
    Instant expiresAt
) {
}
