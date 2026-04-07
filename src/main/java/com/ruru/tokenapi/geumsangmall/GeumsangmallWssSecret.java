package com.ruru.tokenapi.geumsangmall;

import java.time.Instant;

public record GeumsangmallWssSecret(
    String secret,
    String clientId,
    Instant issuedAt,
    Instant expiresAt
) {
}
