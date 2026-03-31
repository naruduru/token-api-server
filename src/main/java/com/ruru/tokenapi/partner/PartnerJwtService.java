package com.ruru.tokenapi.partner;

import com.ruru.tokenapi.config.TokenApiProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class PartnerJwtService {
    private final SecretKey secretKey;
    private final TokenApiProperties properties;

    public PartnerJwtService(TokenApiProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(String clientId,
                             String tokenId,
                             String systemName,
                             List<String> scopes,
                             Instant issuedAt,
                             Instant expiresAt) {
        return Jwts.builder()
            .setSubject(clientId)
            .setIssuer(properties.getIssuer())
            .setIssuedAt(Date.from(issuedAt))
            .setExpiration(Date.from(expiresAt))
            .setId(tokenId)
            .claim("type", PartnerChannel.INTERNAL_SYSTEM.name())
            .claim("systemName", systemName)
            .claim("scope", scopes)
            .signWith(secretKey)
            .compact();
    }

    public ParsedPartnerToken parse(String token) {
        try {
            Claims payload = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            Object rawScopes = payload.get("scope");
            List<String> scopes = rawScopes instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
            String tokenType = payload.get("type", String.class);
            if (!PartnerChannel.INTERNAL_SYSTEM.name().equals(tokenType)) {
                return null;
            }
            return new ParsedPartnerToken(
                payload.getId(),
                payload.getSubject(),
                payload.getIssuer(),
                payload.get("systemName", String.class),
                scopes,
                payload.getExpiration().toInstant()
            );
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
