# Workorder Center With VLS Identity Design

Date: 2026-06-30

## Context

`vls-server` owns the user, department, role, permission, login, and JWT token system. It runs on JDK 17, Spring Boot 3.2, SpringBlade, and Jakarta APIs.

`apaas-workflowforms` owns the workflow-form and workorder capabilities. It runs on JDK 8, Spring Boot 2.7, RuoYi, Sa-Token, Flowable, and Javax APIs.

The implementation target is the copied repository under `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms`. The original external directory `D:\work\ide\WorkSpace\apaas-workflowforms` is reference material only unless explicitly requested.

The target is not to merge the two services. The target is to keep `apaas-workflowforms` as an independent workorder-center service, keep Flowable as an internal engine, remove the public product surface of a general workflow platform, and rely on `vls-server` as the identity authority.

## Goals

- Expose only workorder-center capabilities to users and open-source consumers.
- Keep Flowable internally for arbitrary workorder node design, task routing, approval, transfer, return, copy, and history.
- Do not expose a generic workflow platform product surface.
- Authenticate workorder-service requests with `vls-server` JWT tokens.
- Use `vls-server` user, department, role, and permission data as the single identity source.
- Keep the first phase on the existing `apaas-workflowforms` runtime baseline.
- Do not migrate existing historical workorders, Flowable instances, UUID users, or old tenant data.
- Verify the full workorder flow end to end.

## Non-Goals

- Do not merge `apaas-workflowforms` into `vls-server` in phase one.
- Do not upgrade `apaas-workflowforms` to JDK 17 or Spring Boot 3 in phase one.
- Do not preserve the old `apaas-workflowforms` standalone login system in production mode.
- Do not support production use of the workorder service without `vls-server`.
- Do not keep generic workflow management APIs as public APIs.
- Do not migrate historical Flowable runtime or history tables.

## Architecture

The first phase uses an independent workorder-service architecture:

- `vls-server`: identity authority, login service, JWT issuer, and owner of users, departments, roles, and permissions.
- `apaas-workflowforms`: workorder center, workorder model design, dynamic forms, workorder instances, task handling, approval history, and Flowable runtime.

The workorder service keeps its own database for workorder and workflow execution data:

- Workorder business tables, such as `work_order` and related workorder category/app/model tables.
- Workorder form/model tables, including the required subset of `wf_*` and `re_model_json`.
- Flowable runtime/history tables, such as `ACT_*`.
- Workorder service configuration tables, such as dictionaries, menu entries, and permission-code metadata required by the workorder center.

The workorder service also adds a read-only VLS identity data source. It reads stable views from the `vls-server` database but does not write to the VLS database and does not join workorder business transactions with VLS tables.

Design note: this is not a shared-business-database design. It is an independent business database plus read-only identity projection views.

## VLS Read-Only Views

`vls-server` should provide stable read-only views as the identity contract for the workorder service. These views should expose the minimum fields needed by workorder identity, selection, task routing, and permission checks.

Suggested views:

- `vls_user_view`
  - `user_id`
  - `tenant_id`
  - `account`
  - `user_name`
  - `real_name`
  - `dept_id`
  - `status`
  - `is_deleted`
- `vls_dept_view`
  - `dept_id`
  - `tenant_id`
  - `parent_id`
  - `dept_name`
  - `ancestors`
  - `status`
  - `is_deleted`
- `vls_role_view`
  - `role_id`
  - `tenant_id`
  - `role_name`
  - `role_alias`
  - `status`
  - `is_deleted`
- `vls_user_role_view`
  - `user_id`
  - `role_id`
  - `tenant_id`
- `vls_role_permission_view`
  - `role_id`
  - `permission_code`
  - `tenant_id`
- Optional `vls_permission_view`
  - `permission_code`
  - `permission_name`
  - `status`

All VLS IDs are read as strings by the workorder service. VLS `Long` IDs are converted to strings and used consistently in workorder tables and Flowable identity fields.

## Authentication And Current User Context

The workorder service should not expose a production login endpoint. Requests carry a JWT issued by `vls-server`.

Authentication flow:

1. The caller sends the VLS JWT in the `Authorization` header.
2. `VlsJwtAuthenticationAdapter` validates the token signature and expiration using VLS-compatible JWT settings.
3. The adapter extracts `userId`, `tenantId`, `deptId`, `roleId`, `account`, and `userName`.
4. `userId` is converted to a string and becomes the workorder-service user ID.
5. `CurrentUserProvider` constructs the current user context used by workorder controllers, services, and Flowable operations.
6. Existing direct reads from Sa-Token or Redis-backed login state are replaced with `CurrentUserProvider`.

