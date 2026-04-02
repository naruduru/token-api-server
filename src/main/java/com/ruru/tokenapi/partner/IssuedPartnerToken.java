package com.ruru.tokenapi.partner;

public record IssuedPartnerToken(
    String accessToken,
    String refreshToken,
    long expiresIn,
    long refreshExpiresIn,
    SystemCode systemCode,
    CallSource callSource
) {
}
