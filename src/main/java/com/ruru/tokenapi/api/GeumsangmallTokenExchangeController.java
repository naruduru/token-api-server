package com.ruru.tokenapi.api;

import com.ruru.tokenapi.geumsangmall.GeumsangmallTokenExchangeRequest;
import com.ruru.tokenapi.geumsangmall.GeumsangmallTokenExchangeService;
import com.ruru.tokenapi.partner.IssuePartnerTokenResponse;
import com.ruru.tokenapi.partner.IssuedPartnerToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/external/geumsangmall")
public class GeumsangmallTokenExchangeController {
    private final GeumsangmallTokenExchangeService geumsangmallTokenExchangeService;

    public GeumsangmallTokenExchangeController(GeumsangmallTokenExchangeService geumsangmallTokenExchangeService) {
        this.geumsangmallTokenExchangeService = geumsangmallTokenExchangeService;
    }

    @PostMapping("/token")
    public IssuePartnerTokenResponse exchange(@Valid @RequestBody GeumsangmallTokenExchangeRequest request,
                                              HttpServletRequest httpRequest) {
        return toResponse(geumsangmallTokenExchangeService.exchange(request, httpRequest));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private IssuePartnerTokenResponse toResponse(IssuedPartnerToken issuedToken) {
        return new IssuePartnerTokenResponse(
            issuedToken.accessToken(),
            "Bearer",
            issuedToken.expiresIn(),
            issuedToken.systemCode(),
            issuedToken.callSource()
        );
    }
}
