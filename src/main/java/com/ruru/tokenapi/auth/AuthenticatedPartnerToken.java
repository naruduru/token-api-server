package com.ruru.tokenapi.auth;

import com.ruru.tokenapi.partner.PartnerChannel;

import java.util.List;

public record AuthenticatedPartnerToken(
    String tokenId,
    String clientId,
    PartnerChannel channel,
    String systemName,
    List<String> scopes
) {
}
