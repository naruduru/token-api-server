# token-api-server

DMZ/내부망 서버용 JWT 토큰을 발급/검증하는 서버입니다.

- 관리자 시크릿으로 파트너 클라이언트 등록
- 파트너 시스템은 `clientId` / `clientSecret` / `channel`로 토큰 발급
- 발급 토큰 상태를 Redis에 TTL과 함께 저장
- `/api/internal/**` 요청은 Bearer JWT + Redis 상태로 검증

## Requirements

- Java 21
- Redis
- Gradle Wrapper

기본 설정은 `src/main/resources/application.properties`에 있습니다.

```properties
server.port=8090
spring.data.redis.host=localhost
spring.data.redis.port=6379
token.api.admin-secret=change-me-admin-secret
token.api.access-token-ttl-seconds=1800
token.api.issuer=token-api-server
token.api.jwt-secret=change-me-jwt-secret-change-me-jwt-secret
```

## Channel Samples

- DMZ 프론트: `DMZ_FRONT`
- DMZ 백엔드: `DMZ_BACKEND`
- 내부망 서버: `A`, `B`, `C`

## Run

```bash
./gradlew bootRun
```

## 1. Register Partner Client

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "clientId": "dmz-backend-client",
    "clientSecret": "dmz-backend-secret",
    "active": true,
    "channel": "DMZ_BACKEND",
    "systemName": "dmz-backend",
    "scopes": ["order.read", "order.write"]
  }'
```

## 2. Issue Token

```bash
curl -s -X POST http://127.0.0.1:8090/api/internal/token \
  -H 'Content-Type: application/json' \
  -d '{
    "clientId": "dmz-backend-client",
    "clientSecret": "dmz-backend-secret",
    "channel": "DMZ_BACKEND"
  }'
```

## 3. Call Internal API

```bash
curl -s http://127.0.0.1:8090/api/internal/ping \
  -H 'Authorization: Bearer <accessToken>'
```

## Public API

```bash
curl -s http://127.0.0.1:8090/api/public/health
```

## API Summary

- `POST /api/admin/partner-clients`
  - 헤더: `X-Admin-Secret`
  - body: `clientId`, `clientSecret`, `active`, `channel`, `systemName`, `scopes`
- `POST /api/internal/token`
  - body: `clientId`, `clientSecret`, `channel`
- `GET /api/internal/ping`
  - 파트너 서버 토큰 필요
- `GET /api/public/health`
  - 인증 없음

## JWT Claims

- 공통: `sub`, `iss`, `exp`, `jti`, `type`, `scope`
- 추가: `systemName`

## Redis Keys

- 클라이언트: `partner:client:<clientId>`
- 토큰: `partner:<channel>:token:<jti>`
- revoke: `partner:<channel>:revoke:<jti>`

## Postman

- 컬렉션: `postman/token-api-server.collection.json`
- 환경: `postman/token-api-server.local.postman_environment.json`

## Notes

- 운영 환경에서는 `token.api.admin-secret`, `token.api.jwt-secret`를 반드시 안전한 값으로 바꿔야 합니다.
- `POST /api/internal/token` 호출 시 `channel`은 등록된 클라이언트의 `channel`과 동일해야 합니다.
