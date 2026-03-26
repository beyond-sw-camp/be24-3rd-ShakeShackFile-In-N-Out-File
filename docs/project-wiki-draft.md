# WaffleBear 프로젝트 위키 초안

이 문서는 현재 WaffleBear 백엔드 프로젝트 기준으로 정리한 위키 초안입니다.  
각 섹션은 `기능 설명 -> 핵심 흐름 -> 관련 컴포넌트 -> 필요한 이미지` 순서로 읽을 수 있게 구성했습니다.

## 목차

1. [위키 개요](#1-위키-개요)
2. [보안 및 인증](#2-보안-및-인증)
3. [파이프 라인](#3-파이프-라인)
4. [관리자 계정 및 유저간의 권한](#4-관리자-계정-및-유저간의-권한)
5. [파일 드라이브](#5-파일-드라이브)
6. [실시간 채팅](#6-실시간-채팅)
7. [실시간 협업 워크 스페이스](#7-실시간-협업-워크-스페이스)
8. [구독 결제](#8-구독-결제)
9. [모니터링 및 Exception 설정](#9-모니터링-및-exception-설정)
10. [기타](#10-기타)

## 1. 위키 개요

WaffleBear는 파일 드라이브, 실시간 채팅, 협업 워크스페이스, 권한 관리, 구독 결제, 관리자 대시보드까지 포함한 협업형 백엔드 서비스입니다.  
이 위키는 기능별로 구조를 나누고, 각 기능이 어떤 요청 흐름으로 동작하는지 한눈에 확인할 수 있도록 정리하는 것을 목표로 합니다.

### 이 문서에서 다루는 범위

- 로그인과 토큰 기반 인증
- 관리자 전용 기능과 일반 사용자 기능의 분리
- 파일 업로드, 폴더 관리, 공유, 휴지통, 잠금
- 실시간 채팅과 SSE/WebSocket 기반 알림
- 워크스페이스 생성, 초대, 역할 관리, 자산 첨부
- 구독 결제와 저장소 용량 플랜
- 예외 처리와 모니터링

### 필요한 이미지

- 프로젝트 전체 기능 맵
- 메인 화면 또는 서비스 랜딩 화면
- 로그인 이후 홈 화면

## 2. 보안 및 인증

로그인과 인증은 JWT 기반으로 동작하고, 리프레시 토큰은 쿠키로 관리합니다.  
`LoginFilter`가 로그인 요청을 처리하고, `JwtFilter`가 이후 요청에서 `Authorization: Bearer <token>`을 검증합니다.

### 핵심 흐름

1. 사용자가 `POST /login`으로 이메일과 비밀번호를 전송합니다.
2. `LoginFilter`가 인증을 수행하고 access token / refresh token을 발급합니다.
3. access token은 응답 헤더와 바디에 전달되고, refresh token은 `refresh` 쿠키로 내려갑니다.
4. 이후 요청은 `JwtFilter`가 토큰을 검증한 뒤 사용자 정보를 `AuthUserDetails`로 주입합니다.
5. 토큰 만료 시 `POST /auth/reissue`로 재발급할 수 있습니다.
6. 로그아웃은 `POST /auth/logout`으로 refresh 토큰을 제거합니다.

### 관련 컴포넌트

- `src/main/java/com/example/WaffleBear/config/Filter/LoginFilter.java`
- `src/main/java/com/example/WaffleBear/config/Filter/JwtFilter.java`
- `src/main/java/com/example/WaffleBear/user/controller/AuthController.java`
- `src/main/java/com/example/WaffleBear/user/controller/UserController.java`
- `src/main/java/com/example/WaffleBear/config/SecurityConfig.java`
- `src/main/java/com/example/WaffleBear/utils/JwtUtil.java`

### 문서에 넣으면 좋은 이미지

- 로그인 요청/응답 예시
- 브라우저 개발자 도구의 Authorization 헤더 캡처
- refresh 쿠키가 저장된 화면
- `SecurityConfig`에서 허용 경로와 보호 경로를 보여주는 설정 캡처

## 3. 파이프 라인

이 섹션은 코드 배포와 검증 흐름을 설명하는 곳입니다.  
현재 프로젝트는 Spring Boot 기반이므로, 일반적으로 아래 순서로 설명하면 읽기 좋습니다.

### 권장 서술 구조

1. 개발 브랜치에 코드가 반영됩니다.
2. CI에서 빌드와 테스트가 실행됩니다.
3. 아티팩트 또는 Docker 이미지를 생성합니다.
4. 배포 서버에 반영합니다.
5. 운영 환경에서 헬스체크와 검증 요청을 수행합니다.
6. 장애 시 로그와 모니터링 대시보드로 원인을 확인합니다.

### 현재 프로젝트 기준으로 함께 적으면 좋은 내용

- Spring Boot 3.x, Java 17 기반 빌드
- Gradle 기반 의존성 관리
- nGrinder로 주요 API에 대한 부하/성능 검증 가능
- 배포 후에는 로그인, 파일 업로드, 채팅, 워크스페이스, 결제 흐름을 smoke test로 점검

### 필요한 이미지

- CI/CD 파이프라인 다이어그램
- 빌드 성공 화면
- 배포 후 헬스체크 화면
- nGrinder 실행 결과 요약 화면

> 참고: 저장소에 별도의 GitHub Actions 워크플로 파일이 보이지 않는다면, 이 섹션은 실제 운영 방식에 맞춰 업데이트하면 됩니다.

## 4. 관리자 계정 및 유저간의 권한

관리자 기능은 일반 사용자 기능과 분리되어 있고, 계정 상태와 역할에 따라 접근이 제한됩니다.  
관리자 전용 API는 대시보드, 사용자 상태 변경, 저장소 사용량 분석, 저장 용량 조정 같은 운영 기능을 담당합니다.

### 핵심 흐름

1. 관리자는 일반 로그인과 같은 방식으로 인증합니다.
2. JWT 필터가 역할 정보를 읽어 권한을 주입합니다.
3. 관리자 전용 API에서 관리자 여부를 확인합니다.
4. 일반 사용자는 접근할 수 없는 대시보드와 저장소 분석 API를 제공합니다.
5. 사용자 상태를 변경해 서비스 접근을 제어할 수 있습니다.

### 관련 컴포넌트

- `src/main/java/com/example/WaffleBear/administrator/AdministratorController.java`
- `src/main/java/com/example/WaffleBear/administrator/AdministratorService.java`
- `src/main/java/com/example/WaffleBear/administrator/StorageAnalyticsService.java`
- `src/main/java/com/example/WaffleBear/user/model/AuthUserDetails.java`
- `src/main/java/com/example/WaffleBear/config/Filter/JwtFilter.java`

### 현재 프로젝트에서 적합한 포인트

- 관리자 대시보드
- 유저 계정 활성/비활성 제어
- 저장소 사용량 분석
- 저장소 총량 조정

### 필요한 이미지

- 관리자 대시보드 화면
- 사용자 상태 변경 화면
- 저장소 사용량 분석 그래프
- 사용자별 저장량 테이블

## 5. 파일 드라이브

파일 드라이브는 파일 목록, 폴더 생성, 업로드, 공유, 휴지통, 복구, 삭제를 담당합니다.  
업로드는 초기화 요청과 실제 저장 요청이 분리되어 있고, 대용량 파일은 분할 업로드 방식으로 처리합니다.

### 핵심 흐름

1. 로그인 후 파일 목록을 조회합니다.
2. 새 폴더를 생성하거나 기존 폴더를 탐색합니다.
3. 파일 업로드 초기화 요청으로 저장 경로와 프리사인 URL을 받습니다.
4. 클라이언트는 프리사인 URL로 실제 파일을 업로드합니다.
5. 업로드 완료 API로 메타데이터 저장을 마무리합니다.
6. 공유, 이동, 이름 변경, 휴지통 이동, 복구, 영구 삭제를 수행합니다.

### 관련 컴포넌트

- `src/main/java/com/example/WaffleBear/file/manage/FileManageController.java`
- `src/main/java/com/example/WaffleBear/file/upload/UploadController.java`
- `src/main/java/com/example/WaffleBear/file/info/FileInfoController.java`
- `src/main/java/com/example/WaffleBear/file/share/ShareController.java`
- `src/main/java/com/example/WaffleBear/file/lock/LockController.java`
- `src/main/java/com/example/WaffleBear/file/service/StoragePlanService.java`

### 현재 프로젝트에서 다루기 좋은 기능

- 파일 목록
- 폴더 생성
- 휴지통 이동 및 복구
- 파일 공유 목록
- 파일 상세 정보 및 텍스트 미리보기
- 잠금 상태 관리

### 필요한 이미지

- 파일 드라이브 트리 화면
- 폴더 생성 팝업
- 업로드 진행 상태 화면
- 공유 링크 생성 화면
- 휴지통/복구 화면
- 파일 상세 정보 화면

## 6. 실시간 채팅

채팅은 채팅방 생성, 초대, 입장, 퇴장, 메시지 조회, 읽음 처리, 파일 첨부를 중심으로 동작합니다.  
WebSocket과 SSE가 함께 사용되어 실시간 상태 갱신이 이뤄집니다.

### 핵심 흐름

1. 사용자가 채팅방을 생성합니다.
2. 다른 사용자를 이메일로 초대합니다.
3. 채팅방 목록을 조회하고 입장/퇴장을 수행합니다.
4. 메시지를 보내고 히스토리를 조회합니다.
5. 읽음 상태와 미리보기 업데이트가 실시간으로 반영됩니다.

### 관련 컴포넌트

- `src/main/java/com/example/WaffleBear/chat/ChatRoomController.java`
- `src/main/java/com/example/WaffleBear/chat/ChatMessageController.java`
- `src/main/java/com/example/WaffleBear/config/WebSocketConfig.java`
- `src/main/java/com/example/WaffleBear/config/sse/SseController.java`
- `src/main/java/com/example/WaffleBear/config/sse/SseService.java`

### 필요한 이미지

- 채팅방 리스트 화면
- 채팅방 생성 팝업
- 사용자 초대 화면
- 실시간 메시지 화면
- 읽음 표시가 보이는 채팅 화면

## 7. 실시간 협업 워크 스페이스

워크스페이스는 협업 문서/보드의 중심 기능입니다.  
워크스페이스 생성 후 UUID로 조회하고, 공개 여부와 역할을 관리하며, 워크스페이스 자산도 함께 다룰 수 있습니다.

### 핵심 흐름

1. 사용자가 워크스페이스를 생성합니다.
2. `idx` 또는 `uuid`로 워크스페이스를 조회합니다.
3. 공개 상태를 바꾸고, 역할 정보를 불러오거나 저장합니다.
4. 사용자 초대와 권한 부여를 통해 협업 멤버를 관리합니다.
5. 워크스페이스에 첨부된 자산을 업로드하거나 드라이브로 저장합니다.

### 관련 컴포넌트

- `src/main/java/com/example/WaffleBear/workspace/controller/PostController.java`
- `src/main/java/com/example/WaffleBear/workspace/asset/WorkspaceAssetController.java`
- `src/main/java/com/example/WaffleBear/workspace/service/PostService.java`
- `src/main/java/com/example/WaffleBear/workspace/asset/WorkspaceAssetService.java`
- `src/main/java/com/example/WaffleBear/config/sse/SseService.java`

### 현재 프로젝트 기준으로 적으면 좋은 포인트

- `POST /workspace/save`
- `GET /workspace/read/{idx}`
- `GET /workspace/by-uuid/{uuid}`
- `POST /workspace/isShared/{idx}`
- `GET /workspace/loadRole/{idx}`
- `POST /workspace/saveRole/{idx}`
- `POST /workspace/invite`
- 워크스페이스 자산 업로드/삭제/드라이브 저장

### 필요한 이미지

- 워크스페이스 목록 화면
- 워크스페이스 편집 화면
- UUID 기반 접근 화면
- 공개/비공개 설정 화면
- 역할 편집 화면
- 워크스페이스 자산 업로드 화면

## 8. 구독 결제

구독 결제는 저장소 용량 플랜과 연결되어 있습니다.  
결제 생성 후 검증을 완료해야 저장 용량이 실제로 반영되고, 만료일과 상품 정보도 함께 관리됩니다.

### 핵심 흐름

1. 사용자가 상품 또는 플랜을 선택합니다.
2. 주문 생성 API가 결제 주문 정보를 만듭니다.
3. 결제 완료 후 검증 API가 외부 결제 정보를 확인합니다.
4. 검증 성공 시 저장소 플랜과 만료일이 반영됩니다.
5. 파일 업로드 용량 제한이 구독 상태에 따라 달라집니다.

### 관련 컴포넌트

- `src/main/java/com/example/WaffleBear/order/controller/OrderController.java`
- `src/main/java/com/example/WaffleBear/order/service/OrderService.java`
- `src/main/java/com/example/WaffleBear/config/PortoneConfig.java`
- `src/main/java/com/example/WaffleBear/file/service/StoragePlanService.java`

### 현재 프로젝트에서 다루기 좋은 포인트

- 주문 생성
- 결제 검증
- 플랜 만료일
- 저장 용량 증가
- 사용자별 플랜 상태

### 필요한 이미지

- 상품/플랜 선택 화면
- 결제 진행 화면
- 결제 완료 결과 화면
- 구독 상태가 반영된 저장소 용량 화면

## 9. 모니터링 및 Exception 설정

예외 처리와 모니터링은 서비스 안정성 설명에 꼭 들어가면 좋습니다.  
현재 프로젝트는 전역 예외 처리기와 Actuator/Micrometer 기반 추적 구성을 함께 사용할 수 있습니다.

### 핵심 흐름

1. 요청 처리 중 검증 실패나 비즈니스 오류가 발생하면 전역 예외 처리기가 응답을 표준화합니다.
2. 잘못된 입력은 400 계열, 서버 내부 오류는 500 계열로 반환됩니다.
3. Actuator와 tracing 설정을 통해 운영 상태를 확인할 수 있습니다.
4. 저장소 사용량, 워크스페이스 사용량, 채팅 사용량은 관리자 분석 화면에서 확인할 수 있습니다.

### 관련 컴포넌트

- `src/main/java/com/example/WaffleBear/common/exception/GlobalExceptionHandler.java`
- `src/main/resources/application.yml`
- `src/main/java/com/example/WaffleBear/administrator/StorageAnalyticsService.java`
- `src/main/java/com/example/WaffleBear/administrator/AdministratorController.java`

### 문서에 넣으면 좋은 이미지

- 예외 응답 JSON 예시
- 관리자 저장소 분석 화면
- Actuator health 화면
- tracing/monitoring 대시보드 화면

### 권장 문구

- "입력값 검증 실패 시 통일된 에러 응답을 반환합니다."
- "서비스 오류는 전역 예외 처리기로 수집합니다."
- "운영자는 저장소 사용량과 자산 사용량을 대시보드에서 확인합니다."

## 10. 기타

이 섹션에는 위 목차에 직접 들어가지 않지만 프로젝트 이해에 도움이 되는 기능을 정리하면 좋습니다.

### 포함하면 좋은 항목

- 이메일 인증과 초대 메일
- OAuth2 로그인
- 프로필 및 설정 기능
- 미니 게임 또는 부가 기능
- Swagger/OpenAPI 문서
- WebSocket/SSE 연결 설정

### 현재 프로젝트 기준 예시

- `src/main/java/com/example/WaffleBear/email/EmailVerifyService.java`
- `src/main/java/com/example/WaffleBear/feater/FeaterController.java`
- `src/main/java/com/example/WaffleBear/config/SwaggerConfig.java`
- `src/main/java/com/example/WaffleBear/legup/*`

### 필요한 이미지

- 이메일 인증 화면
- 프로필 설정 화면
- Swagger UI 화면
- 부가 기능 화면 또는 데모 캡처

## 문서 작성 팁

- 각 섹션은 `개요 -> 핵심 흐름 -> 주요 컴포넌트 -> 이미지` 순서로 통일하면 읽기 쉽습니다.
- 이미지가 없을 때는 `TODO: 로그인 화면 캡처`, `TODO: 대시보드 캡처`처럼 자리만 잡아두면 나중에 채우기 편합니다.
- API 중심 설명은 엔드포인트 이름을 그대로 쓰면 유지보수가 쉽습니다.
- 운영 문서는 사용자 화면 캡처와 서버 구성 캡처를 함께 넣으면 이해가 빨라집니다.
