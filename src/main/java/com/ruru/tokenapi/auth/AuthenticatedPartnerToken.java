package com.ruru.tokenapi.auth;

import java.util.List;

public record AuthenticatedPartnerToken(
    String tokenId,
    String clientId,
    String systemName,
    List<String> scopes
) {
}
