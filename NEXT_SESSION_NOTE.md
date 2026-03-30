# Next Session Note

다음 세션에서 이어볼 기준 메모입니다.

## 현재 기준 커밋

- `118c449` (`main`)

## 이번에 반영된 핵심

- `JWT + Redis` 기반 파트너 토큰 구조 적용
- 외부 사용자용 토큰 지원
  - `POST /api/external/token`
  - `userId` 필수
- 내부 서버용 토큰 지원
  - `POST /api/internal/token`
  - `systemName` 기반
- 관리자 클라이언트 등록 API 추가
  - `POST /api/admin/partner-clients`
- 보호 API 샘플 추가
  - `GET /api/external/ping`
  - `GET /api/internal/ping`
- `jjwt 0.11.5` 기준으로 구현
- 저장소는 DB 없이 Redis만 사용

## 주요 문서

- [README.md](/Users/mingulee/vscode/token-api-server/README.md)
- [COMPANY_INTEGRATION_GUIDE.md](/Users/mingulee/vscode/token-api-server/COMPANY_INTEGRATION_GUIDE.md)
- [token-api-server.collection.json](/Users/mingulee/vscode/token-api-server/postman/token-api-server.collection.json)

## 다음에 물어보면 좋은 문장

```text
token-api-server 프로젝트에서 JWT+Redis 파트너 토큰 작업 이어서 보자.
main 브랜치의 118c449 기준이야.
```

## 내일 우선 확인할 것

1. 회사 프로젝트의 `jjwt` 버전이 정말 `0.11.x`인지 다시 확인
2. 회사 프로젝트에 RedisTemplate/StringRedisTemplate이 이미 있는지 확인
3. 회사 프로젝트의 기존 인증 인터셉터/필터 체인과 충돌 없는지 확인
4. 외부 사용자 API와 내부 서버 API의 실제 URI 규칙이 같은지 확인
5. 운영용 `admin-secret`, `jwt-secret` 설정 위치 확인

## 예상 후속 작업

- 회사 프로젝트 패키지 구조에 맞게 클래스 이식
- 기존 인증 체계와 병행 여부 조정
- revoke API 필요 여부 판단
- clientSecret 해시 저장 전환 여부 판단
