# Company Integration Guide

회사 프로젝트에 `JWT + Redis` 기반 파트너 토큰 구조를 이식할 때 필요한 내용을 정리합니다.

## 목적

두 가지 채널을 지원합니다.

- 외부 사용자용 토큰
  - 타 시스템이 사용자 단위로 토큰 발급 요청
  - 우리 시스템은 `userId`가 포함된 JWT 발급
  - 타 시스템은 우리 API 호출 시 Bearer 토큰 전달
- 내부 서버용 토큰
  - 내부망 서버가 시스템 단위로 토큰 발급 요청
  - 우리 시스템은 `systemName`이 포함된 JWT 발급
  - 내부 서버는 우리 API 호출 시 Bearer 토큰 전달

토큰 검증은 `JWT 서명 검증 + Redis 상태 검증`으로 처리합니다.

## 주요 엔드포인트

- `POST /api/admin/partner-clients`
  - 관리자 시크릿으로 클라이언트 등록
- `POST /api/external/token`
  - 외부 사용자용 토큰 발급
- `POST /api/internal/token`
  - 내부 서버용 토큰 발급
- `GET /api/external/ping`
  - 외부 사용자용 보호 API 샘플
- `GET /api/internal/ping`
  - 내부 서버용 보호 API 샘플

## 필수 설정

[application.properties](/Users/mingulee/vscode/token-api-server/src/main/resources/application.properties) 기준으로 아래 설정이 필요합니다.

```properties
token.api.admin-secret=change-me-admin-secret
token.api.access-token-ttl-seconds=1800
token.api.issuer=token-api-server
token.api.jwt-secret=change-me-jwt-secret-change-me-jwt-secret

spring.data.redis.host=localhost
spring.data.redis.port=6379
```

## 회사 프로젝트에 옮길 파일

### 1. 설정

- [TokenApiProperties.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/config/TokenApiProperties.java)
- [WebConfig.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/config/WebConfig.java)

### 2. 관리자 보호 및 등록 API

- [AdminProtectedController.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/api/AdminProtectedController.java)
- [AdminPartnerClientController.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/api/AdminPartnerClientController.java)

### 3. 토큰 발급 API

- [PartnerTokenController.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/api/PartnerTokenController.java)
- [IssuePartnerTokenRequest.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/IssuePartnerTokenRequest.java)
- [IssuePartnerTokenResponse.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/IssuePartnerTokenResponse.java)

### 4. 보호 API 샘플

- [PartnerApiController.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/api/PartnerApiController.java)

### 5. 인터셉터

- [PartnerApiTokenInterceptor.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/auth/PartnerApiTokenInterceptor.java)
- [AuthenticatedPartnerToken.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/auth/AuthenticatedPartnerToken.java)
- [AuthContext.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/auth/AuthContext.java)

### 6. 클라이언트 저장/조회

- [PartnerClient.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/client/PartnerClient.java)
- [RegisterPartnerClientRequest.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/client/RegisterPartnerClientRequest.java)
- [PartnerClientService.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/client/PartnerClientService.java)
- [PartnerClientStore.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/client/PartnerClientStore.java)
- [RedisPartnerClientStore.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/client/RedisPartnerClientStore.java)

### 7. JWT 발급/검증 및 토큰 상태 저장

- [PartnerChannel.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/PartnerChannel.java)
- [PartnerJwtService.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/PartnerJwtService.java)
- [PartnerTokenService.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/PartnerTokenService.java)
- [PartnerTokenStore.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/PartnerTokenStore.java)
- [RedisPartnerTokenStore.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/RedisPartnerTokenStore.java)
- [IssuedPartnerToken.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/IssuedPartnerToken.java)
- [ParsedPartnerToken.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/ParsedPartnerToken.java)

## Redis 키 구조

### 클라이언트 정보

```text
partner:client:{clientId}
```

값 구조:

