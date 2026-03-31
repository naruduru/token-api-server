package com.ruru.tokenapi.auth;

import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.SystemCode;

import java.util.List;

public record AuthenticatedPartnerToken(
    String tokenId,
    String clientId,
    SystemCode systemCode,
    CallSource callSource,
    List<String> scopes
) {
}