Sa-Token handling:

- Do not remove Sa-Token dependencies in phase one if that would cause broad framework churn.
- Disable standalone login, registration, captcha, and online-user product endpoints.
- Permission checks should read the workorder permission adapter, which reads VLS-derived role/permission cache data.
- Legacy code patterns such as `RedisUtils.getCacheObject(AuthorizationInterceptor.getToken())` should be replaced with `CurrentUserProvider.getCurrentUser()`.

Design note: the phase-one goal is to isolate current-user and permission access behind adapters, not to rewrite the whole RuoYi authentication stack.

## Identity Cache

The VLS read-only views are the authoritative identity source. The workorder-service cache is a runtime acceleration layer.

Cache behavior:

1. Load identity data from the VLS read-only views at startup.
2. Refresh identity data on a schedule, such as every 1 to 5 minutes.
3. Use cache first for login context enrichment, user selection, department selection, role selection, and permission checks.
4. On cache miss, fall back to the VLS read-only views.
5. If the VLS read-only views are temporarily unavailable, keep serving with a valid stale cache for a configurable grace period.
6. After the maximum grace period, reject identity-sensitive operations such as starting a process, selecting assignees, and permission checks.
7. Provide an admin-only endpoint to refresh identity cache manually.

Suggested components:

- `VlsIdentityViewRepository`: reads users, departments, roles, and permissions from the VLS read-only data source.
- `WorkOrderIdentityCache`: holds the active identity snapshot and refresh metadata.
- `WorkOrderPermissionAdapter`: answers permission checks using current user roles and workorder permission codes.

## Workorder And Flowable Boundary

Product language and public APIs expose only the workorder center. Flowable remains internal.

Keep these capabilities:

- Workorder model design, named as workorder type, template, or model.
- Dynamic form configuration for workorder submission and task handling.
- Arbitrary node routing for workorder approval and handling.
- Transfer, return, copy, approval history, and task history.
- My workorders, todo workorders, finished workorders, and claimable workorders.
- `work_order` as the business master table.
- Flowable process instances as internal execution records.
- Workorder Excel and PDF export.

Remove or disable these public capabilities:

- Generic workflow management menus.
- Generic workflow category APIs.
- Generic workflow model APIs.
- Generic workflow deployment APIs.
- Generic workflow definition APIs.
- Generic workflow instance management APIs.
- Generic workflow platform examples and demo data.

Controller boundary:

- Generic workflow controllers should be removed when possible.
- If a controller cannot be removed immediately, it should not register public routes.
- Core services, mappers, utility classes, and Flowable integration logic should remain available for workorder-domain controllers.
- Workorder-domain controllers should expose required functionality under `/workorder/**`.
- Swagger/Knife4j must not show generic workflow platform APIs.

Suggested public route groups:

- `/workorder/model/**`: workorder model design, publish, disable, and query.
- `/workorder/form/**`: workorder form configuration.
- `/workorder/workorder/**`: workorder instance creation, query, update, delete, and export.
- `/workorder/task/**`: todo, finished, claim, handle, return, transfer, and approval actions.
- `/workorder/admin/**`: identity-cache refresh, health checks, and required operational actions.

Design note: controllers define the product boundary; services retain the technical capability.

## Permissions

The workorder service keeps its own workorder permission codes, such as `workorder:*` and the required workorder-domain subset formerly represented by `workflow:*`.

`vls-server` remains the permission source. The workorder service reads role-to-permission grants from VLS read-only views and caches them locally.

Rules:

- Workorder permission codes remain stable inside the workorder service.
- VLS role assignments determine which users can use those workorder permission codes.
- Generic workflow platform permission codes should not be exposed as product-facing permissions.
- If an old `workflow:*` code is still required internally, map it to a workorder-domain permission where possible.

## Data Initialization

Phase one initializes a clean workorder center. It does not import old historical data.

Initialize:

- Workorder center menus.
- Workorder center permission codes.
- Workorder dictionaries, such as status, priority, approval buttons, and task status.
- Flowable table structure.
- Workorder business table structure.
- Required form/model table structure.
- Optional example workorder models that do not reference old UUID users, old tenants, or old departments.

Do not initialize:

- Historical workorder instances.
- Historical Flowable process instances.
- Old RuoYi users, departments, roles, or permission master data.
- Generic workflow platform menus.
- Generic workflow demo data.
- Monitor, generator, xxl-job management data unless required at runtime.

Suggested database naming:

- Workorder business database: `vls_workorder` or `apaas_workorder_center`.
- VLS read-only view prefix: `vls_*_view`.

