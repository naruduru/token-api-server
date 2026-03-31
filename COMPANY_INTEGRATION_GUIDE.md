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
- `GET /api/internal/ping`

## 필수 설정

`src/main/resources/application.properties` 기준:

```properties
token.api.admin-secret=change-me-admin-secret
token.api.access-token-ttl-seconds=1800
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
- 토큰 revoke는 관리자 API에서 `accessToken` 기준으로 처리
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

## 테스트

`src/test/java/com/ruru/tokenapi/partner/PartnerTokenServiceTest.java`에서 내부 토큰 발급/검증 및 예외 케이스를 확인합니다.
