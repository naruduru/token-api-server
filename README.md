# token-api-server

연계 시스템이 우리 API를 호출할 때 사용할 JWT 토큰을 발급/검증하는 서버입니다.

- 관리자 시크릿으로 호출 클라이언트 등록
- 등록된 클라이언트는 `clientId` / `clientSecret`으로 토큰 발급
- 발급 토큰 상태를 Redis에 TTL과 함께 저장
- `/api/internal/**` 요청은 Bearer JWT + Redis 상태로 검증
- `systemCode`와 `callSource` 조합 정책으로 발급 대상을 제어

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

## Run

```bash
./gradlew bootRun
```

## 1. Register Geumsangmall Front Client

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "clientId": "geumsangmall-front",
    "clientSecret": "front-secret",
    "systemCode": "GEUMSANGMALL",
    "callSource": "DMZ_FRONT",
    "active": true,
    "scopes": ["api.read"],
    "description": "금상몰 프론트 호출용"
  }'
```

## 2. Register Sign Counsel Backend Client

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "clientId": "sign-counsel-backend",
    "clientSecret": "backend-secret",
    "systemCode": "SIGN_COUNSEL",
    "callSource": "INTERNAL_BACKEND",
    "active": true,
    "scopes": ["api.read"],
    "description": "수어상담 백엔드 호출용"
  }'
```

## 3. Register Statistics Backend Client

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "clientId": "statistics-backend",
    "clientSecret": "statistics-secret",
    "systemCode": "STATISTICS",
    "callSource": "INTERNAL_BACKEND",
    "active": true,
    "scopes": ["api.read"],
    "description": "통계시스템 백엔드 호출용"
  }'
```

## 4. Issue Partner Token

```bash
curl -s -X POST http://127.0.0.1:8090/api/internal/token \
  -H 'Content-Type: application/json' \
  -d '{
    "clientId": "geumsangmall-front",
    "clientSecret": "front-secret"
  }'
```

## 5. Exchange Geumsangmall Front Token

```bash
curl -s -X POST http://127.0.0.1:8090/api/external/geumsangmall/token \
  -H 'Content-Type: application/json' \
  -d '{
    "mallUserId": "mall-user-1",
    "mallSessionId": "session-1"
  }'
```

## 6. Call Internal API

```bash
curl -s http://127.0.0.1:8090/api/internal/ping \
  -H 'Authorization: Bearer <partnerAccessToken>'
```

## 7. List Registered Clients

```bash
curl -s http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'X-Admin-Secret: change-me-admin-secret'
```

## 8. Revoke Partner Token

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-tokens/revoke \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "accessToken": "<partnerAccessToken>"
  }'
```

## 9. Get Revocation History

```bash
curl -s "http://127.0.0.1:8090/api/admin/partner-tokens/revocations?limit=20" \
  -H 'X-Admin-Secret: change-me-admin-secret'
```

## Public API

```bash
curl -s http://127.0.0.1:8090/api/public/health
```

## API Summary

- `POST /api/admin/partner-clients`
  - body: `clientId`, `clientSecret`, `systemCode`, `callSource`, `active`, `scopes`, `description`
- `GET /api/admin/partner-clients`
  - 등록된 클라이언트 목록 조회
- `GET /api/admin/partner-clients/{clientId}`
  - 등록된 클라이언트 단건 조회
- `POST /api/admin/partner-tokens/revoke`
  - body: `accessToken`
- `GET /api/admin/partner-tokens/revocations`
  - revoke 이력 조회
- `POST /api/external/geumsangmall/token`
  - body: `mallUserId`, `mallSessionId`
- `POST /api/internal/token`
  - body: `clientId`, `clientSecret`
- `GET /api/internal/ping`
  - 파트너 토큰 필요
- `GET /api/public/health`
  - 인증 없음

## JWT Claims

- 공통: `sub`, `iss`, `exp`, `jti`, `scope`
- 파트너 토큰: `systemCode`, `callSource`

## Redis Keys

- 클라이언트: `partner:client:<clientId>`
- 활성 토큰: `partner:token:<jti>`
- revoke: `partner:revoke:<jti>`

## Postman

- 컬렉션: `postman/token-api-server.collection.json`
- 환경: `postman/token-api-server.local.postman_environment.json`

## Notes

- 허용 조합은 설계 문서 `TOKEN_CLIENT_DESIGN.md` 기준으로 관리합니다.
- 금상몰 프론트 전용 교환 플로우는 `GEUMSANGMALL_TOKEN_EXCHANGE.md`를 참고합니다.
- 기본 운영 초기 등록 데이터는 `application.properties`의 `token.api.initial-clients`로 로드됩니다.
- 운영 환경에서는 각 `clientSecret`을 환경변수로 주입하는 전제를 둡니다.
- 운영 환경에서는 `token.api.admin-secret`, `token.api.jwt-secret`를 반드시 안전한 값으로 바꿔야 합니다.
