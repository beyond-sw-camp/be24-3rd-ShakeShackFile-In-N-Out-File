# WaffleBear nGrinder 성능 테스트 위키

## 1. 개요

이 문서는 `ngrinder` 폴더의 Groovy 시나리오와 `C:/Users/Playdata/Desktop/성능테스트` 폴더의 결과 이미지를 기준으로 정리한 성능 테스트 위키다.

모든 시나리오는 공통적으로 관리자 계정으로 먼저 로그인한 뒤, 발급받은 Bearer 토큰을 붙여 각 기능 API를 호출한다. 즉, 실제 사용 흐름처럼 `로그인 -> 기능 호출` 순서로 테스트를 진행했다.

대부분의 결과 이미지는 `동시접속자 50명 (에이전트 2대)` 조건으로 수행되어 총 100 VUser로 측정되어 있다. 파일 업로드는 presigned URL 업로드와 완료/취소 절차까지 포함되어 가장 무거운 경로이고, 워크스페이스와 알림은 상대적으로 가벼운 조회/상태 변경 중심이다.

## 2. 파일 관련 시나리오

파일 관련 시나리오는 전체 테스트군 중 가장 무겁다. 특히 업로드 초기화, presigned PUT, 완료 처리, 취소 처리까지 이어지기 때문에 스토리지 연동 비용이 그대로 반영된다.

| 시나리오 | 테스트 내용 | 성능 결과 | 해석 |
| --- | --- | --- | --- |
| `WaffleBearFileScenario.groovy` | `POST /login`으로 인증 후 `GET /file/list` -> `POST /file/folder` -> `POST /file/upload` -> presigned `PUT` 업로드 -> `POST /file/upload/complete` -> `POST /file/upload/abort` -> `GET /file/list`를 순서대로 수행한다. | `100 VUser`, `TPS 21.7`, `최고 TPS 146.0`, `평균 테스트시간 2,320.46 ms`, `총 실행 테스트 5,028`, `성공 4,683`, `에러 345`, `동작 시간 00:03:45` | 파일 업로드와 취소가 섞인 가장 무거운 경로다. 평균 응답 시간이 2.3초대이고 에러도 6.86% 수준이라, 파일 처리와 스토리지 연동이 현재 병목임을 보여준다. |
| `WaffleBearFileAdvancedScenario.groovy` | `GET /file/list`와 폴더 생성, 업로드 완료 흐름, 업로드 취소 흐름을 각각 분리해서 테스트한다. | `100 VUser`, `TPS 23.4`, `최고 TPS 189.0`, `평균 테스트시간 2,377.45 ms`, `총 실행 테스트 5,004`, `성공 4,731`, `에러 273`, `동작 시간 00:03:30` | 기본 파일 시나리오보다 TPS는 약간 높지만, 평균 응답 시간은 비슷하게 길다. 업로드 경로 자체가 무겁기 때문에 시나리오를 분리해도 체감 병목은 크게 남는다. |

정리하면, 파일 관련 기능은 현재 측정된 항목 중 가장 느리고 불안정한 축이다. 평균 테스트시간이 2.3초대를 유지하고 에러도 수백 건 발생해서, 업로드 경로와 파일 저장소 연동을 우선적으로 점검할 필요가 있다.

## 3. 워크스페이스 시나리오

워크스페이스는 파일보다 가볍고, 대부분 0 에러로 안정적으로 동작했다. 특히 `by-uuid`, `isShared`, `delete`는 100 VUser 조건에서도 높은 TPS를 유지했다.

### 3-1. 이미지로 확인된 워크스페이스 테스트

| 시나리오 | 테스트 내용 | 성능 결과 | 해석 |
| --- | --- | --- | --- |
| `WaffleBearWorkspaceListScenario.groovy` | 로그인 후 `GET /workspace/list`만 반복 호출한다. | `100 VUser`, `TPS 99.4`, `최고 TPS 150.0`, `평균 테스트시간 974.35 ms`, `총 실행 테스트 5,174`, `성공 5,174`, `에러 0`, `동작 시간 00:00:59` | 워크스페이스 목록 조회는 안정적이지만, 다른 조회/상태 변경 API보다 약간 무겁다. 목록 응답이 여러 레코드를 포함하기 때문으로 보인다. |
| `WaffleBearWorkspaceByUuidScenario.groovy` | 워크스페이스를 먼저 생성한 뒤 `GET /workspace/by-uuid/{uuid}`로 조회하고 정리한다. | `100 VUser`, `TPS 144.1`, `최고 TPS 211.5`, `평균 테스트시간 555.69 ms`, `총 실행 테스트 5,194`, `성공 5,194`, `에러 0`, `동작 시간 00:00:42` | 워크스페이스 계열 중 가장 빠른 편이다. UUID 조회는 구조가 단순해서 처리량이 높고, 응답도 절반 초 수준으로 안정적이다. |
| `WaffleBearWorkspaceIsSharedScenario.groovy` | 워크스페이스 생성 후 `POST /workspace/isShared/{idx}`에 공개 전환 payload를 보내고 정리한다. | `100 VUser`, `TPS 140.7`, `최고 TPS 211.5`, `평균 테스트시간 557.24 ms`, `총 실행 테스트 5,070`, `성공 5,070`, `에러 0`, `동작 시간 00:00:42` | 공유 상태 변경은 가볍고 안정적이다. 조회류와 비슷한 처리량을 보여서 권한 플래그 변경 비용은 크지 않다. |
| `WaffleBearWorkspaceDeleteScenario.groovy` | 워크스페이스 생성 후 `POST /workspace/delete/{idx}`로 삭제만 수행한다. | `100 VUser`, `TPS 136.5`, `최고 TPS 209.0`, `평균 테스트시간 638.40 ms`, `총 실행 테스트 10,106`, `성공 10,106`, `에러 0`, `동작 시간 00:01:20` | 삭제는 쓰기 작업이지만 에러 없이 안정적으로 처리됐다. 반복 횟수가 100번이라 총 실행 테스트 수가 높지만, 처리량은 여전히 높게 유지된다. |

