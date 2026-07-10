# System Management UI Design

## Goal

Add a static System Management module to `VLStream-ui` and connect it to the existing SpringBlade system management backend APIs. The first implementation scope is the selected permission-management loop: users, roles, menus, departments, posts, tenants, data scopes, and API scopes.

## Current Context

The frontend is a Vue 3, Vite, Element Plus application. Routing is static in `src/router/index.js`, and the top navigation plus side menu are static in `src/layout/index.vue`.

The current `src/views/UserManagement.vue` and `src/views/SystemSettings.vue` pages use local mock state and do not call backend APIs. Existing production-style pages, such as `src/views/VideoAggregation/TagManagement.vue`, use Element Plus tables, trees, dialogs, pagination, and hand-written API modules. The system management module should follow that style instead of introducing a new page framework.

The unified request instance in `src/utils/request.js` already sets the platform headers and token header and returns `response.data`. System APIs should use that instance.

## Chosen Approach

Use a static system-management module.

Add a `系统管理` top menu to the current static layout, add static child routes under the existing `Layout`, and create dedicated API wrappers under `src/api/system`. This matches the current application architecture and avoids depending on backend menu database records being ready for dynamic routing.

Dynamic menu generation from `/system/menu/routes` is out of scope for this pass. AVUE CRUD pages are also out of scope because the existing application uses custom Element Plus pages and the role-grant workflow needs a compound tree-selection dialog.

## Visual And Interaction Style

System management pages must follow the current `VLStream-ui` frontend style instead of introducing a generic admin-template look.

Concrete style references:

- Use the existing `Layout` top navigation, left sidebar behavior, content background, and collapse interaction.
- Follow the table-page structure used by existing business pages such as `src/views/VideoAggregation/TagManagement.vue`: toolbar at the top, table or tree-table in the main content area, pagination at the bottom, and modal dialogs for create/edit operations.
- Use Element Plus components directly, matching the current application pattern.
- Use the existing primary color family already present in the app, especially `#1A53FF` for primary actions and active states.
- Keep page sections dense and operational. Avoid marketing-style headers, oversized hero areas, decorative panels, and unrelated illustrations.
- Use icon buttons or icon-plus-text buttons where the current app already does so, and keep destructive operations visually consistent with existing delete actions.
- Reuse existing shared components where they fit, including `ActionButtonGroup`, `SearchInput`, and `CollapseToggle`. Do not force these components into places where their current shape would make the system page harder to use.
- Keep table operations and dialog forms aligned with the current Element Plus spacing and 8px-or-less card/dialog radius style in the app.

The implementation should remove or replace the old mock `UserManagement.vue` behavior only if that route is intentionally reused. Otherwise, the new system pages should live under `src/views/System` and avoid changing unrelated page styling.

## Navigation

Add a top-level menu item:

- `系统管理`

Add these side-menu entries:

- `用户管理` -> `/system/users`
- `角色管理` -> `/system/roles`
- `菜单管理` -> `/system/menus`
- `部门管理` -> `/system/depts`
- `岗位管理` -> `/system/posts`
- `租户管理` -> `/system/tenants`
- `数据权限` -> `/system/data-scopes`
- `接口权限` -> `/system/api-scopes`

The module uses the existing `Layout` shell. The sidebar should be visible when a system route is active, and clicking `系统管理` should open `/system/users`.

## API Layer

Create API modules under `src/api/system`:

- `user.js`
- `role.js`
- `menu.js`
- `dept.js`
- `post.js`
- `tenant.js`
- `dataScope.js`
- `apiScope.js`
- `index.js`

Each API function should include a short JSDoc comment explaining the backend operation and parameter shape.

Use backend paths exactly as exposed by the existing controllers:

