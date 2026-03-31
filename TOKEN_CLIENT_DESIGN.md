# Token Client Design

## 목적

외부/내부 연계 시스템이 우리 API를 호출할 때 사용할 서버 간 인증 토큰 체계를 정의한다.

- 저장소는 Redis만 사용한다.
- 금상몰은 프론트 직접 호출을 유지한다.
- 토큰 발급 단위는 시스템 자체가 아니라 등록된 호출 클라이언트다.

## 대상 시스템

| 시스템 | systemCode | 실제 호출 주체 | callSource | 토큰 발급 허용 | 비고 |
|---|---|---|---|---|---|
| 금상몰 | `GEUMSANGMALL` | DMZ 프론트 | `DMZ_FRONT` | 허용 | 현재 운영 방식 유지 |
| 수어상담 | `SIGN_COUNSEL` | 내부망 백엔드 | `INTERNAL_BACKEND` | 허용 | 백엔드 직접 호출 |
| 통계시스템 | `STATISTICS` | 내부망 백엔드 | `INTERNAL_BACKEND` | 허용 | 백엔드 직접 호출 |
| 상담APP | `COUNSEL_APP` | 내부망 백엔드 | `INTERNAL_BACKEND` | 허용 | 백엔드 직접 호출 |

## 핵심 설계 원칙

- 인증 대상은 시스템이 아니라 등록된 호출 클라이언트다.
- 각 클라이언트는 `systemCode`와 `callSource`를 가진다.
- 허용 조합 외 발급은 모두 거부한다.
- `DMZ_FRONT`는 현재 `GEUMSANGMALL`만 허용한다.
- 각 시스템은 전용 `clientId`, `clientSecret`을 사용한다.
- 인증은 `JWT + Redis 활성 토큰 확인`으로 처리한다.

## 용어

### systemCode

- `GEUMSANGMALL`
- `SIGN_COUNSEL`
- `STATISTICS`
- `COUNSEL_APP`

### callSource

- `DMZ_FRONT`
- `INTERNAL_BACKEND`

## 등록 클라이언트 예시

| clientId | systemCode | callSource |
|---|---|---|
| `geumsangmall-front` | `GEUMSANGMALL` | `DMZ_FRONT` |
| `sign-counsel-backend` | `SIGN_COUNSEL` | `INTERNAL_BACKEND` |
| `statistics-backend` | `STATISTICS` | `INTERNAL_BACKEND` |
| `counsel-app-backend` | `COUNSEL_APP` | `INTERNAL_BACKEND` |

각 클라이언트는 아래 속성을 가진다.

- `clientId`
- `clientSecret`
- `systemCode`
- `callSource`
- `active`
- `scopes`
- `description`

## JWT Claim

- `sub`: `clientId`
- `iss`
- `exp`
- `jti`
- `systemCode`
- `callSource`
- `scope`

## Redis 구조

- 클라이언트 정보: `partner:client:{clientId}`
- 활성 토큰: `partner:token:{jti}`
- 폐기 토큰: `partner:revoke:{jti}`

클라이언트 정보 값 예시:

```json
{
  "clientId": "geumsangmall-front",
  "clientSecret": "****",
  "systemCode": "GEUMSANGMALL",
  "callSource": "DMZ_FRONT",
  "active": true,
  "scopes": ["api.read"],
  "description": "금상몰 프론트 호출용"
}
```

활성 토큰 값 예시:

```json
{
  "clientId": "geumsangmall-front",
  "systemCode": "GEUMSANGMALL",
  "callSource": "DMZ_FRONT",
  "issuedAt": "2026-03-31T14:00:00Z",
  "expiresAt": "2026-03-31T14:30:00Z"
}
```

## API 명세

### 1. 관리자용 클라이언트 등록

`POST /api/admin/partner-clients`

헤더:

```http
Content-Type: application/json
X-Admin-Secret: {adminSecret}
```

요청:

```json
{
  "clientId": "geumsangmall-front",
  "clientSecret": "front-secret",
  "systemCode": "GEUMSANGMALL",
  "callSource": "DMZ_FRONT",
  "active": true,
  "scopes": ["api.read"],
  "description": "금상몰 프론트 호출용"
}
```

필드 규칙:

- `clientId`: 필수, 고유값
- `clientSecret`: 필수
- `systemCode`: 필수, 허용값은 `GEUMSANGMALL`, `SIGN_COUNSEL`, `STATISTICS`, `COUNSEL_APP`
- `callSource`: 필수, 허용값은 `DMZ_FRONT`, `INTERNAL_BACKEND`
- `active`: 필수
- `scopes`: 선택, 없으면 빈 배열
- `description`: 선택

성공 응답:

```json
{
  "message": "client registered",
  "clientId": "geumsangmall-front",
  "systemCode": "GEUMSANGMALL",
  "callSource": "DMZ_FRONT",
  "active": true
}
```

실패 응답 예시:

```json
{
  "error": "systemCode and callSource combination is not allowed"
}
```

