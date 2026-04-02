package com.ruru.tokenapi.partner;

public record RefreshPartnerTokenRequest(
    String clientId,
    String refreshToken
) {
}
