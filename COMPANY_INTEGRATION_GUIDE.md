# Company Integration Guide

회사 프로젝트에 `JWT + Redis` 기반 파트너 토큰 구조를 이식할 때 필요한 내용을 정리합니다.

## 목적

- 등록된 호출 클라이언트가 토큰 발급 요청
- 우리 시스템은 `systemCode`, `callSource`가 포함된 JWT 발급
- 호출 시스템은 API 호출 시 Bearer 토큰 전달
- 검증은 `JWT 서명 검증 + Redis 상태 검증`

## 주요 엔드포인트

- `POST /api/admin/partner-clients`
- `GET /api/admin/partner-clients`
- `GET /api/admin/partner-clients/{clientId}`
- `POST /api/admin/partner-tokens/revoke`
- `GET /api/admin/partner-tokens/revocations`
- `POST /api/external/geumsangmall/token`
- `POST /api/internal/token`
- `POST /api/internal/token/refresh`
- `GET /api/internal/ping`

## 필수 설정

`src/main/resources/application.properties` 기준:

```properties
token.api.admin-secret=change-me-admin-secret
token.api.access-token-ttl-seconds=1800
token.api.refresh-token-ttl-seconds=1209600
token.api.issuer=token-api-server
token.api.jwt-secret=change-me-jwt-secret-change-me-jwt-secret

spring.data.redis.host=localhost
spring.data.redis.port=6379
```

운영 초기 등록 데이터는 `token.api.initial-clients`로 설정한다.
각 `clientSecret`은 환경변수 치환 방식 사용을 권장한다.

## Redis 키 구조

- 클라이언트 정보: `partner:client:{clientId}`
- 활성 토큰: `partner:token:{jti}`
- refresh 토큰: `partner:refresh:{refreshToken}`
- access 기준 refresh 인덱스: `partner:refresh:access:{jti}`
- revoke 키: `partner:revoke:{jti}`

## JWT Claims

공통 claim:

- `sub`: clientId
- `iss`: issuer
- `exp`: 만료시간
- `jti`: 토큰 ID
- `systemCode`: `GEUMSANGMALL`, `SIGN_COUNSEL`, `STATISTICS`, `COUNSEL_APP`
- `scope`: 권한 목록
- `callSource`: `DMZ_FRONT`, `INTERNAL_BACKEND`

## 발급 규칙

- `systemCode`와 `callSource`는 클라이언트 등록 시 필수
- 허용 조합 외 등록과 발급은 거부
- 토큰 발급 시 `clientId`, `clientSecret` 필수
- 최초 발급 응답에는 `accessToken`, `refreshToken`이 같이 포함
- refresh 호출 시 `clientId`, `refreshToken`으로 새 access token 재발급
- refresh 성공 시 기존 refresh token은 즉시 폐기하고 새 refresh token 발급
- 토큰 revoke는 관리자 API에서 `accessToken` 기준으로 처리
- access token revoke 시 연결된 refresh token도 함께 무효화
- revoke 이력은 Redis에 만료 시각까지 유지되는 메타정보로 조회
- 금상몰 프론트는 전용 exchange API와 금상몰 백엔드 검증 API를 사용

## 검증 규칙

- JWT 서명
- issuer
- expiration
- `systemCode` enum 일치 여부
- `callSource` enum 일치 여부
- Redis 활성 토큰 존재 여부
- Redis revoke 여부

## 타 시스템 구현 가이드

권장 순서:

1. 시스템 기동 시 또는 첫 호출 전에 `POST /api/internal/token`으로 `accessToken`, `refreshToken` 발급
2. 업무 API 호출 시 `Authorization: Bearer {accessToken}` 헤더 사용
3. `401 Invalid or expired token` 응답이면 `POST /api/internal/token/refresh` 호출
4. refresh 성공 응답으로 받은 새 `accessToken`, 새 `refreshToken`으로 저장값 교체
5. refresh 실패 시에는 저장된 토큰을 폐기하고 `clientId/clientSecret` 기반 신규 발급으로 복구

주의사항:

- `accessToken`은 호출 헤더 전용으로 사용하고 평문 로그에 남기지 않는다.
- `refreshToken`은 브라우저 저장소가 아니라 서버 측 비밀 저장소에만 둔다.
- refresh token은 1회 성공 사용 시 회전되므로, 예전 값을 재사용하면 실패한다.
- 여러 인스턴스가 같은 클라이언트 자격증명을 공유한다면 토큰 저장소를 공용 Redis 또는 DB로 통합하는 편이 안전하다.

curl 예시:

```bash
curl -s http://127.0.0.1:8090/api/internal/ping \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

Spring 예시:

```java
HttpHeaders headers = new HttpHeaders();
headers.setBearerAuth(accessToken);

HttpEntity<MyRequest> entity = new HttpEntity<>(requestBody, headers);
ResponseEntity<MyResponse> response = restTemplate.exchange(
    targetUrl,
    HttpMethod.POST,
    entity,
    MyResponse.class
);
```

refresh 예시:

```bash
curl -s -X POST http://127.0.0.1:8090/api/internal/token/refresh \
  -H 'Content-Type: application/json' \
  -d '{
    "clientId": "statistics-backend",
    "refreshToken": "'"${REFRESH_TOKEN}"'"
  }'
```

Bearer 토큰을 서버 코드에서 추출하고 request attribute로 넘기는 샘플은 `AUTHORIZATION_HEADER_SAMPLE.md`를 참고한다.

## 테스트

`src/test/java/com/ruru/tokenapi/partner/PartnerTokenServiceTest.java`에서 내부 토큰 발급/검증 및 예외 케이스를 확인합니다.
