package com.ruru.tokenapi.partner;

public record IssuedPartnerToken(
    String accessToken,
    long expiresIn,
    SystemCode systemCode,
    CallSource callSource
) {
}