- `/system/user/list`, `/system/user/detail`, `/system/user/submit`, `/system/user/update`, `/system/user/remove`, `/system/user/grant`, `/system/user/reset-password`, `/system/user/unlock`
- `/system/role/list`, `/system/role/tree`, `/system/role/tree-by-id`, `/system/role/submit`, `/system/role/remove`, `/system/role/grant`
- `/system/menu/list`, `/system/menu/menu-list`, `/system/menu/tree`, `/system/menu/grant-tree`, `/system/menu/role-tree-keys`, `/system/menu/submit`, `/system/menu/remove`
- `/system/dept/list`, `/system/dept/tree`, `/system/dept/submit`, `/system/dept/remove`
- `/system/post/list`, `/system/post/select`, `/system/post/submit`, `/system/post/remove`
- `/system/tenant/list`, `/system/tenant/select`, `/system/tenant/submit`, `/system/tenant/remove`
- `/system/data-scope/list`, `/system/data-scope/detail`, `/system/data-scope/submit`, `/system/data-scope/remove`
- `/system/api-scope/list`, `/system/api-scope/detail`, `/system/api-scope/submit`, `/system/api-scope/remove`

For `POST /remove` endpoints, pass `ids` as query params. For user role assignment, pass `userIds` and `roleIds` as query params. For role grant, pass a JSON body containing `roleIds`, `menuIds`, `dataScopeIds`, and `apiScopeIds`.

## Shared Frontend Utilities

Create `src/views/System/utils/response.js`.

Responsibilities:

- Extract business payload from SpringBlade `R` responses.
- Extract pagination records from either `data.records`, `records`, or an empty fallback.
- Extract total count from either `data.total`, `total`, or `0`.
- Normalize tree labels when backend rows use `title`, `name`, `roleName`, `deptName`, or `scopeName`.

Create `src/views/System/components/SystemPageShell.vue`.

Responsibilities:

- Provide a consistent page container, toolbar row, content area, and pagination slot that matches the current `VLStream-ui` business-page style.
- Keep layout styling local to the system pages.
- Avoid business logic; pages own data loading and mutations.

Create `src/views/System/components/PermissionGrantDialog.vue`.

Responsibilities:

- Load grant trees from `/system/menu/grant-tree`.
- Load checked keys from `/system/menu/role-tree-keys`.
- Render three checkbox trees: menu permissions, data permissions, and API permissions.
- Submit role authorization through `/system/role/grant`.

## Pages

### User Management

Create `src/views/System/UserManagement.vue`.

Features:

- Search by `account` and `realName`.
- Load paginated users through `/system/user/list`.
- Add or edit users through `/system/user/submit`.
- Remove selected users through `/system/user/remove`.
- Assign roles through `/system/user/grant`.
- Reset password through `/system/user/reset-password`.
- Unlock account through `/system/user/unlock`.
- Load role tree, department tree, post select, and tenant select for form controls.

User form fields:

- `account`
- `name`
- `realName`
- `email`
- `phone`
- `sex`
- `birthday`
- `tenantId`
- `deptId`
- `postId`
- `roleId`

### Role Management

Create `src/views/System/RoleManagement.vue`.

Features:

- Load role list through `/system/role/list`.
- Display role hierarchy using `parentId`.
- Add or edit roles through `/system/role/submit`.
- Remove selected roles through `/system/role/remove`.
- Open permission grant dialog for selected role.

Role form fields:

- `tenantId`
- `parentId`
- `roleName`
- `roleAlias`
- `sort`

### Menu Management

Create `src/views/System/MenuManagement.vue`.

Features:

- Load menu tree list through `/system/menu/list`.
- Add or edit menu entries through `/system/menu/submit`.
- Remove selected entries through `/system/menu/remove`.
- Support menu, submenu, and button entries through `category`.

Menu form fields:

- `parentId`
- `code`
- `name`
- `alias`
- `path`
- `source`
- `category`
- `action`
- `isOpen`
- `sort`
- `remark`

### Department Management

Create `src/views/System/DeptManagement.vue`.

Features:

- Load department tree through `/system/dept/list` or `/system/dept/tree`.
- Add or edit departments through `/system/dept/submit`.
- Remove selected departments through `/system/dept/remove`.

Department form fields:

- `tenantId`
- `parentId`
- `deptName`
- `fullName`
- `sort`
- `remark`

### Post Management

Create `src/views/System/PostManagement.vue`.

