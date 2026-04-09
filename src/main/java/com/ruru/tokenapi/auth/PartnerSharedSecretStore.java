package com.ruru.tokenapi.auth;

public interface PartnerSharedSecretStore {
    String get();
    void save(String secret);
}
