# System Management UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a static System Management module in `VLStream-ui` that connects to the existing SpringBlade backend system APIs and follows the current VLStream frontend style.

**Architecture:** Add static Vue Router entries and a `系统管理` top-menu branch in the existing layout. Add system API wrappers under `src/api/system`, shared response/layout helpers under `src/views/System`, and one Vue page per management area. Keep business logic inside each page and use small shared utilities only for common response parsing and shell layout.

**Tech Stack:** Vue 3 Composition API, Vite, Element Plus, existing `src/utils/request.js`, existing static `src/layout/index.vue`, existing static `src/router/index.js`.

---

## Required Reading Before Editing

- [Design spec](../specs/2026-06-26-system-management-ui-design.md)
- `src/utils/request.js`
- `src/router/index.js`
- `src/layout/index.vue`
- `src/views/VideoAggregation/TagManagement.vue`
- `src/components/ActionButtonGroup.vue`
- `src/components/SearchInput.vue`
- `src/components/CollapseToggle.vue`

## Execution Rules

- Run PowerShell commands through `rtk pwsh -NoProfile -Command "..."`.
- Do not modify backend code.
- Do not add new npm dependencies.
- Do not introduce dynamic backend-driven routing in this pass.
- Keep system pages under `src/views/System`.
- Keep temporary verification files, screenshots, or logs under `D:\work\ide\WorkSpace\vlstream-server\VLStream-ui\codex`.
- Use the current visual style: Element Plus table pages, compact toolbar, modal forms, `#1A53FF` primary color, existing top navigation and sidebar behavior.
- Avoid replacing unrelated business page styles.

## File Map

Create:

- `src/api/system/user.js`
- `src/api/system/role.js`
- `src/api/system/menu.js`
- `src/api/system/dept.js`
- `src/api/system/post.js`
- `src/api/system/tenant.js`
- `src/api/system/dataScope.js`
- `src/api/system/apiScope.js`
- `src/api/system/index.js`
- `src/views/System/utils/response.js`
- `src/views/System/components/SystemPageShell.vue`
- `src/views/System/components/PermissionGrantDialog.vue`
- `src/views/System/UserManagement.vue`
- `src/views/System/RoleManagement.vue`
- `src/views/System/MenuManagement.vue`
- `src/views/System/DeptManagement.vue`
- `src/views/System/PostManagement.vue`
- `src/views/System/TenantManagement.vue`
- `src/views/System/DataScopeManagement.vue`
- `src/views/System/ApiScopeManagement.vue`

Modify:

- `src/router/index.js`
- `src/layout/index.vue`

Optional:

- Leave `src/views/UserManagement.vue` unchanged unless you intentionally redirect old `/user-management` usage to the new `/system/users` route.
- Leave `src/views/SystemSettings.vue` unchanged.

---

### Task 1: Add System API Wrappers

**Files:**

- Create: `src/api/system/user.js`
- Create: `src/api/system/role.js`
- Create: `src/api/system/menu.js`
- Create: `src/api/system/dept.js`
- Create: `src/api/system/post.js`
- Create: `src/api/system/tenant.js`
- Create: `src/api/system/dataScope.js`
- Create: `src/api/system/apiScope.js`
- Create: `src/api/system/index.js`

- [ ] **Step 1: Create `src/api/system/user.js`**

```js
import request from '@/utils/request'

/** Query paginated users. params: current, size, account, realName. */
export function getUserList(params) {
  return request({ url: '/system/user/list', method: 'get', params })
}

/** Query one user detail. params must include id. */
export function getUserDetail(params) {
  return request({ url: '/system/user/detail', method: 'get', params })
}

/** Create or update a user. data follows backend User entity fields. */
export function submitUser(data) {
  return request({ url: '/system/user/submit', method: 'post', data })
}

/** Update a user by id. data follows backend User entity fields. */
export function updateUser(data) {
  return request({ url: '/system/user/update', method: 'post', data })
}

/** Remove users by comma-separated ids. */
export function removeUsers(ids) {
  return request({ url: '/system/user/remove', method: 'post', params: { ids } })
}

/** Assign role ids to user ids. Both arguments are comma-separated strings. */
export function grantUserRoles(userIds, roleIds) {
  return request({ url: '/system/user/grant', method: 'post', params: { userIds, roleIds } })
}

/** Reset passwords for comma-separated user ids. */
export function resetUserPassword(userIds) {
  return request({ url: '/system/user/reset-password', method: 'post', params: { userIds } })
}

/** Unlock comma-separated user ids. */
export function unlockUsers(userIds) {
  return request({ url: '/system/user/unlock', method: 'post', params: { userIds } })
}
```

