package com.ruru.tokenapi.partner;

import java.time.Instant;

public record RevokedPartnerToken(
    String tokenId,
    String clientId,
    SystemCode systemCode,
    CallSource callSource,
    Instant revokedAt,
    Instant expiresAt
) {
}
