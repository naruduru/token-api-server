package com.ruru.tokenapi.auth;

import java.time.Instant;
import java.util.List;

public record AuthenticatedToken(
    String tokenId,
    String clientId,
    List<String> scopes,
    Instant issuedAt,
    Instant expiresAt
) {
}
