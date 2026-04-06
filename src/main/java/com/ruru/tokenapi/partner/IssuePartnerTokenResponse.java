package com.ruru.tokenapi.partner;

public record IssuePartnerTokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    SystemCode systemCode,
    CallSource callSource
) {
}