- [ ] **Step 2: Create `src/api/system/role.js`**

```js
import request from '@/utils/request'

/** Query role list. params: roleName, roleAlias, tenantId. */
export function getRoleList(params) {
  return request({ url: '/system/role/list', method: 'get', params })
}

/** Query role tree. params may include tenantId. */
export function getRoleTree(params) {
  return request({ url: '/system/role/tree', method: 'get', params })
}

/** Query role tree for a specific role id. */
export function getRoleTreeById(roleId) {
  return request({ url: '/system/role/tree-by-id', method: 'get', params: { roleId } })
}

/** Create or update a role. data follows backend Role entity fields. */
export function submitRole(data) {
  return request({ url: '/system/role/submit', method: 'post', data })
}

/** Remove roles by comma-separated ids. */
export function removeRoles(ids) {
  return request({ url: '/system/role/remove', method: 'post', params: { ids } })
}

/** Grant menu, data-scope, and api-scope permissions to roles. */
export function grantRole(data) {
  return request({ url: '/system/role/grant', method: 'post', data })
}
```

- [ ] **Step 3: Create `src/api/system/menu.js`**

```js
import request from '@/utils/request'

/** Query all menus and buttons as a tree-capable list. */
export function getMenuList(params) {
  return request({ url: '/system/menu/list', method: 'get', params })
}

/** Query menu-only list where category is menu. */
export function getMenuOnlyList(params) {
  return request({ url: '/system/menu/menu-list', method: 'get', params })
}

/** Query menu tree. */
export function getMenuTree(params) {
  return request({ url: '/system/menu/tree', method: 'get', params })
}

/** Query role grant tree containing menu, dataScope, and apiScope trees. */
export function getGrantTree(params) {
  return request({ url: '/system/menu/grant-tree', method: 'get', params })
}

/** Query checked menu, dataScope, and apiScope keys for role ids. */
export function getRoleTreeKeys(roleIds) {
  return request({ url: '/system/menu/role-tree-keys', method: 'get', params: { roleIds } })
}

/** Create or update a menu or button. */
export function submitMenu(data) {
  return request({ url: '/system/menu/submit', method: 'post', data })
}

/** Remove menus by comma-separated ids. */
export function removeMenus(ids) {
  return request({ url: '/system/menu/remove', method: 'post', params: { ids } })
}
```

- [ ] **Step 4: Create organization and scope API modules**

Create these files using the same pattern:

```js
// src/api/system/dept.js
import request from '@/utils/request'

/** Query department list. params: deptName, fullName, tenantId. */
export function getDeptList(params) {
  return request({ url: '/system/dept/list', method: 'get', params })
}

/** Query department tree. params may include tenantId. */
export function getDeptTree(params) {
  return request({ url: '/system/dept/tree', method: 'get', params })
}

/** Create or update a department. */
export function submitDept(data) {
  return request({ url: '/system/dept/submit', method: 'post', data })
}

/** Remove departments by comma-separated ids. */
export function removeDepts(ids) {
  return request({ url: '/system/dept/remove', method: 'post', params: { ids } })
}
```

```js
// src/api/system/post.js
import request from '@/utils/request'

/** Query paginated posts. params: current, size, postCode, postName. */
export function getPostList(params) {
  return request({ url: '/system/post/list', method: 'get', params })
}

/** Query posts for select controls. params may include tenantId. */
export function getPostSelect(params) {
  return request({ url: '/system/post/select', method: 'get', params })
}

/** Create or update a post. */
export function submitPost(data) {
  return request({ url: '/system/post/submit', method: 'post', data })
}

/** Remove posts by comma-separated ids. */
export function removePosts(ids) {
  return request({ url: '/system/post/remove', method: 'post', params: { ids } })
}
```

```js
// src/api/system/tenant.js
import request from '@/utils/request'

/** Query paginated tenants. params: current, size, tenantId, tenantName, contactNumber. */
export function getTenantList(params) {
  return request({ url: '/system/tenant/list', method: 'get', params })
}

/** Query tenants for select controls. */
export function getTenantSelect(params) {
  return request({ url: '/system/tenant/select', method: 'get', params })
}

/** Create or update a tenant. */
export function submitTenant(data) {
  return request({ url: '/system/tenant/submit', method: 'post', data })
}

/** Remove tenants by comma-separated ids. */
export function removeTenants(ids) {
  return request({ url: '/system/tenant/remove', method: 'post', params: { ids } })
}
```

