# token-api-server

연계 시스템이 우리 API를 호출할 때 사용할 인증 정보를 관리하는 서버입니다.

- 관리자 시크릿으로 호출 클라이언트 등록
- 등록된 클라이언트는 필요 시 `clientId` / `clientSecret`으로 토큰 발급
- 발급 토큰 상태를 Redis에 TTL과 함께 저장
- `/api/internal/**` 요청은 Bearer JWT + Redis 상태 또는 `clientId/clientSecret` 헤더로 검증
- 금상몰 WSS 연결용 30초 임시 비밀키를 Redis에서 관리
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
token.api.refresh-token-ttl-seconds=1209600
token.api.issuer=token-api-server
token.api.jwt-secret=change-me-jwt-secret-change-me-jwt-secret
token.api.geumsangmall.client-id=geumsangmall-front
token.api.geumsangmall.wss-secret-ttl-seconds=30
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

응답 예시:

```json
{
  "accessToken": "<jwt-access-token>",
  "refreshToken": "<opaque-refresh-token>",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "refreshExpiresIn": 1209600,
  "systemCode": "GEUMSANGMALL",
  "callSource": "DMZ_FRONT"
}
```

금상몰 백엔드는 토큰을 발급받고, 금상몰 프론트는 이 Bearer 토큰으로 우리 API를 호출합니다.

## 5. Issue Geumsangmall WSS Secret

```bash
curl -s -X POST http://127.0.0.1:8090/api/external/geumsangmall/wss-secret \
  -H 'X-Client-Id: geumsangmall-front' \
  -H 'X-Client-Secret: front-secret'
```

## 6. Call Internal API

```bash
curl -s http://127.0.0.1:8090/api/internal/ping \
  -H 'Authorization: Bearer <partnerAccessToken>'
```

내부 시스템 서버투서버 호출:

```bash
curl -s http://127.0.0.1:8090/api/internal/ping \
  -H 'X-Client-Id: statistics-backend' \
  -H 'X-Client-Secret: statistics-secret'
```

외부 시스템 호출 규칙:

- access token은 항상 `Authorization: Bearer {accessToken}` 헤더로 전달
- access token 만료 전까지는 재발급 없이 재사용
- `401 Invalid or expired token` 응답을 받으면 refresh API 또는 `clientId/clientSecret`으로 다시 토큰 발급
- 내부 서버투서버 호출은 `X-Client-Id`, `X-Client-Secret` 헤더 사용

예시:

```bash
ACCESS_TOKEN="<jwt-access-token>"

curl -s http://127.0.0.1:8090/api/internal/ping \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

Java 예시:

```java
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(accessToken);

HttpEntity<Void> entity = new HttpEntity<>(headers);
ResponseEntity<String> response = restTemplate.exchange(
    "http://127.0.0.1:8090/api/internal/ping",
    HttpMethod.GET,
    entity,
    String.class
);
```

refresh 예시:

```bash
curl -s -X POST http://127.0.0.1:8090/api/internal/token/refresh \
  -H 'Content-Type: application/json' \
  -d '{
    "clientId": "geumsangmall-front",
    "refreshToken": "<opaque-refresh-token>"
  }'
```

## 7. List Registered Clients

```bash
curl -s http://127.0.0.1:8090/api/admin/partner-clients \
  -H 'X-Admin-Secret: change-me-admin-secret'
```

응답에는 각 클라이언트의 현재 활성 access token 목록이 `tokens` 필드로 포함됩니다.

## 8. Delete Client

```bash
curl -s -X DELETE http://127.0.0.1:8090/api/admin/partner-clients/geumsangmall-front \
  -H 'X-Admin-Secret: change-me-admin-secret'
```

## 9. Rotate Client Secret

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-clients/statistics-backend/secret \
  -H 'X-Admin-Secret: change-me-admin-secret'
```

## 10. Revoke Partner Token

```bash
curl -s -X POST http://127.0.0.1:8090/api/admin/partner-tokens/revoke \
  -H 'Content-Type: application/json' \
  -H 'X-Admin-Secret: change-me-admin-secret' \
  -d '{
    "accessToken": "<partnerAccessToken>"
  }'
```

## 11. Get Revocation History

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
  - body: `clientId`, `clientSecret(optional)`, `systemCode`, `callSource`, `active`, `scopes`, `description`
  - `clientSecret`이 비어 있으면 서버가 안전한 랜덤 시크릿을 생성해 응답
- `GET /api/admin/partner-clients`
  - 등록된 클라이언트 목록 조회, 활성 access token 포함
- `GET /api/admin/partner-clients/{clientId}`
  - 등록된 클라이언트 단건 조회, 활성 access token 포함
- `DELETE /api/admin/partner-clients/{clientId}`
  - 등록된 클라이언트 삭제, 활성 access token 정리
- `POST /api/admin/partner-clients/{clientId}/secret`
  - client secret 재생성
- `POST /api/admin/partner-tokens/revoke`
  - body: `accessToken`
- `GET /api/admin/partner-tokens/revocations`
  - revoke 이력 조회
- `POST /api/external/geumsangmall/wss-secret`
  - header: `X-Client-Id`, `X-Client-Secret`
  - 금상몰 WSS 연결용 30초 임시 비밀키 발급
- `POST /api/internal/token`
  - body: `clientId`, `clientSecret`
- `POST /api/internal/token/refresh`
  - body: `clientId`, `refreshToken`
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
- refresh 토큰: `partner:refresh:<refreshToken>`
- access 기준 refresh 인덱스: `partner:refresh:access:<jti>`
- 클라이언트별 활성 토큰 인덱스: `partner:client:tokens:<clientId>`
- 금상몰 WSS 임시 비밀키: `geumsangmall:wss-secret:<secret>`
- revoke: `partner:revoke:<jti>`

## Postman

- 컬렉션: `postman/token-api-server.collection.json`
- 환경: `postman/token-api-server.local.postman_environment.json`

## Notes

- 허용 조합은 설계 문서 `TOKEN_CLIENT_DESIGN.md` 기준으로 관리합니다.
- Bearer 토큰을 서버에서 추출하고 사용하는 샘플은 `AUTHORIZATION_HEADER_SAMPLE.md`를 참고합니다.
- 기본 운영 초기 등록 데이터는 `application.properties`의 `token.api.initial-clients`로 로드됩니다.
- 운영 환경에서는 각 `clientSecret`을 환경변수로 주입하는 전제를 둡니다.
- 운영 환경에서는 `token.api.admin-secret`, `token.api.jwt-secret`를 반드시 안전한 값으로 바꿔야 합니다.
