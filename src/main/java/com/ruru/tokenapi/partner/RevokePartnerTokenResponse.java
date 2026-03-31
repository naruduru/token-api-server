package com.ruru.tokenapi.partner;

public record RevokePartnerTokenResponse(
    String message,
    String tokenId,
    String clientId,
    SystemCode systemCode,
    CallSource callSource,
    java.time.Instant revokedAt
) {
}