```js
// src/api/system/dataScope.js
import request from '@/utils/request'

/** Query paginated data scopes. params: current, size, scopeName, resourceCode. */
export function getDataScopeList(params) {
  return request({ url: '/system/data-scope/list', method: 'get', params })
}

/** Query one data-scope detail. params must include id. */
export function getDataScopeDetail(params) {
  return request({ url: '/system/data-scope/detail', method: 'get', params })
}

/** Create or update a data scope. */
export function submitDataScope(data) {
  return request({ url: '/system/data-scope/submit', method: 'post', data })
}

/** Remove data scopes by comma-separated ids. */
export function removeDataScopes(ids) {
  return request({ url: '/system/data-scope/remove', method: 'post', params: { ids } })
}
```

```js
// src/api/system/apiScope.js
import request from '@/utils/request'

/** Query paginated API scopes. params: current, size, scopeName, resourceCode. */
export function getApiScopeList(params) {
  return request({ url: '/system/api-scope/list', method: 'get', params })
}

/** Query one API-scope detail. params must include id. */
export function getApiScopeDetail(params) {
  return request({ url: '/system/api-scope/detail', method: 'get', params })
}

/** Create or update an API scope. */
export function submitApiScope(data) {
  return request({ url: '/system/api-scope/submit', method: 'post', data })
}

/** Remove API scopes by comma-separated ids. */
export function removeApiScopes(ids) {
  return request({ url: '/system/api-scope/remove', method: 'post', params: { ids } })
}
```

- [ ] **Step 5: Create `src/api/system/index.js`**

```js
export * from './user'
export * from './role'
export * from './menu'
export * from './dept'
export * from './post'
export * from './tenant'
export * from './dataScope'
export * from './apiScope'
```

- [ ] **Step 6: Static verification**

Run:

```powershell
rtk pwsh -NoProfile -Command "rg -n \"export function|get[A-Z].*List|submit|remove|grant\" src/api/system"
```

Expected:

- The command exits with code `0`.
- Output includes functions for all eight system domains.

---

### Task 2: Add Shared Response Utilities

**Files:**

- Create: `src/views/System/utils/response.js`

- [ ] **Step 1: Create response helpers**

```js
/** Return SpringBlade business payload from either R<T> or raw payload. */
export function getPayload(response) {
  if (!response) return null
  if (Object.prototype.hasOwnProperty.call(response, 'data')) return response.data
  return response
}

/** Return paginated records from SpringBlade page responses. */
export function getRecords(response) {
  const payload = getPayload(response)
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(response?.records)) return response.records
  return []
}

/** Return total count from SpringBlade page responses. */
export function getTotal(response) {
  const payload = getPayload(response)
  return Number(payload?.total ?? response?.total ?? 0)
}

/** Convert a list to a tree when backend returns flat parentId rows. */
export function buildTree(list, rootParentIds = [0, '0', null, undefined]) {
  const source = Array.isArray(list) ? list : []
  const nodeMap = new Map()
  source.forEach((item) => {
    nodeMap.set(String(item.id), { ...item, children: Array.isArray(item.children) ? item.children : [] })
  })

  const roots = []
  nodeMap.forEach((node) => {
    const parentId = node.parentId
    const parent = nodeMap.get(String(parentId))
    if (parent && parent.id !== node.id) {
      parent.children.push(node)
    } else if (rootParentIds.includes(parentId)) {
      roots.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

/** Add Element Plus tree labels while preserving original backend fields. */
export function normalizeTree(nodes) {
  const list = Array.isArray(nodes) ? nodes : []
  return list.map((node) => {
    const label = node.label || node.title || node.name || node.roleName || node.deptName || node.scopeName || node.menuName || node.fullName || node.postName || node.tenantName || node.code || node.id
    return {
      ...node,
      label,
      children: normalizeTree(node.children)
    }
  })
}

/** Join selected row ids for SpringBlade comma-separated ids params. */
export function joinIds(rows) {
  return (Array.isArray(rows) ? rows : [])
    .map((row) => row?.id)
    .filter((id) => id !== undefined && id !== null && id !== '')
    .join(',')
}

/** Return whether SpringBlade mutation response represents success. */
export function isSuccess(response) {
  if (!response) return false
  if (response.success === true) return true
  if (response.code === 200) return true
  return response.data === true
}
```

