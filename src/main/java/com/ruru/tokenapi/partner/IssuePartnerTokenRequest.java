package com.ruru.tokenapi.partner;

public record IssuePartnerTokenRequest(
    String clientId,
    String clientSecret,
    String userId
) {
}
