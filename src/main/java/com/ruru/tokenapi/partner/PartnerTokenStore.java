package com.ruru.tokenapi.partner;

import java.time.Duration;
import java.util.List;

public interface PartnerTokenStore {
    void saveActiveToken(String tokenId, ActivePartnerToken token, Duration ttl);
    ActivePartnerToken findActiveToken(String tokenId);
    void saveRefreshToken(String refreshToken, ActiveRefreshToken token, Duration ttl);
    ActiveRefreshToken findRefreshToken(String refreshToken);
    ActiveRefreshToken findRefreshTokenByAccessTokenId(String accessTokenId);
    void deleteRefreshToken(String refreshToken);
    void revoke(RevokedPartnerToken token, Duration ttl);
    boolean isRevoked(String tokenId);
    List<RevokedPartnerToken> findRevokedTokens(int limit);
}
