package com.ruru.tokenapi.api;

import com.ruru.tokenapi.auth.AuthenticatedPartnerToken;
import com.ruru.tokenapi.auth.PartnerClientSecretAuthService;
import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.PartnerTokenService;
import com.ruru.tokenapi.partner.SystemCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartnerApiController.class)
class PartnerApiSecretControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartnerTokenService partnerTokenService;

    @MockBean
    private PartnerClientSecretAuthService partnerClientSecretAuthService;

    @MockBean
    private TokenApiProperties tokenApiProperties;

    @Test
    void acceptsInternalClientSecretForInternalApi() throws Exception {
        given(partnerClientSecretAuthService.authenticate("statistics-backend", "backend-secret")).willReturn(
            new AuthenticatedPartnerToken(
                "secret:statistics-backend",
                "statistics-backend",
                SystemCode.STATISTICS,
                CallSource.INTERNAL_BACKEND,
                List.of("api.read")
            )
        );

        mockMvc.perform(get("/api/internal/ping")
                .header("X-Client-Id", "statistics-backend")
                .header("X-Client-Secret", "backend-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientId").value("statistics-backend"))
            .andExpect(jsonPath("$.systemCode").value("STATISTICS"))
            .andExpect(jsonPath("$.callSource").value("INTERNAL_BACKEND"));
    }
}
