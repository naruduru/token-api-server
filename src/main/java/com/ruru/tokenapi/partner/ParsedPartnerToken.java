package com.ruru.tokenapi.partner;

import java.time.Instant;
import java.util.List;

public record ParsedPartnerToken(
    String tokenId,
    String clientId,
    String issuer,
    String systemName,
    List<String> scopes,
    Instant expiresAt
) {
}