- [ ] **Step 2: Static verification**

Run:

```powershell
rtk pwsh -NoProfile -Command "rg -n \"export function getPayload|export function buildTree|export function normalizeTree|export function joinIds\" src/views/System/utils/response.js"
```

Expected:

- The command exits with code `0`.
- All four function names appear.

---

### Task 3: Add Shared System Page Shell

**Files:**

- Create: `src/views/System/components/SystemPageShell.vue`

- [ ] **Step 1: Create the shell component**

```vue
<template>
  <div class="system-page">
    <div v-if="$slots.toolbar" class="system-toolbar">
      <slot name="toolbar" />
    </div>

    <div class="system-content">
      <slot />
    </div>

    <div v-if="$slots.pagination" class="system-pagination">
      <slot name="pagination" />
    </div>
  </div>
</template>

<style scoped>
.system-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  padding: 20px;
  overflow: hidden;
}

.system-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 8px 8px 0 0;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.system-content {
  flex: 1;
  min-height: 0;
  background: #fff;
  padding: 20px;
  overflow: hidden;
}

.system-pagination {
  display: flex;
  justify-content: flex-end;
  background: #fff;
  border-radius: 0 0 8px 8px;
  padding: 0 20px 20px;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.el-button--primary) {
  background-color: #1A53FF;
  border-color: #1A53FF;
}

:deep(.el-button--primary:hover) {
  background-color: #4A72FF;
  border-color: #4A72FF;
}
</style>
```

- [ ] **Step 2: Static verification**

Run:

```powershell
rtk pwsh -NoProfile -Command "rg -n \"system-page|system-toolbar|#1A53FF\" src/views/System/components/SystemPageShell.vue"
```

Expected:

- The command exits with code `0`.
- Output includes the shell class names and primary color.

---

### Task 4: Add Permission Grant Dialog

**Files:**

- Create: `src/views/System/components/PermissionGrantDialog.vue`

- [ ] **Step 1: Implement permission dialog**

The component must:

- Accept `modelValue` and `role`.
- Load `/system/menu/grant-tree` and `/system/menu/role-tree-keys`.
- Render three `el-tree` controls with checkboxes.
- Submit `{ roleIds, menuIds, dataScopeIds, apiScopeIds }` to `/system/role/grant`.
- Emit `success` after a successful save.

Use this structure:

```vue
<template>
  <el-dialog
    :model-value="modelValue"
    title="权限配置"
    width="900px"
    @open="loadGrantData"
    @close="handleClose"
  >
    <div class="grant-dialog" v-loading="loading">
      <div class="grant-title">
        <span>{{ role?.roleName || '未选择角色' }}</span>
        <small>{{ role?.roleAlias || '' }}</small>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="菜单权限" name="menu">
          <el-tree ref="menuTreeRef" :data="grantTrees.menu" node-key="id" show-checkbox default-expand-all :props="treeProps" />
        </el-tab-pane>
        <el-tab-pane label="数据权限" name="data">
          <el-tree ref="dataScopeTreeRef" :data="grantTrees.dataScope" node-key="id" show-checkbox default-expand-all :props="treeProps" />
        </el-tab-pane>
        <el-tab-pane label="接口权限" name="api">
          <el-tree ref="apiScopeTreeRef" :data="grantTrees.apiScope" node-key="id" show-checkbox default-expand-all :props="treeProps" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getGrantTree, getRoleTreeKeys } from '@/api/system/menu'
import { grantRole } from '@/api/system/role'
import { getPayload, normalizeTree, isSuccess } from '../utils/response'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  role: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'success'])

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('menu')
const menuTreeRef = ref()
const dataScopeTreeRef = ref()
const apiScopeTreeRef = ref()

const treeProps = { label: 'label', children: 'children' }
const grantTrees = reactive({ menu: [], dataScope: [], apiScope: [] })

const setCheckedKeys = async (keys) => {
  await nextTick()
  menuTreeRef.value?.setCheckedKeys(keys?.menu || [])
  dataScopeTreeRef.value?.setCheckedKeys(keys?.dataScope || [])
  apiScopeTreeRef.value?.setCheckedKeys(keys?.apiScope || [])
}

const loadGrantData = async () => {
  if (!props.role?.id) return
  loading.value = true
  try {
    const [treeResponse, keyResponse] = await Promise.all([
      getGrantTree(),
      getRoleTreeKeys(String(props.role.id))
    ])
    const treePayload = getPayload(treeResponse) || {}
    grantTrees.menu = normalizeTree(treePayload.menu || [])
    grantTrees.dataScope = normalizeTree(treePayload.dataScope || [])
    grantTrees.apiScope = normalizeTree(treePayload.apiScope || [])
    const checkedPayload = getPayload(keyResponse) || {}
    await setCheckedKeys(checkedPayload)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!props.role?.id) return
  saving.value = true
  try {
    const response = await grantRole({
      roleIds: String(props.role.id),
      menuIds: menuTreeRef.value?.getCheckedKeys(false).join(',') || '',
      dataScopeIds: dataScopeTreeRef.value?.getCheckedKeys(false).join(',') || '',
      apiScopeIds: apiScopeTreeRef.value?.getCheckedKeys(false).join(',') || ''
    })
    if (isSuccess(response)) {
      ElMessage.success('权限配置已保存')
      emit('success')
      handleClose()
    }
  } finally {
    saving.value = false
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.grant-dialog {
  min-height: 360px;
}

.grant-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #303133;
  font-weight: 600;
}

.grant-title small {
  color: #909399;
  font-weight: 400;
}
</style>
```

