package com.ruru.tokenapi.api;

import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.geumsangmall.GeumsangmallTokenExchangeService;
import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.IssuedPartnerToken;
import com.ruru.tokenapi.partner.PartnerTokenService;
import com.ruru.tokenapi.partner.SystemCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeumsangmallTokenExchangeController.class)
class GeumsangmallTokenExchangeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeumsangmallTokenExchangeService geumsangmallTokenExchangeService;

    @MockBean
    private PartnerTokenService partnerTokenService;

    @MockBean
    private TokenApiProperties tokenApiProperties;

    @BeforeEach
    void setUp() {
        when(tokenApiProperties.getAdminSecret()).thenReturn("admin-secret");
    }

    @Test
    void exchangesToken() throws Exception {
        when(geumsangmallTokenExchangeService.exchange(any(), any())).thenReturn(
            new IssuedPartnerToken("jwt-token", 300, SystemCode.GEUMSANGMALL, CallSource.DMZ_FRONT)
        );

        mockMvc.perform(post("/api/external/geumsangmall/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mallUserId":"mall-user-1",
                      "mallSessionId":"session-1"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.expiresIn").value(300))
            .andExpect(jsonPath("$.systemCode").value("GEUMSANGMALL"))
            .andExpect(jsonPath("$.callSource").value("DMZ_FRONT"));
    }
}
