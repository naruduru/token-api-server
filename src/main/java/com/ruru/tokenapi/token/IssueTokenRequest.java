package com.ruru.tokenapi.token;

import java.util.List;

public record IssueTokenRequest(
    String clientId,
    List<String> scopes,
    Long ttlSeconds
) {
}