- [ ] **Step 2: Static verification**

Run:

```powershell
rtk pwsh -NoProfile -Command "rg -n \"getGrantTree|getRoleTreeKeys|grantRole|menuIds|dataScopeIds|apiScopeIds\" src/views/System/components/PermissionGrantDialog.vue"
```

Expected:

- The command exits with code `0`.
- Output includes all three permission id payload fields.

---

### Task 5: Add Static Routes And System Menu

**Files:**

- Modify: `src/router/index.js`
- Modify: `src/layout/index.vue`

- [ ] **Step 1: Add system child routes inside the existing `Layout` children array**

Add these route records near the other child routes:

```js
{
  path: '/system/users',
  name: 'SystemUserManagement',
  component: () => import('@/views/System/UserManagement.vue'),
  meta: { title: '用户管理', icon: '用户管理' }
},
{
  path: '/system/roles',
  name: 'SystemRoleManagement',
  component: () => import('@/views/System/RoleManagement.vue'),
  meta: { title: '角色管理', icon: '角色管理' }
},
{
  path: '/system/menus',
  name: 'SystemMenuManagement',
  component: () => import('@/views/System/MenuManagement.vue'),
  meta: { title: '菜单管理', icon: '菜单管理' }
},
{
  path: '/system/depts',
  name: 'SystemDeptManagement',
  component: () => import('@/views/System/DeptManagement.vue'),
  meta: { title: '部门管理', icon: '部门管理' }
},
{
  path: '/system/posts',
  name: 'SystemPostManagement',
  component: () => import('@/views/System/PostManagement.vue'),
  meta: { title: '岗位管理', icon: '岗位管理' }
},
{
  path: '/system/tenants',
  name: 'SystemTenantManagement',
  component: () => import('@/views/System/TenantManagement.vue'),
  meta: { title: '租户管理', icon: '租户管理' }
},
{
  path: '/system/data-scopes',
  name: 'SystemDataScopeManagement',
  component: () => import('@/views/System/DataScopeManagement.vue'),
  meta: { title: '数据权限', icon: '数据权限' }
},
{
  path: '/system/api-scopes',
  name: 'SystemApiScopeManagement',
  component: () => import('@/views/System/ApiScopeManagement.vue'),
  meta: { title: '接口权限', icon: '接口权限' }
}
```

- [ ] **Step 2: Add `系统管理` to `topMenus` in `src/layout/index.vue`**

Add this item:

```js
{ key: 'system-management', title: '系统管理' }
```

- [ ] **Step 3: Add side-menu routes to `menuRoutesMap`**

Add this branch:

```js
'system-management': [
  { path: '/system/users', meta: { title: '用户管理', icon: '用户管理' } },
  { path: '/system/roles', meta: { title: '角色管理', icon: '角色管理' } },
  { path: '/system/menus', meta: { title: '菜单管理', icon: '菜单管理' } },
  { path: '/system/depts', meta: { title: '部门管理', icon: '部门管理' } },
  { path: '/system/posts', meta: { title: '岗位管理', icon: '岗位管理' } },
  { path: '/system/tenants', meta: { title: '租户管理', icon: '租户管理' } },
  { path: '/system/data-scopes', meta: { title: '数据权限', icon: '数据权限' } },
  { path: '/system/api-scopes', meta: { title: '接口权限', icon: '接口权限' } }
]
```

