package com.ruru.tokenapi.geumsangmall;

import jakarta.validation.constraints.NotBlank;

public record GeumsangmallTokenExchangeRequest(
    @NotBlank String mallUserId,
    @NotBlank String mallSessionId
) {
}
