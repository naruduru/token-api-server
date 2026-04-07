package com.ruru.tokenapi.api;

import com.ruru.tokenapi.auth.AuthenticatedPartnerToken;
import com.ruru.tokenapi.config.TokenApiProperties;
import com.ruru.tokenapi.geumsangmall.GeumsangmallAccessKeyService;
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
class PartnerApiAccessKeyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartnerTokenService partnerTokenService;

    @MockBean
    private GeumsangmallAccessKeyService geumsangmallAccessKeyService;

    @MockBean
    private TokenApiProperties tokenApiProperties;

    @Test
    void acceptsGeumsangmallAccessKeyForInternalApi() throws Exception {
        given(geumsangmallAccessKeyService.authenticate("access-key")).willReturn(new AuthenticatedPartnerToken(
            "geumsangmall-access-key",
            "geumsangmall-server",
            SystemCode.GEUMSANGMALL,
            CallSource.INTERNAL_BACKEND,
            List.of("api.read")
        ));

        mockMvc.perform(get("/api/internal/ping")
                .header("X-Geumsangmall-Access-Key", "access-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientId").value("geumsangmall-server"))
            .andExpect(jsonPath("$.systemCode").value("GEUMSANGMALL"))
            .andExpect(jsonPath("$.callSource").value("INTERNAL_BACKEND"));
    }
}
