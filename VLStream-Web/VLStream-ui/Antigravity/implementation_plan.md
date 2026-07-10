# Implementation Plan - System Management UI

This plan outlines the steps to build the static System Management frontend module in `VLStream-ui` and connect it with the SpringBlade system management backend APIs.

## User Review Required

Please review the open questions below. Once you select the options and approve the plan, we will proceed with the implementation.

## Open Questions

### Question 1: User Management - Layout for Password Reset and Unlock Actions
The User Management page requires password reset and unlock actions. Since the table already has "Edit", "Assign Roles", and "Delete" buttons, adding two more buttons would make the operation column too wide.
- **[Option A] (Recommended):** Add a "More" (更多) dropdown button in each row's operation column, containing "Reset Password" (重置密码) and "Unlock Account" (解锁账号).
  * *Reason:* Keeps the page neat, prevents horizontal scrolling on lower resolution screens, and aligns with standard modern admin patterns.
- **[Option B]:** Render "Reset Password" and "Unlock Account" directly as flat buttons in the operation column (will increase the column width to ~320px).
- **[Option C]:** Make these operations batch-only and place them in the top toolbar, depending on checkboxes.

### Question 2: Menu Management - Category Selection for Menus vs. Buttons
SpringBlade menu entities use a `category` integer (1 for menus/submenus, 2 for buttons).
- **[Option A] (Recommended):** Form has a simple category selector with "Menu" (菜单, value 1) and "Button" (按钮, value 2). Submenus are created by selecting a parent menu in the "Parent Menu" selector.
  * *Reason:* Directly maps to SpringBlade backend logic without complex translations, and parent selection handles nesting.
- **[Option B]:** Form provides "Menu", "Submenu", and "Button" as three distinct options, and frontend logic automatically sets `category` and enforces parent constraints.

---

## Proposed Changes

We will implement the tasks systematically as outlined in the implementation document.

### 1. API Integration Layer
We will create wrappers for the 8 system domains under `src/api/system`:
- `src/api/system/user.js`
- `src/api/system/role.js`
- `src/api/system/menu.js`
- `src/api/system/dept.js`
- `src/api/system/post.js`
- `src/api/system/tenant.js`
- `src/api/system/dataScope.js`
- `src/api/system/apiScope.js`
- `src/api/system/index.js` (exposing all)

### 2. Common Utilities & Base Components
- **[NEW] [response.js](file:///D:/work/ide/WorkSpace/vlstream-server/VLStream-ui/src/views/System/utils/response.js):** Standardizes backend response parsing, tree rebuilding, and normalization.
- **[NEW] [SystemPageShell.vue](file:///D:/work/ide/WorkSpace/vlstream-server/VLStream-ui/src/views/System/components/SystemPageShell.vue):** Custom shell layout matching VLStream's custom toolbar and table structure.
- **[NEW] [PermissionGrantDialog.vue](file:///D:/work/ide/WorkSpace/vlstream-server/VLStream-ui/src/views/System/components/PermissionGrantDialog.vue):** The compound authorization dialog containing Menu, Data, and API scope trees.

### 3. Layout & Routing Changes
- **[MODIFY] [index.vue](file:///D:/work/ide/WorkSpace/vlstream-server/VLStream-ui/src/layout/index.vue):**
  * Add static top menu item "系统管理".
  * Add "system-management" sidebar route configuration mapping to the 8 side menus.
  * Import and register side menu icons.
- **[MODIFY] [index.js](file:///D:/work/ide/WorkSpace/vlstream-server/VLStream-ui/src/router/index.js):**
  * Register the 8 static system sub-routes.

### 4. Management Pages (src/views/System)
We will create the 8 page components following VLStream's visual design (Element Plus tables/trees, card-less layout, dense UI, primary color `#1A53FF`):
- `UserManagement.vue` (supports role assignment, unlock, password reset, search)
- `RoleManagement.vue` (supports parent-child roles tree table, permission grant trigger)
- `MenuManagement.vue` (supports menu tree table, category constraints)
- `DeptManagement.vue` (supports department hierarchy tree table)
- `PostManagement.vue` (supports paginated list)
- `TenantManagement.vue` (supports paginated list)
- `DataScopeManagement.vue` (supports menu-associated data scope rules)
- `ApiScopeManagement.vue` (supports menu-associated API scope paths)

---

## Verification Plan

### Automated Static Verification
We will run ripgrep commands to confirm all endpoints, file structures, and critical props are in place:
1. `rtk pwsh -NoProfile -Command "rg -n \"export function|get[A-Z].*List|submit|remove|grant\" src/api/system"`
2. `rtk pwsh -NoProfile -Command "rg -n \"PermissionGrantDialog|getRoleList|submitRole|removeRoles|roleAlias|parentId\" src/views/System/RoleManagement.vue"`

### Build Verification
Run Vite compilation checks:
`rtk pwsh -NoProfile -Command "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; npm run build"`

### Manual Verification
1. Open local dev site, navigate through the newly created "系统管理" menu.
2. Verify all 8 pages load successfully and display tables (or empty placeholders) without console errors.
3. Test creating, editing, and deleting a temporary post.
4. Test the compound "权限配置" dialog on a role, checking Menu, Data, and API tabs, and submit the grant payload.
5. Record any blockages or environment limits in `codex/system-management-smoke.md`.
