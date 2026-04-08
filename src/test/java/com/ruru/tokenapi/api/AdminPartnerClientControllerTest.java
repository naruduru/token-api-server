package com.ruru.tokenapi.api;

import com.ruru.tokenapi.client.PartnerClient;
import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.auth.PartnerClientSecretAuthService;
import com.ruru.tokenapi.partner.ActivePartnerToken;
import com.ruru.tokenapi.partner.ActivePartnerTokenWithId;
import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.PartnerTokenService;
import com.ruru.tokenapi.partner.SystemCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPartnerClientController.class)
class AdminPartnerClientControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartnerClientService partnerClientService;

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
    void registersClient() throws Exception {
        given(partnerClientService.register(any())).willReturn(new PartnerClient(
            "geumsangmall-front",
            "secret",
            SystemCode.GEUMSANGMALL,
            CallSource.DMZ_FRONT,
            true,
            List.of("api.read"),
            "금상몰 프론트 호출용"
        ));

        mockMvc.perform(post("/api/admin/partner-clients")
                .header("X-Admin-Secret", "admin-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "clientId":"geumsangmall-front",
                      "clientSecret":"secret",
                      "systemCode":"GEUMSANGMALL",
                      "callSource":"DMZ_FRONT",
                      "active":true,
                      "scopes":["api.read"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientId").value("geumsangmall-front"))
            .andExpect(jsonPath("$.clientSecret").value("secret"))
            .andExpect(jsonPath("$.systemCode").value("GEUMSANGMALL"))
            .andExpect(jsonPath("$.callSource").value("DMZ_FRONT"));
    }

    @Test
    void listsClients() throws Exception {
        given(partnerClientService.findAllClients()).willReturn(List.of(
            new PartnerClient(
                "statistics-backend",
                "secret",
                SystemCode.STATISTICS,
                CallSource.INTERNAL_BACKEND,
                true,
                List.of("api.read"),
                "통계시스템 백엔드 호출용"
            )
        ));
        given(partnerTokenService.getActiveTokens("statistics-backend")).willReturn(List.of(
            new ActivePartnerTokenWithId(
                "token-1",
                new ActivePartnerToken(
                    "statistics-backend",
                    SystemCode.STATISTICS,
                    CallSource.INTERNAL_BACKEND,
                    Instant.parse("2026-04-01T00:00:00Z"),
                    Instant.parse("2026-04-01T00:30:00Z")
                )
            )
        ));

        mockMvc.perform(get("/api/admin/partner-clients")
                .header("X-Admin-Secret", "admin-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].clientId").value("statistics-backend"))
            .andExpect(jsonPath("$[0].systemCode").value("STATISTICS"))
            .andExpect(jsonPath("$[0].callSource").value("INTERNAL_BACKEND"))
            .andExpect(jsonPath("$[0].tokens[0].tokenId").value("token-1"));
    }

    @Test
    void deletesClient() throws Exception {
        given(partnerClientService.deleteClient("statistics-backend")).willReturn(true);

        mockMvc.perform(delete("/api/admin/partner-clients/statistics-backend")
                .header("X-Admin-Secret", "admin-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("client deleted"))
            .andExpect(jsonPath("$.clientId").value("statistics-backend"));
    }

    @Test
    void rotatesClientSecret() throws Exception {
        given(partnerClientService.rotateSecret("statistics-backend")).willReturn(new PartnerClient(
            "statistics-backend",
            "rotated-secret",
            SystemCode.STATISTICS,
            CallSource.INTERNAL_BACKEND,
            true,
            List.of("api.read"),
            "통계시스템 백엔드 호출용"
        ));

        mockMvc.perform(post("/api/admin/partner-clients/statistics-backend/secret")
                .header("X-Admin-Secret", "admin-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("client secret rotated"))
            .andExpect(jsonPath("$.clientSecret").value("rotated-secret"));
    }
}
