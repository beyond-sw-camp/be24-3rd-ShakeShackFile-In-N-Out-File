<div align="center">
<img src="./images/mainimage.png" alt="FileInNOut Main" />
</div>

<div align="center">
<h3>파일 관리, 실시간 문서 협업, 채팅을 하나로 연결한 통합 워크스페이스</h3>
<p>
<b>파일 저장소</b>, <b>실시간 문서 편집</b>, <b>협업 채팅</b>, <b>알림</b>을 하나의 서비스로 통합한 협업 플랫폼입니다.<br/>
업무 도구 간 전환 비용을 줄이고, 팀 단위 협업 흐름을 자연스럽게 이어주는 것을 목표로 합니다.
</p>
</div>

<div align="center">
<table border="0" width="80%">
<tr>
<td align="center" colspan="4">
<b>TEAM MEMBER</b>
</td>
</tr>
<tr>
<td align="center" width="25%">
<a href="https://github.com/Joohyeng">
<img src="https://github.com/Joohyeng.png" width="100"/>

김주형
</a>
</td>
<td align="center" width="25%">
<a href="https://github.com/Yoonjoon13">
<img src="https://github.com/Yoonjoon13.png" width="100"/>

범윤준
</a>
</td>
<td align="center" width="25%">
<a href="https://github.com/sunyeoplee0">
<img src="https://github.com/sunyeoplee0.png" width="100"/>

이선엽
</a>
</td>
<td align="center" width="25%">
<a href="https://github.com/Lumisia">
<img src="https://github.com/Lumisia.png" width="100"/>

최재원
</a>
</td>
</tr>
</table>
</div>

<div align="center">
<a href="#프로젝트-소개">소개</a> &nbsp;|&nbsp;
<a href="#기술-스택">기술 스택</a> &nbsp;|&nbsp;
<a href="#swagger-api-문서">Swagger</a> &nbsp;|&nbsp;
<a href="#시스템-아키텍처">아키텍처</a> &nbsp;|&nbsp;
<a href="#데이터베이스-설계">ERD</a> &nbsp;|&nbsp;
<a href="#서비스-도메인">도메인</a>
</div>

<hr/>

<h2 id="프로젝트-소개">프로젝트 소개</h2>

<div align="center">
<p><b>"전환 비용(Switching Cost)을 줄이는 협업 중심 통합 환경"</b></p>
<p>
FileInNOut은 클라우드 파일 저장소, 실시간 문서 협업, 채팅, 알림 기능을 하나의 흐름으로 연결한 워크스페이스입니다.<br/>
사용자는 웹상에서 파일을 관리하고, 문서를 공동 편집하며, 팀원과 실시간으로 소통할 수 있습니다.
</p>
</div>

<h3>Problem & Solution</h3>

<table width="100%">
<tr>
<td width="50%" valign="top">
<h3>Problem (Pain Points)</h3>
<ul>
<li><b>도구의 분리</b>: 파일 저장, 문서 작성, 협업 채팅이 각각 따로 존재해 업무 흐름이 끊어지는 문제</li>
<li><b>협업의 단절</b>: 문서 작업 중인 맥락과 커뮤니케이션이 분리되어 팀 협업 속도가 느려지는 문제</li>
<li><b>복잡한 공유 구조</b>: 파일, 채팅, 워크스페이스마다 공유/초대 흐름이 달라 관리가 어려운 문제</li>
</ul>
</td>
<td width="50%" valign="top">
<h3>Solution (Key Values)</h3>
<ul>
<li><b>통합 협업 공간</b>: 파일, 문서, 채팅, 초대, 알림을 하나의 서비스 안에서 연결</li>
<li><b>실시간 협업 환경</b>: WebSocket, SSE, Yjs 기반 실시간 상호작용 지원</li>
<li><b>유연한 공유 구조</b>: 개인, 그룹, 관계 기반으로 파일/채팅/워크스페이스 공유 가능</li>
</ul>
</td>
</tr>
</table>

<hr/>

<h2 id="기술-스택">기술 스택</h2>

<div align="center">
<h3>Backend & Storage</h3>
<img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
<img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
<img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security">
<img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT">
<img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white" alt="MariaDB">
<img src="https://img.shields.io/badge/MinIO-C72E49?style=for-the-badge&logo=minio&logoColor=white" alt="MinIO">

<h3>Frontend</h3>
<img src="https://img.shields.io/badge/Vue.js-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white" alt="Vue.js">
<img src="https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite">
<img src="https://img.shields.io/badge/Pinia-FFD859?style=for-the-badge&logo=pinia&logoColor=black" alt="Pinia">
<img src="https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white" alt="Axios">
<img src="https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" alt="Tailwind CSS">

