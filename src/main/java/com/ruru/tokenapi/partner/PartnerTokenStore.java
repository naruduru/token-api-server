package com.ruru.tokenapi.partner;

import java.time.Duration;

public interface PartnerTokenStore {
    void saveActiveToken(PartnerChannel channel, String tokenId, String clientId, Duration ttl);
    String findActiveTokenClientId(PartnerChannel channel, String tokenId);
    boolean isRevoked(PartnerChannel channel, String tokenId);
}
