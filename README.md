# token-api-server

외부 사용자용 토큰과 내부망 서버용 토큰을 발급하는 JWT 토큰 서버입니다.

- 관리자 시크릿으로 파트너 클라이언트 등록
- 외부 시스템은 `clientId` / `clientSecret` / `userId`로 사용자별 토큰 발급
- 내부망 시스템은 `clientId` / `clientSecret`으로 서버용 토큰 발급
- 발급된 토큰 상태는 Redis에 TTL과 함께 저장
- `/api/external/**`, `/api/internal/**` 요청은 Bearer JWT + Redis 상태로 검증

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
token.api.access-token-ttl-seconds=1800
token.api.issuer=token-api-server
token.api.jwt-secret=change-me-jwt-secret-change-me-jwt-secret
```

## Run

```bash
./gradlew bootRun
```

## 1. Register External User Client

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "clientId": "partner-app",
    "clientSecret": "secret123",
    "active": true,
    "channel": "EXTERNAL_USER",
    "scopes": ["member.read", "order.read"]
  }'
```

## 2. Issue External User Token

```bash
curl -s -X POST http://127.0.0.1:8090/api/external/token \
  -H 'Content-Type: application/json' \
  -d '{
    "clientId": "partner-app",
    "clientSecret": "secret123",
    "userId": "user-100"
  }'
```

예시 응답:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

## 3. Call External API

```bash
curl -s http://127.0.0.1:8090/api/external/ping \
  -H 'Authorization: Bearer <externalAccessToken>'
```

## 4. Register Internal System Client

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "clientId": "intranet-batch",
    "clientSecret": "internal-secret",
    "active": true,
    "channel": "INTERNAL_SYSTEM",
    "systemName": "order-sync-server",
    "scopes": ["batch.read", "batch.write"]
  }'
```

## 5. Issue Internal System Token

```bash
curl -s -X POST http://127.0.0.1:8090/api/internal/token \
  -H 'Content-Type: application/json' \
  -d '{
    "clientId": "intranet-batch",
    "clientSecret": "internal-secret"
  }'
```

## 6. Call Internal API

```bash
curl -s http://127.0.0.1:8090/api/internal/ping \
  -H 'Authorization: Bearer <internalAccessToken>'
```

## Public API

```bash
curl -s http://127.0.0.1:8090/api/public/health
```

## API Summary

- `POST /api/admin/partner-clients`
  - 헤더: `X-Admin-Secret`
  - body: `clientId`, `clientSecret`, `active`, `channel`, `systemName`, `scopes`
- `POST /api/external/token`
  - body: `clientId`, `clientSecret`, `userId`
- `POST /api/internal/token`
  - body: `clientId`, `clientSecret`
- `GET /api/external/ping`
  - 외부 사용자 토큰 필요
- `GET /api/internal/ping`
  - 내부 서버 토큰 필요
- `GET /api/public/health`
  - 인증 없음

## JWT Claims

- 공통: `sub`, `iss`, `exp`, `jti`, `type`, `scope`
- 외부 사용자 토큰: `userId`
- 내부 서버 토큰: `systemName`

## Redis Keys

- 클라이언트: `partner:client:<clientId>`
- 외부 토큰: `partner:external_user:token:<jti>`
- 내부 토큰: `partner:internal_system:token:<jti>`
- revoke: `partner:<channel>:revoke:<jti>`

## Postman

- 컬렉션: [postman/token-api-server.collection.json](/Users/mingulee/vscode/token-api-server/postman/token-api-server.collection.json)
- 환경: [postman/token-api-server.local.postman_environment.json](/Users/mingulee/vscode/token-api-server/postman/token-api-server.local.postman_environment.json)

## Notes

- 외부 사용자 토큰은 `userId`가 필수입니다.
- 내부 서버 토큰은 `userId`를 받지 않습니다.
- 운영 환경에서는 `token.api.admin-secret`, `token.api.jwt-secret`를 반드시 안전한 값으로 바꿔야 합니다.
