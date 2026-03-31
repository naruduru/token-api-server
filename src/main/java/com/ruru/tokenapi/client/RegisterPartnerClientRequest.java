package com.ruru.tokenapi.client;

import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.SystemCode;

import java.util.List;

public record RegisterPartnerClientRequest(
    String clientId,
    String clientSecret,
    SystemCode systemCode,
    CallSource callSource,
    boolean active,
    List<String> scopes,
    String description
) {
}
