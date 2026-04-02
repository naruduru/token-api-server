package com.ruru.tokenapi.partner;

import java.time.Instant;

public record ActiveRefreshToken(
    String refreshToken,
    String accessTokenId,
    String clientId,
    SystemCode systemCode,
    CallSource callSource,
    Instant issuedAt,
    Instant expiresAt
) {
}
