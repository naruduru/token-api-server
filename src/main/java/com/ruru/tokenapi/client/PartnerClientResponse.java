package com.ruru.tokenapi.client;

import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.SystemCode;

import java.util.List;

public record PartnerClientResponse(
    String clientId,
    SystemCode systemCode,
    CallSource callSource,
    boolean active,
    List<String> scopes,
    String description,
    List<PartnerClientTokenResponse> tokens
) {
    public static PartnerClientResponse from(PartnerClient client, List<PartnerClientTokenResponse> tokens) {
        return new PartnerClientResponse(
            client.clientId(),
            client.systemCode(),
            client.callSource(),
            client.active(),
            client.scopes(),
            client.description(),
            tokens
        );
    }
}
