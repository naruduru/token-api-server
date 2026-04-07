package com.ruru.tokenapi.geumsangmall;

import java.time.Duration;

public interface GeumsangmallWssSecretStore {
    void save(GeumsangmallWssSecret secret, Duration ttl);
    GeumsangmallWssSecret find(String secret);
    void delete(String secret);
}
