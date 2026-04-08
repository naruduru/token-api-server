package com.ruru.tokenapi.api;

import com.ruru.tokenapi.geumsangmall.GeumsangmallWssSecretResponse;
import com.ruru.tokenapi.geumsangmall.GeumsangmallWssSecretService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/external/geumsangmall")
public class GeumsangmallWssSecretController {
    private final GeumsangmallWssSecretService wssSecretService;

    public GeumsangmallWssSecretController(GeumsangmallWssSecretService wssSecretService) {
        this.wssSecretService = wssSecretService;
    }

    @PostMapping("/wss-secret")
    public GeumsangmallWssSecretResponse issueWssSecret(
        @RequestHeader(name = "X-Client-Id", required = false) String clientId,
        @RequestHeader(name = "X-Client-Secret", required = false) String clientSecret
    ) {
        return wssSecretService.issue(clientId, clientSecret);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