Features:

- Load paginated posts through `/system/post/list`.
- Add or edit posts through `/system/post/submit`.
- Remove selected posts through `/system/post/remove`.

Post form fields:

- `category`
- `postCode`
- `postName`
- `sort`
- `remark`

### Tenant Management

Create `src/views/System/TenantManagement.vue`.

Features:

- Load paginated tenants through `/system/tenant/list`.
- Add or edit tenants through `/system/tenant/submit`.
- Remove selected tenants through `/system/tenant/remove`.

Tenant form fields:

- `tenantId`
- `tenantName`
- `domain`
- `linkman`
- `contactNumber`
- `address`

### Data Scope Management

Create `src/views/System/DataScopeManagement.vue`.

Features:

- Load paginated data scopes through `/system/data-scope/list`.
- Add or edit data scopes through `/system/data-scope/submit`.
- Remove selected scopes through `/system/data-scope/remove`.
- Select related menu from `/system/menu/menu-list`.

Data scope form fields:

- `menuId`
- `resourceCode`
- `scopeName`
- `scopeField`
- `scopeClass`
- `scopeColumn`
- `scopeType`
- `scopeValue`
- `remark`

### API Scope Management

Create `src/views/System/ApiScopeManagement.vue`.

Features:

- Load paginated API scopes through `/system/api-scope/list`.
- Add or edit API scopes through `/system/api-scope/submit`.
- Remove selected scopes through `/system/api-scope/remove`.
- Select related menu from `/system/menu/menu-list`.

API scope form fields:

- `menuId`
- `resourceCode`
- `scopeName`
- `scopePath`
- `scopeType`
- `remark`

## Data Flow

Every page follows the same request lifecycle:

1. Initialize filters and pagination state.
2. Load list data on mount.
3. Convert backend `R.data` into page rows through `response.js`.
4. Open dialog with a cloned row when editing.
5. Validate required fields before submit.
6. Submit to backend.
7. Reload current page after successful mutation.

Tree pages use backend `children` directly when present. If a list is returned without `children`, build a tree by matching `id` and `parentId`.

## Error Handling

The existing axios response interceptor already shows request-level errors. System pages should add business-level messages only after explicit user actions:

- Show success after save, delete, role grant, password reset, unlock, and user-role assignment.
- Show confirmation dialogs before delete, password reset, and unlock.
- Disable batch operations when no row is selected.
- Disable edit and grant actions when selection count is not exactly one.

## Testing

Run frontend build verification from `D:\work\ide\WorkSpace\vlstream-server\VLStream-ui`:

```powershell
rtk pwsh -NoProfile -Command "npm run build"
```

If temporary verification output or screenshots are needed, place them under `D:\work\ide\WorkSpace\vlstream-server\VLStream-ui\codex`.

Manual smoke checks after build:

- Open system management top menu.
- Confirm all eight side-menu entries navigate.
- Confirm each page loads data or shows an empty state without console errors.
- Confirm create/edit/delete dialogs validate required fields.
- Confirm role authorization loads menu, data-scope, and API-scope trees and submits selected keys.

## Execution Handoff

This design spec is not the only execution document. The implementation worker must also follow `docs/superpowers/plans/2026-06-26-system-management-ui-implementation.md`.

The implementation plan contains:

- Exact files to create and modify.
- Backend API wrapper function names and payload shapes.
- Route and layout changes required for the static `系统管理` menu.
- Required page-level behavior for each management page.
- Static verification commands for each task.
- Final `npm run build` verification.
- Manual smoke-test flow and optional `codex/system-management-smoke.md` result template.

The implementation is not acceptable until the final acceptance checklist in the implementation plan is complete. If any check cannot be completed because the backend environment has no usable token, data, or permission, the worker must record the blocked item and reason in `codex/system-management-smoke.md`.

## Out Of Scope

- Backend changes.
- Dynamic route generation from backend menu records.
- Workflow engine pages.
- Dictionary, parameter, auth client, top-menu, and log management pages.
- Replacing existing business modules with backend-driven menu permissions.
