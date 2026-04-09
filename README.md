# token-api-server

연계 시스템이 우리 API를 호출할 때 사용할 인증 정보를 관리하는 서버입니다.

- 공용 shared secret은 우리 서버가 1개 발급하고 모든 클라이언트가 공통으로 사용
- 클라이언트 ID는 우리 서버가 생성해서 각 클라이언트에 전달
- 금상몰만 `clientId` / `sharedSecret`으로 access token을 발급
- 다른 내부 시스템은 `clientId` / `sharedSecret` 헤더로 바로 호출
- `/api/internal/**` 요청은 Bearer JWT 또는 `X-Client-Id` / `X-Client-Secret` 헤더로 검증
- 발급 토큰 상태는 Redis에 TTL과 함께 저장

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

## 1. Issue Shared Secret

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-shared-secret \
  -H 'X-Admin-Secret: change-me-admin-secret'
```

응답 예시:

```json
{
  "message": "shared secret ready",
  "sharedSecret": "<shared-secret>"
}
```

## 2. Register Geumsangmall Front Client

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "systemCode": "GEUMSANGMALL",
    "callSource": "DMZ_FRONT",
    "active": true,
    "scopes": ["api.read"],
    "description": "금상몰 프론트 호출용"
  }'
```

응답으로 서버가 생성한 `clientId`와 공용 `clientSecret(shared secret)`을 내려줍니다.

## 3. Register Internal Backend Client

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "systemCode": "STATISTICS",
    "callSource": "INTERNAL_BACKEND",
    "active": true,
    "scopes": ["api.read"],
    "description": "통계시스템 백엔드 호출용"
  }'
```

## 4. Issue Geumsangmall Token

```bash
curl -s -X POST http://127.0.0.1:8090/api/internal/token \
  -H 'Content-Type: application/json' \
  -d '{
    "clientId": "geumsangmall-front",
    "clientSecret": "<shared-secret>"
  }'
```

응답 예시:

```json
{
  "accessToken": "<jwt-access-token>",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "systemCode": "GEUMSANGMALL",
  "callSource": "DMZ_FRONT"
}
```

금상몰 백엔드는 토큰을 발급받고, 금상몰 프론트는 이 Bearer 토큰으로 우리 REST API와 WSS를 호출합니다.

## 5. Call Internal API

Bearer 토큰 호출:

```bash
curl -s http://127.0.0.1:8090/api/internal/ping \
  -H 'Authorization: Bearer <partnerAccessToken>'
```

내부 서버투서버 호출:

```bash
curl -s http://127.0.0.1:8090/api/internal/ping \
  -H 'X-Client-Id: statistics-backend' \
  -H 'X-Client-Secret: <shared-secret>'
```

규칙:

- 금상몰은 토큰 만료 시 `clientId/sharedSecret`으로 다시 access token 발급
- 다른 내부 시스템은 토큰 없이 `X-Client-Id`, `X-Client-Secret` 헤더만 사용

## 6. List Registered Clients

```bash
curl -s http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'X-Admin-Secret: change-me-admin-secret'
```

응답에는 각 클라이언트의 현재 활성 access token 목록이 `tokens` 필드로 포함됩니다.

## 7. Delete Client

```bash
curl -s -X DELETE http://127.0.0.1:8090/api/admin/partner-clients/geumsangmall-front \
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

- `POST /api/admin/partner-shared-secret`
  - 공용 shared secret 발급 또는 기존 값 반환
- `POST /api/admin/partner-clients`
  - body: `systemCode`, `callSource`, `active`, `scopes`, `description`
  - 서버가 `clientId`를 생성하고 공용 `clientSecret`을 함께 응답
- `GET /api/admin/partner-clients`
  - 등록된 클라이언트 목록 조회, 활성 access token 포함
- `GET /api/admin/partner-clients/{clientId}`
  - 등록된 클라이언트 단건 조회, 활성 access token 포함
- `DELETE /api/admin/partner-clients/{clientId}`
  - 등록된 클라이언트 삭제, 활성 access token 정리
- `POST /api/admin/partner-tokens/revoke`
  - body: `accessToken`
- `GET /api/admin/partner-tokens/revocations`
  - revoke 이력 조회
- `POST /api/internal/token`
  - body: `clientId`, `clientSecret`
  - `DMZ_FRONT` 클라이언트만 access token 발급 가능
- `GET /api/internal/ping`
  - 파트너 토큰 또는 서버 시크릿 필요
- `GET /api/public/health`
  - 인증 없음

## JWT Claims

- 공통: `sub`, `iss`, `exp`, `jti`, `scope`
- 파트너 토큰: `systemCode`, `callSource`

## Redis Keys

- 클라이언트: `partner:client:<clientId>`
- 활성 토큰: `partner:token:<jti>`
- 클라이언트별 활성 토큰 인덱스: `partner:client:tokens:<clientId>`
- 공용 shared secret: `partner:shared-secret`
- revoke: `partner:revoke:<jti>`

## Postman

- 컬렉션: `postman/token-api-server.collection.json`
- 환경: `postman/token-api-server.local.postman_environment.json`

## Notes

- 허용 조합은 설계 문서 `TOKEN_CLIENT_DESIGN.md` 기준으로 관리합니다.
- Bearer 토큰을 서버에서 추출하고 사용하는 샘플은 `AUTHORIZATION_HEADER_SAMPLE.md`를 참고합니다.
- 기본 운영 초기 등록 데이터는 `application.properties`의 `token.api.initial-clients`로 로드됩니다.
- 운영 환경에서는 `token.api.admin-secret`, `token.api.jwt-secret`를 반드시 안전한 값으로 바꿔야 합니다.
