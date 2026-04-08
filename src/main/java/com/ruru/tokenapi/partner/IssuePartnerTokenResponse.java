package com.ruru.tokenapi.partner;

public record IssuePartnerTokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    long refreshExpiresIn,
    SystemCode systemCode,
    CallSource callSource
) {
}