- [ ] **Step 4: Add icons for system menu names**

Import icons from `@element-plus/icons-vue` if not already imported:

```js
UserFilled,
User,
Menu,
OfficeBuilding,
Briefcase,
Key,
Lock,
Tickets
```

Add these entries to `iconMap`:

```js
'用户管理': UserFilled,
'角色管理': User,
'菜单管理': Menu,
'部门管理': OfficeBuilding,
'岗位管理': Briefcase,
'租户管理': OfficeBuilding,
'数据权限': Key,
'接口权限': Lock
```

If an icon name conflicts with an existing import, alias it in the import list and use the alias in `iconMap`.

- [ ] **Step 5: Static verification**

Run:

```powershell
rtk pwsh -NoProfile -Command "rg -n \"system-management|/system/users|SystemUserManagement|接口权限|数据权限\" src/router/index.js src/layout/index.vue"
```

Expected:

- The command exits with code `0`.
- Output includes route and layout references for system pages.

---

### Task 6: Implement User Management Page

**Files:**

- Create: `src/views/System/UserManagement.vue`

- [ ] **Step 1: Build the page with these required elements**

Required imports:

```js
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import SystemPageShell from './components/SystemPageShell.vue'
import { getUserList, submitUser, removeUsers, grantUserRoles, resetUserPassword, unlockUsers } from '@/api/system/user'
import { getRoleTree } from '@/api/system/role'
import { getDeptTree } from '@/api/system/dept'
import { getPostSelect } from '@/api/system/post'
import { getTenantSelect } from '@/api/system/tenant'
import { getPayload, getRecords, getTotal, normalizeTree, joinIds, isSuccess } from './utils/response'
```

Required state:

- `loading`, `saving`, `dialogVisible`, `grantDialogVisible`
- `selectedRows`
- `rows`, `total`
- `query` with `current`, `size`, `account`, `realName`
- `form` with `id`, `account`, `name`, `realName`, `email`, `phone`, `sex`, `birthday`, `tenantId`, `deptId`, `postId`, `roleId`
- option lists for roles, departments, posts, and tenants

Required table columns:

- selection
- `account`
- `name`
- `realName`
- `email`
- `phone`
- `roleId`
- `deptId`
- `postId`
- operations: edit, assign roles, reset password, unlock, delete

Required methods:

- `loadOptions()`
- `loadData()`
- `openCreate()`
- `openEdit(row)`
- `handleSubmit()`
- `handleRemove(rowOrRows)`
- `openGrant(row)`
- `handleGrant()`
- `handleResetPassword(row)`
- `handleUnlock(row)`

Validation:

- `account` required.
- `realName` required.
- `roleId` required before assigning roles.

- [ ] **Step 2: Behavior requirements**

- On mount, call `loadOptions()` and `loadData()`.
- For list requests, send `current`, `size`, `account`, and `realName`.
- After save, delete, role assignment, password reset, or unlock, show success and reload the list.
- Use `joinIds(selectedRows.value)` for batch removal.
- Use `String(row.id)` and `String(form.roleId)` when calling `grantUserRoles`.

- [ ] **Step 3: Static verification**

Run:

```powershell
rtk pwsh -NoProfile -Command "rg -n \"getUserList|grantUserRoles|resetUserPassword|unlockUsers|SystemPageShell|roleId|deptId|postId\" src/views/System/UserManagement.vue"
```

Expected:

- The command exits with code `0`.
- Output includes all user management operations and relation fields.

---

### Task 7: Implement Role And Permission Management Page

**Files:**

- Create: `src/views/System/RoleManagement.vue`

- [ ] **Step 1: Build role page**

Required imports:

```js
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SystemPageShell from './components/SystemPageShell.vue'
import PermissionGrantDialog from './components/PermissionGrantDialog.vue'
import { getRoleList, submitRole, removeRoles } from '@/api/system/role'
import { getTenantSelect } from '@/api/system/tenant'
import { buildTree, getPayload, normalizeTree, joinIds, isSuccess } from './utils/response'
```

Required fields:

- `tenantId`
- `parentId`
- `roleName`
- `roleAlias`
- `sort`

