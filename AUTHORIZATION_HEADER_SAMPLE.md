# Authorization Header Sample

타 시스템이 `Authorization: Bearer {accessToken}`으로 호출했을 때 서버에서 토큰을 어떻게 꺼내고 쓰는지 정리합니다.

## 흐름

1. 인터셉터가 `Authorization` 헤더를 읽는다.
2. `Bearer ` 접두사를 제거해서 access token 문자열만 뽑는다.
3. `PartnerTokenService.authenticate()`로 JWT와 Redis 상태를 검증한다.
4. 검증 성공 시 `AuthenticatedPartnerToken`을 request attribute에 저장한다.
5. 컨트롤러나 서비스가 그 값을 꺼내서 `clientId`, `systemCode`, `callSource`, `scopes`를 사용한다.

## 실제 참고 파일

- 헤더 추출: `src/main/java/com/ruru/tokenapi/auth/PartnerApiTokenInterceptor.java`
- request attribute key: `src/main/java/com/ruru/tokenapi/auth/AuthContext.java`
- 인증 결과 객체: `src/main/java/com/ruru/tokenapi/auth/AuthenticatedPartnerToken.java`
- 토큰 검증: `src/main/java/com/ruru/tokenapi/partner/PartnerTokenService.java`
- 컨트롤러 사용 예시: `src/main/java/com/ruru/tokenapi/api/PartnerApiController.java`
- 인터셉터 등록: `src/main/java/com/ruru/tokenapi/config/WebConfig.java`

## 1. 헤더에서 Bearer 토큰 추출

실제 서버에서는 인터셉터에서 이렇게 처리합니다.

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    String token = extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
    if (token == null) {
        writeUnauthorized(response, "Missing Bearer token");
        return false;
    }

    AuthenticatedPartnerToken authenticatedToken = partnerTokenService.authenticate(token);
    if (authenticatedToken == null) {
        writeUnauthorized(response, "Invalid or expired token");
        return false;
    }

    request.setAttribute(AuthContext.REQUEST_ATTRIBUTE, authenticatedToken);
    return true;
}

private String extractBearerToken(String authorization) {
    if (authorization == null || authorization.isBlank()) {
        return null;
    }
    if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
        return null;
    }
    String token = authorization.substring(7).trim();
    return token.isBlank() ? null : token;
}
```

포인트:

- 헤더 이름은 `Authorization`
- 값 형식은 `Bearer {jwt}`
- `Bearer` 대소문자는 무시하고 처리 가능
- 토큰이 없거나 형식이 틀리면 바로 `401`

## 2. 컨트롤러에서 인증 정보 꺼내기

이미 검증이 끝난 뒤에는 컨트롤러에서 request attribute만 꺼내면 됩니다.

```java
@GetMapping("/internal/orders")
public Map<String, Object> getOrders(HttpServletRequest request) {
    AuthenticatedPartnerToken auth =
        (AuthenticatedPartnerToken) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);

    return Map.of(
        "clientId", auth.clientId(),
        "systemCode", auth.systemCode(),
        "callSource", auth.callSource(),
        "scopes", auth.scopes()
    );
}
```

이 방식이면 컨트롤러에서 JWT 파싱을 다시 하지 않아도 됩니다.

## 3. 서비스 레이어까지 넘기는 샘플

회사 프로젝트에 붙일 때는 보통 컨트롤러에서 필요한 값만 서비스로 넘기면 됩니다.

```java
@PostMapping("/internal/orders")
public ResponseEntity<OrderResponse> createOrder(HttpServletRequest request,
                                                 @RequestBody CreateOrderRequest body) {
    AuthenticatedPartnerToken auth =
        (AuthenticatedPartnerToken) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);

    OrderResponse response = orderService.createOrder(
        auth.clientId(),
        auth.systemCode(),
        auth.callSource(),
        body
    );
    return ResponseEntity.ok(response);
}
```

```java
public OrderResponse createOrder(String clientId,
                                 SystemCode systemCode,
                                 CallSource callSource,
                                 CreateOrderRequest request) {
    if (systemCode != SystemCode.GEUMSANGMALL) {
        throw new IllegalArgumentException("systemCode is not allowed");
    }

    return new OrderResponse(clientId, request.orderId(), "accepted");
}
```

## 4. 새 프로젝트에 옮길 때 최소 구현

필수 순서:

1. `HandlerInterceptor` 하나를 만든다.
2. `Authorization` 헤더에서 Bearer 토큰을 추출한다.
3. 공통 토큰 서비스로 검증한다.
4. 검증 성공 시 request attribute에 넣는다.
5. 보호 대상 API 경로에 인터셉터를 등록한다.

샘플:

```java
@Configuration
public class ApiSecurityConfig implements WebMvcConfigurer {
    private final PartnerApiTokenInterceptor partnerApiTokenInterceptor;

    public ApiSecurityConfig(PartnerApiTokenInterceptor partnerApiTokenInterceptor) {
        this.partnerApiTokenInterceptor = partnerApiTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(partnerApiTokenInterceptor)
            .addPathPatterns("/api/internal/**")
            .excludePathPatterns("/api/internal/token", "/api/internal/token/refresh");
    }
}
```

## 5. 타 시스템 호출 예시

호출 측은 access token을 헤더에만 붙이면 됩니다.

```bash
curl -s http://127.0.0.1:8090/api/internal/ping \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

## 운영 팁

- access token 원문은 로그에 남기지 않는 편이 안전합니다.
- `AuthenticatedPartnerToken` 전체를 비즈니스 로직 전역으로 넘기기보다 필요한 필드만 넘기는 편이 결합도가 낮습니다.
- 공통 API마다 `clientId`, `systemCode`, `callSource` 기준 권한 체크 규칙을 두는 편이 운영에 유리합니다.
