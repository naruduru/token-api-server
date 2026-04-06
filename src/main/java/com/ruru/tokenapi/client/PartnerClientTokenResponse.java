package com.ruru.tokenapi.client;

import com.ruru.tokenapi.partner.ActivePartnerTokenWithId;

import java.time.Instant;

public record PartnerClientTokenResponse(
    String tokenId,
    Instant issuedAt,
    Instant expiresAt
) {
    public static PartnerClientTokenResponse from(ActivePartnerTokenWithId activeToken) {
        return new PartnerClientTokenResponse(
            activeToken.tokenId(),
            activeToken.token().issuedAt(),
            activeToken.token().expiresAt()
        );
    }
}