Required behavior:

- Load roles with `/system/role/list`.
- Convert list to tree using `buildTree` and `normalizeTree`.
- Show tree table with role name, alias, sort, tenant id.
- Add, edit, delete roles.
- Open `PermissionGrantDialog` for exactly one selected or row-clicked role.
- Reload list after grant dialog emits `success`.

- [ ] **Step 2: Static verification**

Run:

```powershell
rtk pwsh -NoProfile -Command "rg -n \"PermissionGrantDialog|getRoleList|submitRole|removeRoles|roleAlias|parentId\" src/views/System/RoleManagement.vue"
```

Expected:

- The command exits with code `0`.
- Output includes role CRUD and grant dialog references.

---

### Task 8: Implement Menu And Organization Pages

**Files:**

- Create: `src/views/System/MenuManagement.vue`
- Create: `src/views/System/DeptManagement.vue`
- Create: `src/views/System/PostManagement.vue`
- Create: `src/views/System/TenantManagement.vue`

- [ ] **Step 1: Menu page requirements**

Use APIs:

- `getMenuList`
- `submitMenu`
- `removeMenus`

Fields:

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

UI:

- Tree table.
- `category` select with values `1` for menu, `2` for button. If backend data uses other SpringBlade category values, display the raw value with a fallback label.
- Parent menu selector from current menu tree.

- [ ] **Step 2: Department page requirements**

Use APIs:

- `getDeptList`
- `submitDept`
- `removeDepts`
- `getTenantSelect`

Fields:

- `tenantId`
- `parentId`
- `deptName`
- `fullName`
- `sort`
- `remark`

UI:

- Tree table.
- Parent department selector from department tree.

- [ ] **Step 3: Post page requirements**

Use APIs:

- `getPostList`
- `submitPost`
- `removePosts`

Fields:

- `category`
- `postCode`
- `postName`
- `sort`
- `remark`

UI:

- Paginated table.
- Search by `postCode` and `postName`.

- [ ] **Step 4: Tenant page requirements**

Use APIs:

- `getTenantList`
- `submitTenant`
- `removeTenants`

Fields:

- `tenantId`
- `tenantName`
- `domain`
- `linkman`
- `contactNumber`
- `address`

UI:

- Paginated table.
- Search by `tenantId`, `tenantName`, and `contactNumber`.

- [ ] **Step 5: Static verification**

Run:

```powershell
rtk pwsh -NoProfile -Command "rg -n \"getMenuList|submitMenu|removeMenus|category|isOpen\" src/views/System/MenuManagement.vue; rg -n \"getDeptList|submitDept|removeDepts|deptName|fullName\" src/views/System/DeptManagement.vue; rg -n \"getPostList|submitPost|removePosts|postCode|postName\" src/views/System/PostManagement.vue; rg -n \"getTenantList|submitTenant|removeTenants|tenantName|contactNumber\" src/views/System/TenantManagement.vue"
```

Expected:

- The command exits with code `0`.
- Output includes CRUD API usage and required fields for all four pages.

---

### Task 9: Implement Data Scope And API Scope Pages

**Files:**

- Create: `src/views/System/DataScopeManagement.vue`
- Create: `src/views/System/ApiScopeManagement.vue`

- [ ] **Step 1: Data scope page requirements**

Use APIs:

- `getDataScopeList`
- `submitDataScope`
- `removeDataScopes`
- `getMenuOnlyList`

Fields:

- `menuId`
- `resourceCode`
- `scopeName`
- `scopeField`
- `scopeClass`
- `scopeColumn`
- `scopeType`
- `scopeValue`
- `remark`

UI:

- Paginated table.
- Search by `scopeName` and `resourceCode`.
- Menu selector from `getMenuOnlyList`.

- [ ] **Step 2: API scope page requirements**

Use APIs:

- `getApiScopeList`
- `submitApiScope`
- `removeApiScopes`
- `getMenuOnlyList`

Fields:

- `menuId`
- `resourceCode`
- `scopeName`
- `scopePath`
- `scopeType`
- `remark`

UI:

- Paginated table.
- Search by `scopeName` and `resourceCode`.
- Menu selector from `getMenuOnlyList`.

- [ ] **Step 3: Static verification**

Run:

