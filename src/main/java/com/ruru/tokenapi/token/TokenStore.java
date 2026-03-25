package com.ruru.tokenapi.token;

import java.time.Duration;

public interface TokenStore {
    void save(String tokenHash, StoredToken token, Duration ttl);
    StoredToken findByHash(String tokenHash);
}
