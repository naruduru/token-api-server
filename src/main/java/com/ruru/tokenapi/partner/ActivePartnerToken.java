package com.ruru.tokenapi.partner;

import java.time.Instant;

public record ActivePartnerToken(
    String clientId,
    SystemCode systemCode,
    CallSource callSource,
    Instant issuedAt,
    Instant expiresAt
) {
}
