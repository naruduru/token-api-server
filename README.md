# token-api-server

Redis 기반 API 토큰 발급 서버입니다.

- 관리자 시크릿으로 토큰 발급
- 발급된 토큰은 Redis에 TTL과 함께 저장
- 클라이언트는 `Authorization: Bearer <token>` 으로 보호 API 접근
- 토큰 원문은 Redis에 직접 저장하지 않고 SHA-256 해시로 저장

## Requirements

- Java 21
- Redis
- Gradle Wrapper

기본 설정은 [application.properties](/Users/mingulee/vscode/token-api-server/src/main/resources/application.properties)에 있습니다.

```properties
server.port=8090
spring.data.redis.host=localhost
spring.data.redis.port=6379
token.api.admin-secret=change-me-admin-secret
token.api.default-ttl-seconds=3600
token.api.token-prefix=ruru
```

## Run

```bash
./gradlew bootRun
```

## Issue Token

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/tokens \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "clientId": "demo-client",
    "scopes": ["read", "write"],
    "ttlSeconds": 600
  }'
```

예시 응답:

```json
{
  "tokenType": "Bearer",
  "accessToken": "ruru_xxx",
  "tokenId": "abc123",
  "clientId": "demo-client",
  "scopes": ["read", "write"],
  "issuedAt": "2026-03-25T14:39:29.141998Z",
  "expiresAt": "2026-03-25T14:49:29.141998Z"
}
```

## Call Protected API

```bash
curl -s http://127.0.0.1:8090/api/secure/me \
  -H 'Authorization: Bearer <accessToken>'
```

```bash
curl -s http://127.0.0.1:8090/api/secure/ping \
  -H 'Authorization: Bearer <accessToken>'
```

## Public API

```bash
curl -s http://127.0.0.1:8090/api/public/health
```

## API Summary

- `POST /api/admin/tokens`
  - 헤더: `X-Admin-Secret`
  - body: `clientId`, `scopes`, `ttlSeconds`
- `GET /api/public/health`
  - 인증 없음
- `GET /api/secure/me`
  - Bearer 토큰 필요
- `GET /api/secure/ping`
  - Bearer 토큰 필요

## Notes

- `clientId`는 필수입니다.
- `ttlSeconds`를 생략하면 `token.api.default-ttl-seconds` 값을 사용합니다.
- Redis 키는 `api:token:<sha256>` 형식으로 저장됩니다.
- 운영 환경에서는 `token.api.admin-secret`를 반드시 안전한 값으로 바꿔야 합니다.
