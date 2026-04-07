package com.ruru.tokenapi.geumsangmall;

import java.time.Instant;

public record GeumsangmallWssSecretResponse(
    String secret,
    long expiresIn,
    Instant expiresAt
) {
}
