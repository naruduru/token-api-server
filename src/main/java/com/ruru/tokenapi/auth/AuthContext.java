package com.ruru.tokenapi.auth;

public final class AuthContext {
    public static final String REQUEST_ATTRIBUTE = AuthContext.class.getName() + ".authenticatedToken";

    private AuthContext() {
    }
}
