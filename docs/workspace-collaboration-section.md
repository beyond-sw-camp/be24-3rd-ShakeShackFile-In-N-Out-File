# 실시간 협업 워크스페이스

이 문서는 WaffleBear의 실시간 협업 워크스페이스 기능만 따로 정리한 설명입니다.
백엔드는 `PostController`, `PostService`, `WorkspaceAssetController`, `WorkspaceAssetService`, `WebSocketConfig`, `SseService`를 기준으로 보고, 프론트는 아래 파일들을 기준으로 분석했습니다.

- `C:\projects\frontend\be24-2nd-ShakeShackFile-In-N-Out-File\be24-2nd-ShakeShackFile-In-N-Out-File\src\components\workspace\editor.js`
- `C:\projects\frontend\be24-2nd-ShakeShackFile-In-N-Out-File\be24-2nd-ShakeShackFile-In-N-Out-File\src\views\workspace\WorkSpace.vue`
- `C:\projects\frontend\be24-2nd-ShakeShackFile-In-N-Out-File\be24-2nd-ShakeShackFile-In-N-Out-File\src\views\workspace\WorkSpaceReadOnly.vue`
- `C:\projects\frontend\be24-2nd-ShakeShackFile-In-N-Out-File\be24-2nd-ShakeShackFile-In-N-Out-File\src\api\postApi.js`

## 1. 개요

이 스크립트는 단순한 문서 편집 화면이 아니라, 문서 본문, 파일 자산, 권한, 실시간 커서, 알림을 하나의 워크스페이스 안에서 함께 다루는 협업 화면입니다.

백엔드에서는 워크스페이스 자체를 `Post` 엔티티로 관리하고, 사용자와 워크스페이스의 관계를 `UserPost`로 연결합니다. 이때 `AccessRole`은 `READ`, `WRITE`, `ADMIN`으로 나뉘어 권한을 세분화합니다.

프론트에서는 `WorkSpace.vue`가 편집 진입점 역할을 하고, `editor.js`가 실제 EditorJS + Yjs 협업 로직을 담당합니다. 읽기 전용 화면은 `WorkSpaceReadOnly.vue`가 맡아서 같은 워크스페이스 데이터를 보기 전용으로 렌더링합니다.

이 구조의 핵심은 다음과 같습니다.

- 문서 내용은 EditorJS JSON으로 저장한다.
- 여러 사용자의 동시 편집 상태는 Yjs가 관리한다.
- 첨부 파일과 이미지 같은 바이너리는 워크스페이스 자산 API로 분리한다.
- 권한 변경과 강퇴는 SSE와 STOMP로 실시간 반영한다.

필요한 이미지 예시:

- 워크스페이스 편집 메인 화면 전체 캡처
- 읽기 전용 화면 캡처
- 왼쪽/오른쪽 자산 패널이 보이는 화면 캡처

## 2. 워크스페이스 상세 기능

### 2-1. 워크스페이스 생성 및 조회

백엔드의 `POST /workspace/save`는 워크스페이스를 생성하거나 기존 문서를 갱신합니다. 프론트의 `WorkSpace.vue`에서는 저장이 아직 안 된 상태라면 먼저 `savePost()`를 호출해서 워크스페이스 ID를 확보한 뒤, 이후 자산 업로드나 협업 연결을 이어갑니다.

`GET /workspace/read/{idx}`는 워크스페이스의 내용을 읽는 기본 진입점이고, `GET /workspace/by-uuid/{uuid}`는 초대 링크나 UUID 기반 접근을 위한 진입점입니다. 프론트의 `checkAndRedirectUuid()`는 초대 URL로 들어왔을 때 UUID를 먼저 조회한 뒤, 실제 워크스페이스 ID로 라우팅을 바꿔줍니다.

### 2-2. 권한 제어

백엔드에서는 `PostService.read()`가 현재 사용자와 워크스페이스의 관계를 확인하고, `PostService.resolveByUuid()`가 UUID로 들어온 사용자의 접근 권한을 결정합니다. 공개 워크스페이스의 경우, 아직 멤버가 아니면 관계를 생성해 `WRITE` 권한으로 연결할 수도 있습니다.

프론트에서는 `workspaceAccessRole`을 기준으로 UI를 제어합니다.

- `READ`면 편집이 아닌 읽기 전용 화면으로 이동한다.
- `ADMIN`과 `WRITE`는 편집과 파일 관리가 가능하다.
- 자산 삭제나 드라이브 저장 같은 기능도 권한에 따라 버튼이 비활성화된다.

### 2-3. 실시간 커서와 사용자 상태

`src/components/workspace/editor.js`는 Yjs의 `awareness`를 사용해서 현재 접속자 정보를 공유합니다. 로컬 사용자의 이름, 색상, 역할, 사용자 인덱스를 local state로 넣고, 마우스 위치는 `setLocalStateField('mouse', ...)`로 계속 전송합니다.

`WorkSpace.vue`는 이 상태를 받아서 `remoteCursors`를 화면 위에 오버레이합니다. 덕분에 다른 사용자의 커서가 색상과 이름이 붙은 형태로 보입니다.

