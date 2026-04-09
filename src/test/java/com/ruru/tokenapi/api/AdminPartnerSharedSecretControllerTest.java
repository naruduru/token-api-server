package com.ruru.tokenapi.api;

import com.ruru.tokenapi.auth.PartnerClientSecretAuthService;
import com.ruru.tokenapi.auth.PartnerSharedSecretService;
import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.partner.PartnerTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPartnerSharedSecretController.class)
class AdminPartnerSharedSecretControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartnerSharedSecretService partnerSharedSecretService;

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
    void issuesSharedSecret() throws Exception {
        given(partnerSharedSecretService.getOrCreate()).willReturn("shared-secret");

        mockMvc.perform(post("/api/admin/partner-shared-secret")
                .header("X-Admin-Secret", "admin-secret"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("shared secret ready"))
            .andExpect(jsonPath("$.sharedSecret").value("shared-secret"));
    }
}
