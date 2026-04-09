package com.ruru.tokenapi.config;

import com.ruru.tokenapi.partner.CallSource;
import com.ruru.tokenapi.partner.SystemCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "token.api")
public class TokenApiProperties {
    private String adminSecret;
    private long accessTokenTtlSeconds = 1800;
    private String issuer = "token-api-server";
    private String jwtSecret = "change-me-jwt-secret-change-me-jwt-secret";
    private List<InitialClient> initialClients = new ArrayList<>();

    public String getAdminSecret() {
        return adminSecret;
    }

    public void setAdminSecret(String adminSecret) {
        this.adminSecret = adminSecret;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public List<InitialClient> getInitialClients() {
        return initialClients;
    }

    public void setInitialClients(List<InitialClient> initialClients) {
        this.initialClients = initialClients;
    }

    public static class InitialClient {
        private String clientId;
        private String clientSecret;
        private SystemCode systemCode;
        private CallSource callSource;
        private boolean active = true;
        private List<String> scopes = new ArrayList<>();
        private String description;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public SystemCode getSystemCode() {
            return systemCode;
        }

        public void setSystemCode(SystemCode systemCode) {
            this.systemCode = systemCode;
        }

        public CallSource getCallSource() {
            return callSource;
        }

        public void setCallSource(CallSource callSource) {
            this.callSource = callSource;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

}