<h3>Realtime & Collaboration</h3>
<img src="https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=websocket&logoColor=white" alt="WebSocket">
<img src="https://img.shields.io/badge/STOMP-2E7D32?style=for-the-badge" alt="STOMP">
<img src="https://img.shields.io/badge/SSE-1E88E5?style=for-the-badge" alt="SSE">
<img src="https://img.shields.io/badge/Yjs-FFB300?style=for-the-badge" alt="Yjs">
<img src="https://img.shields.io/badge/Editor.js-FF6B00?style=for-the-badge" alt="Editor.js">

<h3>Infrastructure</h3>
<img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white" alt="Nginx">
<img src="https://img.shields.io/badge/HAProxy-1E90FF?style=for-the-badge&logo=haproxy&logoColor=white" alt="HAProxy">
<img src="https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black" alt="Linux">
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
</div>

<hr/>

<h2 id="swagger-api-문서">Swagger API 문서</h2>
<details>
<summary>Swagger API 문서 상세 보기</summary>

<a href="https://api.fileinnout.kro.kr/swagger-ui/index.html" target="_blank"> 🚀 Swagger API 명세서 (Live)</a>
<p>백엔드 API 명세와 테스트는 Swagger UI에서 확인할 수 있습니다.</p>
</details>

<hr/>

<h2 id="시스템-아키텍처">시스템 아키텍처</h2>
<details>
<summary>시스템 아키텍처 상세 보기</summary>

<img src="./images/fileinnout.system_architecture.png" alt="FileInNOut System Architecture" width="1000"/>
</details>

<hr/>

<h2 id="데이터베이스-설계">데이터베이스 설계 (ERD)</h2>
<details>
<summary>데이터베이스 설계도(ERD) 상세 보기</summary>

<img src="/images/DB.png" alt="ERD" width="850"/>
</details>

<hr/>

<h2 id="서비스-도메인">서비스 도메인</h2>
<div align="left">
<a href="https://www.fileinnout.kro.kr" target="_blank" rel="noopener noreferrer">
<p>www.fileinnout.kro.kr</p>
</a>
</div>

<hr/>

<h2 id="서비스-시나리오">서비스 시나리오 및 기능 데모</h2>

<p>FileInNOut의 주요 기능을 시나리오별로 정리했습니다. 각 항목을 펼쳐 상세 흐름과 데모 화면을 확인할 수 있습니다.</p>

<details>
<summary>1. 사용자 인증 (회원가입 및 로그인)</summary>
<div align="center">
<img src="./Image/user_auth.gif" width="850" alt="user auth demo" />
<p>회원가입, 로그인, OAuth 기반 인증과 토큰 기반 세션 흐름을 지원합니다.</p>
</div>
</details>

<details>
<summary>2. 파일 및 문서 관리</summary>
<div align="center">
<img src="./Image/file_docs.gif" width="850" alt="file docs demo" />
<p>파일 트리 구조 안에서 폴더/파일을 관리하고, 문서(Page)를 생성해 연결된 작업 흐름을 제공합니다.</p>
</div>
</details>

<details>
<summary>3. 실시간 채팅</summary>
<div align="center">
<img src="./Image/chat.gif" width="850" alt="chat demo" />
<p>텍스트, 이미지, 파일 메시지를 기반으로 팀원과 실시간 소통할 수 있으며, 읽음 상태와 알림 흐름을 함께 제공합니다.</p>
</div>
</details>

<details>
<summary>4. 실시간 문서 편집기</summary>
<div align="center">
<img src="./Image/editor.gif" width="850" alt="editor demo" />
<p>블록 기반 에디터와 실시간 동시 편집 기능을 통해 협업 문서 작업을 지원합니다.</p>
</div>
</details>

<details>
<summary>5. 그룹, 초대, 공유</summary>
<div align="center">
<p>사용자 간 연결 요청, 그룹 생성, 그룹 초대, 파일/채팅/워크스페이스 공유 기능을 통해 협업 대상을 유연하게 구성할 수 있습니다.</p>
</div>
</details>

<details>
<summary>6. 알림 및 결제</summary>
<div align="center">
<p>실시간 알림으로 초대, 채팅, 요청 상태를 빠르게 확인할 수 있으며, PortOne 기반 결제를 통해 스토리지 확장 흐름을 제공합니다.</p>
</div>
</details>