## Error Handling And Degraded Behavior

Authentication:

- Missing, expired, or invalid JWT returns 401.
- Valid JWT with disabled or missing VLS user returns 403.
- Token missing required identity claims returns 401 and records a security log.

Permissions:

- Authenticated user without the required workorder permission returns 403.
- Cache miss triggers a VLS read-only view lookup.
- VLS view unavailable and no usable cache returns 503 for identity-sensitive operations.
- VLS view unavailable but cache still within the configured grace period continues serving and records an alert.

Cache:

- Cache entries should track `lastRefreshTime` and `expiresAt`.
- A configurable grace period, such as 10 to 30 minutes, may allow stale identity data.
- After maximum grace period, reject permission checks, assignee selection, and process start operations.
- Existing workorder lists may continue during grace period, but display fields may degrade to user IDs if names are unavailable.

Workorder and Flowable consistency:

- If workorder creation fails after Flowable start, the transaction should roll back both state changes when they are in the same database transaction.
- If full rollback is not possible, record a compensating repair task.
- Task handling must verify the current user is the assignee or a valid candidate before mutation.
- Return, transfer, delegate, approve, or reject failures must not update the workorder status.

## Full Flow Verification

The full workorder flow must be tested end to end before considering implementation complete.

Required verification chain:

1. Prepare VLS user, department, role, and permission view data.
2. Start the workorder service and load VLS identity cache.
3. Access the workorder service with a VLS JWT.
4. Create a workorder model with form fields, nodes, assignees or candidate roles, and approval buttons.
5. Publish the workorder model.
6. Start a workorder and verify both `work_order` and the Flowable process instance are created.
7. Query "my workorders".
8. Query the handler's todo workorders.
9. Execute claim, handle, return, transfer, approve, reject, and complete actions required by the supported model.
10. Verify workorder status, task status, and Flowable state after each action.
11. Query finished workorders and approval history.
12. Export Excel and PDF.
13. Verify unauthorized users cannot query or handle workorders.
14. Verify generic workflow controller routes are inaccessible and absent from API docs.
15. Simulate temporary VLS view outage and verify cache-based degraded behavior.
16. Simulate cache expiration beyond the grace period and verify identity-sensitive operations fail safely.
17. Run full compile and automated tests.

## Build And Test Constraints

- For `apaas-workflowforms`, read its root `pom.xml` before build/test and use JDK 8 because `<java.version>1.8</java.version>`.
- For `vls-server`, read its root `pom.xml` before build/test and use JDK 17 because `<java.version>17</java.version>`.
- Test-generated files, reports, and temporary scripts should be placed under the relevant project root `codex/` directory.
- PowerShell commands must use the system `pwsh`.
- Shell commands should use the local `rtk` prefix.

## Rollout Plan

All implementation steps target `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms` for the workorder service and `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server` for VLS view/JWT integration work.

1. Add VLS read-only views and document required fields.
2. Add workorder-service VLS read-only data source configuration.
3. Add JWT authentication adapter and current-user provider.
4. Add identity repository, cache, and permission adapter.
5. Replace direct current-user reads in workorder paths.
6. Remove or disable generic workflow controllers.
7. Re-expose required model/form/task capabilities through workorder-domain controllers.
8. Clean initialization scripts and remove old user/workflow demo data.
9. Update deployment documentation.
10. Implement automated tests and execute the full flow verification chain.

## Implementation Defaults

- Use the view names documented in this spec: `vls_user_view`, `vls_dept_view`, `vls_role_view`, `vls_user_role_view`, `vls_role_permission_view`, and optional `vls_permission_view`.
- Default identity-cache refresh interval is 5 minutes.
- Default stale-cache grace period is 30 minutes.
- Generic workflow controllers should be removed when their services are not directly required by workorder routes. Known first-pass candidates include `WfCategoryController`, `WfModelController`, `WfDeployController`, `WfInstanceController`, `WfProcessController`, and generic parts of `WfTaskController`.
- Form/model controllers that are required for workorder model design should be re-exposed only under `/workorder/**`. Known review candidates include `WfFormController`, `WfFormAppController`, `WfFormSynthesisController`, `ReModeJsonController`, `ProcessTemplateController`, and `ProcessViewLogController`.
- Workorder permission codes should use `workorder:*` for public product permissions. Any remaining internal `workflow:*` checks should be wrapped or mapped behind the workorder permission adapter.
- The open-source seed model should be a simple multi-node workorder model using VLS string IDs from seed view data, not old UUID users or old tenants.
