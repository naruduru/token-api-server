package com.ruru.tokenapi.token;

import java.time.Instant;
import java.util.List;

public record IssueTokenResponse(
    String tokenType,
    String accessToken,
    String tokenId,
    String clientId,
    List<String> scopes,
    Instant issuedAt,
    Instant expiresAt
) {
}