### 1-1. 관리자용 클라이언트 목록 조회

`GET /api/admin/partner-clients`

헤더:

```http
X-Admin-Secret: {adminSecret}
```

성공 응답:

```json
[
  {
    "clientId": "geumsangmall-front",
    "systemCode": "GEUMSANGMALL",
    "callSource": "DMZ_FRONT",
    "active": true,
    "scopes": ["api.read"],
    "description": "금상몰 프론트 호출용"
  }
]
```

### 1-2. 관리자용 클라이언트 단건 조회

`GET /api/admin/partner-clients/{clientId}`

헤더:

```http
X-Admin-Secret: {adminSecret}
```

### 2. 토큰 발급

`POST /api/internal/token`

헤더:

```http
Content-Type: application/json
```

요청:

```json
{
  "clientId": "geumsangmall-front",
  "clientSecret": "front-secret"
}
```

성공 응답:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "systemCode": "GEUMSANGMALL",
  "callSource": "DMZ_FRONT"
}
```

실패 응답 예시:

```json
{
  "error": "Invalid client credentials"
}
```

### 2-1. 관리자용 토큰 revoke

`POST /api/admin/partner-tokens/revoke`

헤더:

```http
Content-Type: application/json
X-Admin-Secret: {adminSecret}
```

요청:

```json
{
  "accessToken": "<jwt>"
}
```

성공 응답:

```json
{
  "message": "token revoked",
  "tokenId": "...",
  "clientId": "geumsangmall-front",
  "systemCode": "GEUMSANGMALL",
  "callSource": "DMZ_FRONT",
  "revokedAt": "2026-04-01T00:00:00Z"
}
```

### 2-2. 관리자용 revoke 이력 조회

`GET /api/admin/partner-tokens/revocations?limit=20`

헤더:

```http
X-Admin-Secret: {adminSecret}
```

성공 응답:

```json
[
  {
    "tokenId": "...",
    "clientId": "statistics-backend",
    "systemCode": "STATISTICS",
    "callSource": "INTERNAL_BACKEND",
    "revokedAt": "2026-04-01T00:00:00Z",
    "expiresAt": "2026-04-01T00:30:00Z"
  }
]
```

### 3. 보호 API 호출 예시

`GET /api/internal/ping`

헤더:

```http
Authorization: Bearer <jwt>
```

성공 응답 예시:

```json
{
  "message": "success",
  "clientId": "geumsangmall-front",
  "systemCode": "GEUMSANGMALL",
  "callSource": "DMZ_FRONT",
  "scopes": ["api.read"],
  "tokenId": "..."
}
```

인증 실패 응답 예시:

```json
{
  "error": "Invalid or expired token"
}
```

## 검증 규칙

### 토큰 발급 시

1. `clientId` 존재 확인
2. `clientSecret` 일치 확인
3. `active=true` 확인
4. `systemCode + callSource` 허용 조합 확인

### API 인증 시

1. JWT 서명 검증
2. `iss` 검증
3. `exp` 검증
4. `jti` 존재 검증
5. Redis 활성 토큰 존재 검증
6. Redis revoke 키 존재 시 거부

## 구현 범위

다음 구현은 아래 범위를 기준으로 진행한다.

1. 요청/응답 DTO를 `systemCode`, `callSource` 기준으로 재정의
2. 허용 조합 정책을 서비스 레벨에서 검증
3. JWT claim을 `systemCode`, `callSource`로 변경
4. Redis 저장 포맷을 클라이언트 메타정보 중심으로 정리
5. 테스트를 4개 시스템 정책 기준으로 다시 작성

## 운영 초기 등록 데이터

애플리케이션 시작 시 `token.api.initial-clients` 프로퍼티를 읽어 Redis에 초기 클라이언트를 등록한다.

- 이미 같은 `clientId`가 있으면 덮어쓰지 않고 건너뜀
- 기본 샘플은 `application.properties`에 4개 시스템 기준으로 등록됨
- 실제 운영에서는 각 `clientSecret`을 환경변수로 주입해서 사용

예시:

```properties
token.api.initial-clients[0].client-id=geumsangmall-front
token.api.initial-clients[0].client-secret=${GEUMSANGMALL_FRONT_CLIENT_SECRET}
token.api.initial-clients[0].system-code=GEUMSANGMALL
token.api.initial-clients[0].call-source=DMZ_FRONT
token.api.initial-clients[0].active=true
token.api.initial-clients[0].scopes[0]=api.read
```

## 금상몰 프론트 예외 플로우

금상몰 프론트는 `clientSecret` 없이 우리 전용 exchange API를 호출한다.

- 우리 API: `POST /api/external/geumsangmall/token`
- 우리 서버는 금상몰 백엔드 검증 API를 서버 간 호출
- 검증 성공 시 짧은 TTL 토큰 발급

상세 스펙은 `GEUMSANGMALL_TOKEN_EXCHANGE.md`를 참고한다.
