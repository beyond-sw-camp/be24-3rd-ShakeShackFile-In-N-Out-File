# nGrinder Scripts

This folder contains Groovy load-test scripts for the backend APIs.

## Layout

- `auth/`, `user/`, `administrator/`, `feater/`, `order/`, `notification/`, `group/`, `chatRoom/`, `chat/`, `workspace/`, `sse/`, `game/`
- `file/` is split by method, as requested:
  - `file/info/*`
  - `file/lock/*`
  - `file/manage/*`
  - `file/share/*`
  - `file/upload/*`

Each runnable Groovy file is self-contained:

- it logs in during `@BeforeThread`
- it stores the access token from the login response
- authenticated requests send `ATOKEN: Bearer <accessToken>`

## Shared properties

Set these as nGrinder parameters or JVM system properties:

- `baseUrl`
- `loginEmail`
- `loginPassword`
- `adminLoginEmail`
- `adminLoginPassword`
- `gameLoginEmail`
- `gameLoginPassword`

Optional endpoint-specific properties:

- `fileIdx`, `workspaceIdx`, `workspaceUuid`, `roomId`, `roomIdx`
- `targetUserIdx`, `relationshipId`, `groupId`, `inviteId`
- `assetId`, `assetIdx`, `messageIdx`
- `notificationTargetId`, `notificationUuid`, `paymentId`

## Notes

- `notification/delete.groovy` uses `java.net.http.HttpClient` because the nGrinder HTTP client does not expose a DELETE-with-body overload.
- `sse/connect.groovy` reads only a small number of bytes so the stream does not hang the test thread.
- The `*-ws.groovy` files are authenticated SockJS info probes that keep `ATOKEN` in play; a full STOMP/WebSocket harness is still needed for message-level tests.
