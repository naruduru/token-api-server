package com.ruru.tokenapi.client;

import java.util.List;

public interface PartnerClientStore {
    void save(PartnerClient client);
    PartnerClient findByClientId(String clientId);
    List<PartnerClient> findAll();
    void delete(String clientId);
}
