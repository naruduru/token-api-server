package com.ruru.tokenapi.partner;

import java.time.Instant;

public record RevokedPartnerTokenResponse(
    String tokenId,
    String clientId,
    SystemCode systemCode,
    CallSource callSource,
    Instant revokedAt,
    Instant expiresAt
) {
    public static RevokedPartnerTokenResponse from(RevokedPartnerToken token) {
        return new RevokedPartnerTokenResponse(
            token.tokenId(),
            token.clientId(),
            token.systemCode(),
            token.callSource(),
            token.revokedAt(),
            token.expiresAt()
        );
    }
}
