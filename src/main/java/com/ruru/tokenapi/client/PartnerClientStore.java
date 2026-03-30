package com.ruru.tokenapi.client;

public interface PartnerClientStore {
    void save(PartnerClient client);
    PartnerClient findByClientId(String clientId);
}