### 3-2. 코드상 존재하지만 현재 이미지가 없는 워크스페이스 스크립트

| 시나리오 | 코드상 테스트 흐름 | 비고 |
| --- | --- | --- |
| `WaffleBearWorkspaceReadScenario.groovy` | 워크스페이스 생성 후 `GET /workspace/read/{idx}`를 호출하고 정리한다. | 현재 성능 이미지 없음 |
| `WaffleBearWorkspaceLoadRoleScenario.groovy` | 워크스페이스 생성 후 `GET /workspace/loadRole/{idx}`를 호출하고 정리한다. | 현재 성능 이미지 없음 |
| `WaffleBearWorkspaceSaveRoleScenario.groovy` | 워크스페이스 생성 후 `POST /workspace/saveRole/{idx}`를 호출하고 정리한다. | 현재 성능 이미지 없음 |
| `WaffleBearWorkspaceScenario.groovy` | list/save/read/by-uuid/isShared/loadRole/saveRole/delete를 한 스크립트에 묶은 통합 시나리오다. | 성능 이미지 없음 |
| `WaffleBearWorkspaceAdvancedScenario.groovy` | list/save/delete, read/by-uuid, share/role 흐름을 분리한 확장 시나리오다. | 성능 이미지 없음 |

워크스페이스 계열은 이미지가 있는 항목만 봐도 성능 특성이 분명하다. 단순 조회나 상태 변경은 100 TPS 이상을 안정적으로 유지했고, 에러도 0이었다. 반면 파일과 연결된 작업은 훨씬 무겁게 나타났다.

### 3-3. 협업 연관 시나리오

| 시나리오 | 테스트 내용 | 성능 결과 | 해석 |
| --- | --- | --- | --- |
| `WaffleBearGroupChatScenario.groovy` | `GET /group/relationships` -> `GET /group/overview` -> `POST /group/groups` -> `DELETE /group/groups/{groupId}` -> `GET /chatRoom/list?page=0&size=5` 순서로 협업 관련 기능을 확인한다. | `100 VUser`, `TPS 314.7`, `최고 TPS 407.0`, `평균 테스트시간 305.52 ms`, `총 실행 테스트 5,044`, `성공 5,044`, `에러 0`, `동작 시간 00:00:23` | 전체 측정 이미지 중 가장 빠른 축이다. 조회, 생성, 삭제가 섞여 있는데도 평균 응답 시간이 300ms대라서 상대적으로 가벼운 협업 기능으로 보인다. |

코드상 `WaffleBearGroupChatAdvancedScenario.groovy`도 존재하지만 현재 이미지 폴더에는 결과가 없다. 이 스크립트는 relationships/overview, 그룹 생성/삭제, chatRoom 페이지네이션을 분리해서 확인하는 용도다.

## 4. 알림 시나리오

알림은 읽기 중심이라 파일보다 훨씬 가볍고, 워크스페이스 목록보다도 안정적으로 동작했다.

| 시나리오 | 테스트 내용 | 성능 결과 | 해석 |
| --- | --- | --- | --- |
| `WaffleBearNotificationScenario.groovy` | `GET /notification/list`를 기본 조회, 반복 조회, 응답 바디 파싱 검증까지 포함해 확인한다. | `100 VUser`, `TPS 118.6`, `최고 TPS 183.0`, `평균 테스트시간 819.04 ms`, `총 실행 테스트 5,222`, `성공 5,222`, `에러 0`, `동작 시간 00:00:51` | 알림 목록은 읽기 전용 성격이라 안정적으로 동작한다. 워크스페이스 목록보다 빠르고, 파일 계열보다는 훨씬 가볍다. |

정리하면 알림은 중간 정도의 처리량을 보이면서도 에러가 없어서, 현재 기준으로는 무난하고 안정적인 조회 API로 볼 수 있다.

## 참고

폴더에는 `회원가입 테스트 동시접속자 198명 (50번 반복) 만명 테스트.png` 이미지도 있었지만, 현재 `ngrinder` 스크립트와 직접 매칭되는 파일은 찾지 못했다. 별도 테스트로 보이기 때문에 참고값으로만 분리해 둔다.

| 항목 | 성능 결과 | 해석 |
| --- | --- | --- |
| 회원가입 테스트 | `198 VUser`, `TPS 42.8`, `최고 TPS 53.0`, `평균 테스트시간 4,577.08 ms`, `총 실행 테스트 10,109`, `성공 10,102`, `에러 7`, `동작 시간 00:04:09` | 가입 흐름은 다른 시나리오보다 훨씬 느리고, 평균 응답 시간이 4.5초대라서 이메일 인증이나 계정 생성 로직이 무거운 편으로 보인다. 에러는 적지만 0은 아니다. |

또한 `WaffleBearWorkspaceSupport.groovy`와 `common/WaffleBearNGrinderSupport.groovy`는 단독 성능 측정용 스크립트라기보다 공통 헬퍼 성격이 강하다. 이 파일들은 로그인, URL 생성, 토큰 처리, 워크스페이스 생성/삭제 같은 공통 로직을 재사용하기 위한 용도라서 결과 이미지가 별도로 없는 것이 자연스럽다.
