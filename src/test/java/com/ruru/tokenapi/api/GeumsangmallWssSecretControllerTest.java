package com.ruru.tokenapi.api;

import com.ruru.tokenapi.auth.PartnerClientSecretAuthService;
import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.geumsangmall.GeumsangmallWssSecretResponse;
import com.ruru.tokenapi.geumsangmall.GeumsangmallWssSecretService;
import com.ruru.tokenapi.partner.PartnerTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeumsangmallWssSecretController.class)
class GeumsangmallWssSecretControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeumsangmallWssSecretService wssSecretService;

    @MockBean
    private PartnerTokenService partnerTokenService;

    @MockBean
    private PartnerClientSecretAuthService partnerClientSecretAuthService;

    @MockBean
    private TokenApiProperties tokenApiProperties;

    @BeforeEach
    void setUp() {
        when(tokenApiProperties.getAdminSecret()).thenReturn("admin-secret");
    }

    @Test
    void issuesWssSecret() throws Exception {
        given(wssSecretService.issue(anyString(), anyString())).willReturn(new GeumsangmallWssSecretResponse(
            "temporary-secret",
            30,
            Instant.parse("2026-04-08T00:00:30Z")
        ));

        mockMvc.perform(post("/api/external/geumsangmall/wss-secret")
                .header("X-Client-Id", "geumsangmall-front")
                .header("X-Client-Secret", "front-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.secret").value("temporary-secret"))
            .andExpect(jsonPath("$.expiresIn").value(30));
    }
}
