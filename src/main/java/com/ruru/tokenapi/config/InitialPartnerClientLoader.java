package com.ruru.tokenapi.config;

import com.ruru.tokenapi.client.PartnerClientService;
import com.ruru.tokenapi.client.RegisterPartnerClientRequest;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitialPartnerClientLoader implements ApplicationRunner {
    private final TokenApiProperties properties;
    private final PartnerClientService partnerClientService;

    public InitialPartnerClientLoader(TokenApiProperties properties, PartnerClientService partnerClientService) {
        this.properties = properties;
        this.partnerClientService = partnerClientService;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (TokenApiProperties.InitialClient initialClient : properties.getInitialClients()) {
            if (partnerClientService.findClient(initialClient.getClientId()) != null) {
                continue;
            }
            partnerClientService.register(new RegisterPartnerClientRequest(
                initialClient.getClientId(),
                initialClient.getClientSecret(),
                initialClient.getSystemCode(),
                initialClient.getCallSource(),
                initialClient.isActive(),
                initialClient.getScopes(),
                initialClient.getDescription()
            ));
        }
    }
}