또한 `activeUsersRef`를 통해 현재 워크스페이스 참여자 목록도 함께 표시할 수 있습니다.

### 2-4. 제목 및 본문 실시간 동기화

문서 제목은 Yjs의 `yTitle`에 저장되고, 본문은 `yMap.get('contents')`에 저장됩니다.

- `title`이 바뀌면 `updateTitleFromLocal()`가 Yjs 문서에 반영한다.
- `yTitle.observe()`는 다른 사용자의 제목 변경을 로컬 상태에 다시 반영한다.
- 본문은 `yMap.observe()`를 통해 들어온 새 내용을 다시 렌더링한다.

즉, 제목과 본문이 따로 관리되면서도 하나의 협업 문서처럼 동작합니다.

### 2-5. 파일 업로드와 워크스페이스 자산

워크스페이스의 파일은 일반 문서 내용과 분리해서 관리합니다. 백엔드의 `WorkspaceAssetController`와 `WorkspaceAssetService`가 담당하는 영역입니다.

주요 흐름은 다음과 같습니다.

- `GET /workspace/{workspaceId}/assets`로 자산 목록을 가져온다.
- `POST /workspace/{workspaceId}/assets`로 일반 파일을 업로드한다.
- `POST /workspace/{workspaceId}/assets/editorjs`로 EditorJS 이미지 업로드를 처리한다.
- `DELETE /workspace/{workspaceId}/assets/{assetId}`로 자산을 삭제한다.
- `POST /workspace/{workspaceId}/assets/{assetId}/save-to-drive`로 워크스페이스 자산을 개인 드라이브로 복사한다.

`WorkSpace.vue`에서는 업로드 후 바로 `refreshWorkspaceAssets()`를 호출하고, STOMP 구독을 통해 다른 사용자가 올린 파일도 즉시 목록에 반영합니다.

`WorkspaceAssetService`는 업로드된 파일을 MinIO에 저장하고, 저장된 메타데이터를 DB에 기록한 뒤, `/sub/workspace/assets/{workspaceIdx}`로 이벤트를 발행합니다.

### 2-6. 실시간 자산 반영

백엔드의 `WorkspaceAssetService.publishAssetEvent()`는 `UPLOAD`와 `DELETE` 이벤트를 STOMP 브로커로 보냅니다. 프론트의 `connectWorkspaceAssetRealtime()`는 `/sub/workspace/assets/{workspaceId}`를 구독해서 자산 목록을 즉시 갱신합니다.

이 방식의 장점은 다음과 같습니다.

- 새로고침 없이 파일 목록이 바뀐다.
- 한 사람이 업로드하거나 삭제한 결과가 다른 사용자 화면에도 바로 보인다.
- 편집 문서와 첨부 자산의 상태가 어긋나지 않는다.

### 2-7. 권한 변경과 강퇴

`PostController`에는 `saveRole`, `changeSingleRole`, `kickMember`, `isShared`, `loadRole`, `invite` 같은 권한 관련 API가 있습니다. 프론트의 `WorkSpace.vue`는 SSE의 `role-changed` 이벤트를 감지해 역할이 바뀌면 화면을 새로고침하거나, 강퇴되면 워크스페이스 목록으로 되돌립니다.

필요한 이미지 예시:

- 실시간 커서가 보이는 협업 편집 화면
- 자산 목록이 STOMP로 즉시 갱신되는 화면
- 권한 드롭다운 또는 멤버 관리 모달 화면

## 3. 왜 Yjs 라이브러리를 쓴 이유

Yjs는 단순한 "실시간 동기화 도구"가 아니라, 협업 편집에 필요한 충돌 해결과 상태 공유를 잘 묶어주는 CRDT 기반 라이브러리입니다.

이 프로젝트에서 Yjs를 선택한 이유는 다음과 같이 정리할 수 있습니다.

- 여러 사용자가 동시에 같은 문서를 수정해도 충돌 처리를 안정적으로 할 수 있다.
- 중앙 서버가 편집 순서를 강하게 강제하지 않아도 된다.
- 네트워크가 불안정해도 로컬 편집 내용을 보존한 뒤 다시 동기화할 수 있다.
- 문서 상태와 "누가 지금 보고 있는가" 같은 존재감 정보는 분리해서 다룰 수 있다.
- `WebsocketProvider`로 연결하면 구현이 비교적 단순하면서도 협업 기능을 빠르게 붙일 수 있다.

특히 이 프로젝트의 워크스페이스는 "문서 내용"과 "접속자 상태"를 분리하는 구조가 잘 맞습니다. 문서 본문은 `yMap`과 `yTitle`에, 커서와 사용자 목록은 `awareness`에 두었기 때문에 코드 구조가 깔끔해집니다.

필요한 이미지 예시:

- Yjs와 awareness 흐름을 보여주는 구조도
- 동시 편집 시 충돌이 병합되는 예시 다이어그램

## 4. Yjs 알고리즘 특장점

Yjs의 핵심은 CRDT 방식입니다. 즉, 여러 클라이언트가 독립적으로 변경해도 최종적으로 같은 상태로 수렴하도록 설계되어 있습니다.

