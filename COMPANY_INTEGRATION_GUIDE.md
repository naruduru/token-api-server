# Company Integration Guide

회사 프로젝트에 `JWT + Redis` 기반 **DMZ/내부망 시스템 토큰** 구조를 이식할 때 필요한 내용을 정리합니다.

## 목적

- DMZ/내부망 서버가 시스템 단위로 토큰 발급 요청
- 우리 시스템은 `channel`, `systemName`이 포함된 JWT 발급
- 각 시스템은 API 호출 시 Bearer 토큰 전달
- 검증은 `JWT 서명 검증 + Redis 상태 검증`

## 샘플 채널

- DMZ 프론트: `DMZ_FRONT`
- DMZ 백엔드: `DMZ_BACKEND`
- 내부망: `A`, `B`, `C`
- 권장 clientId 예시: `dmz-front-client`, `dmz-backend-client`, `internal-a-client`, `internal-b-client`, `internal-c-client`

## 주요 엔드포인트

- `POST /api/admin/partner-clients`
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

## Redis 키 구조

- 클라이언트 정보: `partner:client:{clientId}`
- 활성 토큰: `partner:{channel}:token:{jti}`
- revoke 키: `partner:{channel}:revoke:{jti}`

## JWT Claims

공통 claim:

- `sub`: clientId
- `iss`: issuer
- `exp`: 만료시간
- `jti`: 토큰 ID
- `type`: channel (`DMZ_FRONT`, `DMZ_BACKEND`, `A`, `B`, `C`)
- `scope`: 권한 목록
- `systemName`: 시스템 이름

## 발급 규칙

- `channel`은 등록된 클라이언트 channel과 일치해야 함
- `systemName`은 클라이언트 등록 시 필수
- 토큰 발급 시 `clientId`, `clientSecret`, `channel` 필수

## 검증 규칙

- JWT 서명
- issuer
- expiration
- channel parse/유효성
- `systemName` 존재 여부
- 등록된 활성 client 여부
- Redis 활성 토큰 존재 여부
- Redis revoke 여부

## 테스트

`src/test/java/com/ruru/tokenapi/partner/PartnerTokenServiceTest.java`에서 채널별 토큰 발급/검증 및 예외 케이스를 확인합니다.
