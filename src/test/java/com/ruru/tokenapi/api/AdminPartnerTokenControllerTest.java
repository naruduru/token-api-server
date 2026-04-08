package com.ruru.tokenapi.api;

import com.ruru.tokenapi.auth.PartnerClientSecretAuthService;
import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.PartnerTokenService;
import com.ruru.tokenapi.partner.RevokePartnerTokenResponse;
import com.ruru.tokenapi.partner.RevokedPartnerTokenResponse;
import com.ruru.tokenapi.partner.SystemCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPartnerTokenController.class)
class AdminPartnerTokenControllerTest {
    @Autowired
    private MockMvc mockMvc;

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
    void revokesToken() throws Exception {
        given(partnerTokenService.revoke(anyString())).willReturn(new RevokePartnerTokenResponse(
            "token revoked",
            "token-1",
            "geumsangmall-front",
            SystemCode.GEUMSANGMALL,
            CallSource.DMZ_FRONT,
            Instant.parse("2026-04-01T00:00:00Z")
        ));

        mockMvc.perform(post("/api/admin/partner-tokens/revoke")
                .header("X-Admin-Secret", "admin-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "accessToken":"sample-token"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("token revoked"))
            .andExpect(jsonPath("$.tokenId").value("token-1"))
            .andExpect(jsonPath("$.callSource").value("DMZ_FRONT"));
    }

    @Test
    void returnsRevocationHistory() throws Exception {
        given(partnerTokenService.getRevocationHistory(anyInt())).willReturn(List.of(
            new RevokedPartnerTokenResponse(
                "token-1",
                "statistics-backend",
                SystemCode.STATISTICS,
                CallSource.INTERNAL_BACKEND,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:30:00Z")
            )
        ));

        mockMvc.perform(get("/api/admin/partner-tokens/revocations")
                .header("X-Admin-Secret", "admin-secret")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].clientId").value("statistics-backend"))
            .andExpect(jsonPath("$[0].systemCode").value("STATISTICS"));
    }
}
