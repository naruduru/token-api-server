package com.ruru.tokenapi.geumsangmall;

public record GeumsangmallVerificationResponse(
    boolean valid,
    String mallUserId,
    String message
) {
}