```text
clientSecret|Y|EXTERNAL_USER||member.read,order.read
clientSecret|Y|INTERNAL_SYSTEM|order-sync-server|batch.read,batch.write
```

### 활성 토큰

외부 사용자:

```text
partner:external_user:token:{jti}
```

내부 서버:

```text
partner:internal_system:token:{jti}
```

값은 `clientId`이고 TTL은 access token 만료시간과 동일합니다.

### revoke 키

```text
partner:{channel}:revoke:{jti}
```

현재 revoke API는 없지만, 구조는 이미 반영돼 있습니다.

## JWT Claims

공통 claim:

- `sub`: clientId
- `iss`: issuer
- `exp`: 만료시간
- `jti`: 토큰 ID
- `type`: `EXTERNAL_USER` 또는 `INTERNAL_SYSTEM`
- `scope`: 권한 목록

외부 사용자 전용 claim:

- `userId`

내부 서버 전용 claim:

- `systemName`

## 발급 규칙

### 외부 사용자용

- 채널은 `EXTERNAL_USER`
- `userId` 필수
- `systemName`은 클라이언트 등록 정보에 없어도 됨

### 내부 서버용

- 채널은 `INTERNAL_SYSTEM`
- `userId` 금지
- `systemName`은 클라이언트 등록 시 필수

## 검증 규칙

[PartnerTokenService.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/PartnerTokenService.java) 기준으로 아래를 검증합니다.

- JWT 서명
- issuer
- expiration
- channel 일치 여부
- 외부 사용자 토큰의 `userId` 존재 여부
- 내부 서버 토큰의 `systemName` 존재 여부
- Redis 활성 토큰 존재 여부
- Redis revoke 여부

## jjwt 버전

회사 프로젝트가 `jjwt 0.11.x`라면 현재 코드와 맞습니다.

[PartnerJwtService.java](/Users/mingulee/vscode/token-api-server/src/main/java/com/ruru/tokenapi/partner/PartnerJwtService.java) 는 아래 스타일을 사용합니다.

```java
Jwts.parserBuilder()
    .setSigningKey(secretKey)
    .build()
    .parseClaimsJws(token)
    .getBody();
```

## 의존성

[build.gradle](/Users/mingulee/vscode/token-api-server/build.gradle) 기준:

```gradle
implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'
```

## 호출 예시

상세 예시는 아래 문서를 그대로 사용하면 됩니다.

- [README.md](/Users/mingulee/vscode/token-api-server/README.md)
- [token-api-server.collection.json](/Users/mingulee/vscode/token-api-server/postman/token-api-server.collection.json)

기본 순서는 다음과 같습니다.

### 외부 사용자

1. `POST /api/admin/partner-clients`
2. `POST /api/external/token`
3. `GET /api/external/ping`

### 내부 서버

1. `POST /api/admin/partner-clients`
2. `POST /api/internal/token`
3. `GET /api/internal/ping`

## 테스트

서비스 단위 검증은 [PartnerTokenServiceTest.java](/Users/mingulee/vscode/token-api-server/src/test/java/com/ruru/tokenapi/partner/PartnerTokenServiceTest.java) 에 있습니다.

확인하는 항목:

- 외부 사용자 토큰 발급/검증
- 내부 서버 토큰 발급/검증
- 외부 사용자 토큰의 `userId` 필수 검증
- 내부 서버 토큰의 `userId` 금지 검증
- 채널 mismatch 검증

## 운영 메모

- DB 없이 Redis만 사용합니다.
- Redis 데이터가 유실되면 등록 클라이언트와 활성 토큰 상태도 함께 유실됩니다.
- 운영 환경에서는 `clientSecret` 평문 저장 대신 해시 저장으로 바꾸는 것이 좋습니다.
- 필요 시 다음 기능을 추가하면 됩니다.
  - revoke API
  - scope 권한 체크
  - rate limit
  - IP 제한
