# Geumsangmall Token Exchange

## 목적

금상몰 프론트가 `clientSecret` 없이 우리 토큰을 발급받도록 하기 위한 전용 교환 플로우다.

흐름:

1. 금상몰 프론트가 우리 서버의 `POST /api/external/geumsangmall/token` 호출
2. 우리 서버가 금상몰 백엔드 검증 API 호출
3. 금상몰 백엔드가 세션/사용자 유효성을 응답
4. 우리 서버가 짧은 TTL의 토큰 발급
5. 금상몰 프론트가 발급 토큰으로 우리 보호 API 호출

## 우리 서버 API

### `POST /api/external/geumsangmall/token`

요청:

```json
{
  "mallUserId": "mall-user-1",
  "mallSessionId": "session-1"
}
```

성공 응답:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "systemCode": "GEUMSANGMALL",
  "callSource": "DMZ_FRONT"
}
```

실패 응답:

```json
{
  "error": "invalid session"
}
```

## 금상몰 백엔드가 구현해야 하는 검증 API

### `POST /api/internal/token-verifications/geumsangmall`

헤더:

```http
Content-Type: application/json
X-Token-Exchange-Secret: {sharedSecret}
```

요청:

```json
{
  "mallUserId": "mall-user-1",
  "mallSessionId": "session-1",
  "clientIp": "203.0.113.10",
  "userAgent": "Mozilla/5.0 ..."
}
```

성공 응답:

```json
{
  "valid": true,
  "mallUserId": "mall-user-1",
  "message": "ok"
}
```

실패 응답:

```json
{
  "valid": false,
  "mallUserId": "mall-user-1",
  "message": "invalid session"
}
```

## 금상몰 백엔드 검증 로직 권장사항

- `X-Token-Exchange-Secret` 검증
- `mallSessionId`가 현재 금상몰 로그인 세션인지 확인
- `mallUserId`와 세션의 실제 사용자 일치 여부 확인
- 필요 시 요청 IP, User-Agent도 함께 검증
- 세션이 만료되었거나 불일치면 `valid=false` 응답

## 운영 설정

`application.properties` 예시:

```properties
token.api.geumsangmall.exchange-enabled=true
token.api.geumsangmall.exchange-client-id=geumsangmall-front
token.api.geumsangmall.exchange-token-ttl-seconds=300
token.api.geumsangmall.verification-url=https://geumsangmall.example.com/api/internal/token-verifications/geumsangmall
token.api.geumsangmall.verification-secret=${GEUMSANGMALL_VERIFICATION_SECRET}
```

## 참고

- 금상몰 프론트는 `clientSecret`을 보관하지 않는다.
- 발급 토큰은 짧은 TTL로 사용한다.
- 장기적으로는 금상몰 백엔드가 우리 업무 API까지 대행 호출하는 구조가 더 안전하다.
