# nGrinder Scripts for WaffleBear

This folder contains standalone nGrinder Groovy scripts for WaffleBear APIs.

## Folder structure

- `workspace/`
  - `WaffleBearWorkspaceScenario.groovy` (all workspace endpoints in one script)
  - `WaffleBearWorkspaceListScenario.groovy`
  - `WaffleBearWorkspaceSaveScenario.groovy`
  - `WaffleBearWorkspaceReadScenario.groovy`
  - `WaffleBearWorkspaceByUuidScenario.groovy`
  - `WaffleBearWorkspaceIsSharedScenario.groovy`
  - `WaffleBearWorkspaceLoadRoleScenario.groovy`
  - `WaffleBearWorkspaceSaveRoleScenario.groovy`
  - `WaffleBearWorkspaceDeleteScenario.groovy`
  - `WaffleBearWorkspaceAdvancedScenario.groovy` (extra multi-method performance tests)
  - `WaffleBearWorkspaceSupport.groovy` (legacy helper)
- `file/`
  - `WaffleBearFileScenario.groovy`
  - `WaffleBearFileAdvancedScenario.groovy` (extra multi-method performance tests)
- `group/`
  - `WaffleBearGroupChatScenario.groovy`
  - `WaffleBearGroupChatAdvancedScenario.groovy` (extra multi-method performance tests)
- `notification/`
  - `WaffleBearNotificationScenario.groovy`
- `common/`
  - `WaffleBearNGrinderSupport.groovy` (legacy helper)

## Common behavior (all scenario scripts)

- Each test iteration logs in first (`/login`, fallback `/api/login`), then runs endpoint tests with Bearer token.
- Default admin account is embedded:
  - `administrator@administrator.adm`
  - `fweiuhfge2232n12@#xSD23@`
- Controller-side Validate can skip network calls by default:
  - `validate.skipNetworkOnController=true`

## Flow summary by script

- `workspace/WaffleBearWorkspaceListScenario.groovy`
  - login -> `GET /workspace/list`
- `workspace/WaffleBearWorkspaceSaveScenario.groovy`
  - login -> `POST /workspace/save` -> cleanup `POST /workspace/delete/{idx}`
- `workspace/WaffleBearWorkspaceReadScenario.groovy`
  - login -> create workspace -> `GET /workspace/read/{idx}` -> cleanup
- `workspace/WaffleBearWorkspaceByUuidScenario.groovy`
  - login -> create workspace -> `GET /workspace/by-uuid/{uuid}` -> cleanup
- `workspace/WaffleBearWorkspaceIsSharedScenario.groovy`
  - login -> create workspace -> `POST /workspace/isShared/{idx}` -> cleanup
- `workspace/WaffleBearWorkspaceLoadRoleScenario.groovy`
  - login -> create workspace -> `GET /workspace/loadRole/{idx}` -> cleanup
- `workspace/WaffleBearWorkspaceSaveRoleScenario.groovy`
  - login -> create workspace -> `POST /workspace/saveRole/{idx}` -> cleanup
- `workspace/WaffleBearWorkspaceDeleteScenario.groovy`
  - login -> create workspace -> `POST /workspace/delete/{idx}`
- `workspace/WaffleBearWorkspaceScenario.groovy`
  - login -> list/save/read/by-uuid/isShared/loadRole/saveRole/delete full flow
- `workspace/WaffleBearWorkspaceAdvancedScenario.groovy`
  - login -> list/save/delete flow
  - login -> read + by-uuid flow
  - login -> isShared + loadRole + saveRole flow
- `file/WaffleBearFileScenario.groovy`
  - login -> list -> folder create -> upload init -> pre-signed PUT -> upload complete -> upload abort -> list
- `file/WaffleBearFileAdvancedScenario.groovy`
  - login -> list + folder create
  - login -> upload init + PUT + complete
  - login -> upload init + abort
- `group/WaffleBearGroupChatScenario.groovy`
  - login -> relationships -> overview -> group create/delete -> chatRoom list
- `group/WaffleBearGroupChatAdvancedScenario.groovy`
  - login -> relationships + overview
  - login -> group create + delete
  - login -> chatRoom pagination (`page=0`, `page=1`)
- `notification/WaffleBearNotificationScenario.groovy`
  - login -> notification list
  - login -> notification list repeated check
  - login -> notification list parse check

## Optional runtime overrides

You can override values from nGrinder properties, JVM args, or env:

- `baseUrl`
- `loginEmail`
- `loginPassword`
- `http.timeout.ms`
- `validate.skipNetworkOnController`

## Notes

- All new `Advanced` scripts are standalone (no helper import required).
- Keep only scripts you need in the same nGrinder folder when possible, to avoid compile issues from unrelated broken files.
- File upload scenarios require object storage endpoint (MinIO/S3) to be reachable from nGrinder agent.