```powershell
rtk pwsh -NoProfile -Command "rg -n \"getDataScopeList|submitDataScope|removeDataScopes|scopeField|scopeClass|scopeColumn|scopeValue\" src/views/System/DataScopeManagement.vue; rg -n \"getApiScopeList|submitApiScope|removeApiScopes|scopePath|getMenuOnlyList\" src/views/System/ApiScopeManagement.vue"
```

Expected:

- The command exits with code `0`.
- Output includes CRUD API usage and required scope fields.

---

### Task 10: Build Verification

**Files:**

- All created and modified frontend files.

- [ ] **Step 1: Run build**

Run:

```powershell
rtk pwsh -NoProfile -Command "npm run build"
```

Expected:

- Exit code `0`.
- Vite build completes.
- If build fails because of import names from `@element-plus/icons-vue`, replace the missing icon with an already imported icon such as `Setting`, then rerun the build.

- [ ] **Step 2: Capture build output if needed**

If the executor wants to save logs, create:

```powershell
rtk pwsh -NoProfile -Command "New-Item -ItemType Directory -Force 'codex' | Out-Null; npm run build *> 'codex/system-management-build.log'"
```

Expected:

- `codex/system-management-build.log` exists.
- The log ends with successful Vite build output.

---

### Task 11: Manual Self-Test Flow

**Files:**

- Runtime app from `npm run dev`.
- Optional notes under `codex/system-management-smoke.md`.

- [ ] **Step 1: Start dev server**

Run:

```powershell
rtk pwsh -NoProfile -Command "npm run dev"
```

Expected:

- Vite starts and prints a local URL, usually `http://localhost:3000/bus/vls-ui/`.
- Keep the server running for manual testing.

- [ ] **Step 2: Navigation smoke test**

Open the Vite URL in a browser.

Verify:

- Top navigation includes `系统管理`.
- Clicking `系统管理` navigates to `/system/users`.
- Sidebar includes `用户管理`, `角色管理`, `菜单管理`, `部门管理`, `岗位管理`, `租户管理`, `数据权限`, `接口权限`.
- Each sidebar item navigates without a blank screen.

- [ ] **Step 3: Page smoke test**

For each page:

- Confirm the table or tree table renders.
- Confirm the first load either shows backend data or an empty table.
- Confirm browser console has no Vue runtime errors.
- Confirm search/reset controls do not throw errors.
- Confirm add/edit dialog opens and closes.
- Confirm required fields show validation before submit.

- [ ] **Step 4: Permission flow smoke test**

On `角色管理`:

- Select or use a row-level grant action for one role.
- Confirm `权限配置` dialog opens.
- Confirm it shows three tabs or sections: `菜单权限`, `数据权限`, `接口权限`.
- Confirm checked keys are loaded from `/system/menu/role-tree-keys`.
- Save without changing selections.
- Confirm success message appears and dialog closes.

- [ ] **Step 5: Mutation flow checks**

Use non-production test data only.

Check:

- Create a temporary post, edit it, then delete it.
- Create a temporary department under an existing tenant if safe, edit it, then delete it.
- Do not reset real user passwords unless the environment is explicitly a test environment.
- Do not delete built-in admin roles, root menus, root departments, or production tenants.

- [ ] **Step 6: Record smoke-test result**

If saving notes, create `codex/system-management-smoke.md` with this format:

```markdown
# System Management Smoke Test

Date:
Tester:
Frontend URL:
Backend base URL:

## Build

- Command: `npm run build`
- Result:

## Navigation

- 系统管理 top menu:
- 用户管理:
- 角色管理:
- 菜单管理:
- 部门管理:
- 岗位管理:
- 租户管理:
- 数据权限:
- 接口权限:

## Role Permission Flow

- Grant tree loaded:
- Checked keys loaded:
- Save result:

## Notes

- 
```

---

## Final Acceptance Checklist

- [ ] `npm run build` exits with code `0`.
- [ ] System API wrapper files exist under `src/api/system`.
- [ ] All eight Vue pages exist under `src/views/System`.
- [ ] `src/router/index.js` contains all eight `/system/*` routes.
- [ ] `src/layout/index.vue` contains the `系统管理` top menu and side-menu branch.
- [ ] Role management can open the permission grant dialog.
- [ ] Permission grant payload includes `roleIds`, `menuIds`, `dataScopeIds`, and `apiScopeIds`.
- [ ] User management can assign roles through `/system/user/grant`.
- [ ] Page style matches current VLStream table-page style.
- [ ] Temporary test files or logs, if any, are under `codex`.
