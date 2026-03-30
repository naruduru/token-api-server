package com.ruru.tokenapi.client;

import com.ruru.tokenapi.partner.PartnerChannel;

import java.util.List;

public record PartnerClient(
    String clientId,
    String clientSecret,
    boolean active,
    PartnerChannel channel,
    String systemName,
    List<String> scopes
) {
}
