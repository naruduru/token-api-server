package com.ruru.tokenapi.geumsangmall;

public record GeumsangmallVerificationRequest(
    String mallUserId,
    String mallSessionId,
    String clientIp,
    String userAgent
) {
}