특장점은 아래와 같습니다.

- 동시 수정 충돌을 자동으로 병합한다.
- 서버가 모든 편집 순서를 한 줄로 정렬하지 않아도 된다.
- 변경 메시지가 교환되는 방식이라 오프라인 후 재연결에도 강하다.
- 문서의 일부만 바뀌어도 전체를 다시 보내지 않고 필요한 변경만 전달할 수 있다.
- 업데이트는 합쳐도 결과가 안정적이라 협업 편집에 적합하다.

이 프로젝트에서는 특히 아래 부분이 중요합니다.

- 문서 본문은 빈번하게 바뀌지만, 사용자가 체감하는 반응 속도는 빨라야 한다.
- 여러 명이 동시에 제목과 본문을 수정해도 데이터를 잃지 않아야 한다.
- 읽기/쓰기 권한이 다르더라도 같은 문서 구조를 공유해야 한다.

결국 Yjs는 "실시간"과 "안정성"을 동시에 챙길 수 있는 선택입니다.

필요한 이미지 예시:

- 동시 편집 전/후 비교 그림
- 로컬 편집 후 재연결 시 동기화 흐름 그림

## 5. 파일 업로드 및 EditorJS 쓴 이유

### 5-1. 파일 업로드를 분리한 이유

워크스페이스에서는 문서 본문에 이미지나 파일이 들어갈 수 있지만, 실제 바이너리는 본문 JSON 안에 넣지 않고 별도 자산으로 저장합니다.

이 방식의 장점은 다음과 같습니다.

- 문서 내용과 파일 저장을 분리할 수 있다.
- 이미지/파일을 재사용하거나 드라이브로 옮기기 쉽다.
- 대용량 파일을 문서 JSON에 섞지 않아도 된다.
- 삭제, 이동, 공유 같은 파일 관리 기능을 별도로 설계할 수 있다.

백엔드는 MinIO를 사용해 업로드 파일을 저장하고, 자산 메타데이터는 DB에 남깁니다. 프론트는 업로드 후 반환된 `assetIdx`, `downloadUrl`, `previewUrl` 등을 사용해서 화면에 바로 보여줍니다.

### 5-2. EditorJS를 쓴 이유

EditorJS는 블록 기반 에디터라서 워크스페이스 같은 문서형 협업 화면에 잘 맞습니다.

선택 이유는 다음과 같습니다.

- 제목, 문단, 목록, 인용, 표, 코드, 이미지처럼 블록 단위로 구조가 분명하다.
- JSON으로 저장되기 때문에 서버 저장과 복원이 쉽다.
- 읽기 전용 화면에서 같은 JSON을 HTML로 다시 렌더링하기 좋다.
- 이미지 업로드 훅을 커스터마이징해서 워크스페이스 자산 API와 자연스럽게 연결할 수 있다.

`editor.js`에서는 EditorJS의 `image` 툴을 커스텀 업로더로 바꿔서, 이미지가 올라오면 곧바로 `postApi.uploadEditorJsImage()`를 호출합니다. 또한 사용자가 문서에서 이미지를 지우면 `deleteEditorJsImage()`로 서버 자산도 함께 정리합니다.

이 덕분에 EditorJS 화면과 서버 자산 상태가 엇갈리지 않습니다.

### 5-3. 읽기 전용 화면과의 호환성

`WorkSpaceReadOnly.vue`는 EditorJS JSON을 블록 단위로 다시 HTML로 렌더링합니다. 즉, 작성 화면에서 저장한 구조가 읽기 전용 화면에서도 거의 그대로 재사용됩니다.

이 구조는 협업 문서 시스템에서 매우 유리합니다.

- 작성 화면과 조회 화면의 데이터 형식이 같다.
- 편집기 자체가 아닌 JSON 블록을 중심으로 관리할 수 있다.
- 나중에 블록 타입이 늘어나도 렌더러만 확장하면 된다.

필요한 이미지 예시:

- EditorJS 편집 화면 캡처
- 이미지 업로드 후 본문에 삽입된 모습
- 읽기 전용 렌더링 화면 캡처

## 6. 마무리

WaffleBear의 실시간 협업 워크스페이스는 단순한 게시글 편집 기능이 아니라, 아래 요소가 함께 동작하는 협업 플랫폼입니다.

- Yjs 기반 실시간 동기화
- EditorJS 기반 블록 에디터
- MinIO 기반 파일 저장
- STOMP 기반 자산 실시간 갱신
- SSE 기반 권한 변경 알림
- `READ`, `WRITE`, `ADMIN`으로 나뉜 권한 제어

이 조합 덕분에 문서 작성, 파일 업로드, 커서 공유, 권한 관리, 읽기 전용 조회까지 하나의 흐름으로 연결됩니다.

정리하면 이 워크스페이스는 "문서가 먼저가 아니라 협업 경험이 먼저인 구조"로 설계되어 있고, 그 중심에 Yjs와 EditorJS가 배치되어 있습니다.

필요한 이미지 예시:

- 전체 아키텍처 요약 다이어그램
- 워크스페이스 협업 흐름도
- 권한/파일/커서가 함께 보이는 최종 화면
