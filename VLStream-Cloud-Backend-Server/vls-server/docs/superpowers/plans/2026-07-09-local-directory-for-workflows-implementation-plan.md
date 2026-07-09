# Local Directory For Workflows Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move workflow, form, and work order user/department/role directory lookups from old external SSO/Auth APIs to the local RuoYi system user directory in the clean backend.

**Architecture:** Add one frontend local directory adapter under `src/api/system` that returns the legacy chooser field shape while calling local `/system/**` APIs. Migrate core chooser/name/dispatch components to that adapter. Replace the backend work order PDF/project-name external SSO department lookup with `ISysDeptService`.

**Tech Stack:** Vue 3, Vite, Element Plus, Axios request wrappers, Java 8, Spring Boot 2.7, RuoYi Flowable Plus, Sa-Token, MyBatis Plus, Node smoke scripts.

---

## File Structure

### Create

- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\api\system\directory.js`
  - Local directory API adapter. Calls `/system/user/selectUser`, `/system/user/{id}`, `/system/dept/list`, `/system/dept/{id}`, and `/system/role/list`.
  - Converts local RuoYi users/departments/roles into the old chooser fields (`user_id`, `user_name`, `dept_id`, `dept_name`, `son_dept`, `ruuid`, `name`).
  - Provides empty-success stubs for recent contacts, address book, and tag APIs so optional chooser tabs do not call external SSO.

- `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-migration-static-check.cjs`
  - Static regression check for old imports and backend external SSO department calls.

- `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-api-smoke.cjs`
  - Runtime API smoke test for local users, departments, roles, and adapter assumptions.

- `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\browser-local-directory-network-smoke-results.json`
  - Browser/network verification output, generated during verification.

### Modify

- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\ID2Name.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\ID2HeadPic.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\deptIdToName.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\personHome\searchPerson.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\personHome\addressSetingDialog.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\personHome\deptUserTree.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\personHome\recentlyUsePerson.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\personHome\myAlwaysUse.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\personHome\myTag.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\processui\flowProp\components\choosePersonPanel.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\processui\flowProp\components\choosePersonPanelNotifyNode.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\VForm\components\form-designer\setting-panel\address-setting-selectScope.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\VForm\components\form-designer\form-widget\field-widget\components\choosePersonOrDept.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\pages\processui\views\page\workOrderManage\newWorkOrderDispatch.vue`
  - Replace old `@/api/unifiedUsert/sso` and `@/api/unifiedUsert/apaasAuth` directory imports with `@/api/system/directory`.

- `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms-clean\apaas-workflowforms\ruoyi-system\src\main\java\com\ruoyi\workorder\service\impl\WorkOrderServiceImpl.java`
  - Inject `ISysDeptService`.
  - Remove `http.apaas-sso` field usage.
  - Resolve project name from local `sys_dept`.

---

### Task 1: Static Regression Test

**Files:**
- Create: `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-migration-static-check.cjs`

- [ ] **Step 1: Write the failing static check**

Create `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-migration-static-check.cjs`:

```js
const fs = require('fs');
const path = require('path');

const repoRoot = 'D:/work/ide/WorkSpace/VLStream-Web/VLStream-ui';
const backendRoot = 'D:/work/ide/WorkSpace/VLStream-Cloud/VLStream-Cloud-Backend-Server/apaas-workflowforms-clean/apaas-workflowforms';
const outputFile = 'D:/work/ide/WorkSpace/VLStream-Cloud/VLStream-Cloud-Backend-Server/vls-server/codex/local-directory-migration-static-check-results.json';

const frontendFiles = [
  'src/components/ID2Name.vue',
  'src/components/ID2HeadPic.vue',
  'src/components/deptIdToName.vue',
  'src/components/personHome/searchPerson.vue',
  'src/components/personHome/addressSetingDialog.vue',
  'src/components/personHome/deptUserTree.vue',
  'src/components/personHome/recentlyUsePerson.vue',
  'src/components/personHome/myAlwaysUse.vue',
  'src/components/personHome/myTag.vue',
  'src/components/processui/flowProp/components/choosePersonPanel.vue',
  'src/components/processui/flowProp/components/choosePersonPanelNotifyNode.vue',
  'src/components/VForm/components/form-designer/setting-panel/address-setting-selectScope.vue',
  'src/components/VForm/components/form-designer/form-widget/field-widget/components/choosePersonOrDept.vue',
  'src/pages/processui/views/page/workOrderManage/newWorkOrderDispatch.vue',
];

const forbiddenFrontendPatterns = [
  '@/api/unifiedUsert/sso',
  '@/api/unifiedUsert/apaasAuth',
  'config.gateWay + \\'apaas-sso/\\'',
  'config.gateWay + \\'apaas-auth\\'',
];

const backendFiles = [
  'ruoyi-system/src/main/java/com/ruoyi/workorder/service/impl/WorkOrderServiceImpl.java',
];

const forbiddenBackendPatterns = [
  'apaasSso + "/sso/v1/getDeptUser"',
  '@Value("${http.apaas-sso}")',
  'private String apaasSso',
];

const requiredAdapterExports = [
  'export function getUserList',
  'export function getDeptList',
  'export function getDeptUser',
  'export function deptUserList',
  'export function roleList',
  'export function usedSet',
  'export function usedGet',
  'export function addressList',
  'export function tagList',
];

const failures = [];

function read(file) {
  return fs.readFileSync(file, 'utf8');
}

function assertNoPatterns(base, files, patterns, label) {
  for (const relative of files) {
    const absolute = path.join(base, relative);
    const text = read(absolute);
    for (const pattern of patterns) {
      if (text.includes(pattern)) {
        failures.push({ label, file: absolute, pattern });
      }
    }
  }
}

function assertAdapterExists() {
  const adapter = path.join(repoRoot, 'src/api/system/directory.js');
  if (!fs.existsSync(adapter)) {
    failures.push({ label: 'frontend adapter', file: adapter, pattern: 'missing file' });
    return;
  }
  const text = read(adapter);
  for (const expected of requiredAdapterExports) {
    if (!text.includes(expected)) {
      failures.push({ label: 'frontend adapter export', file: adapter, pattern: expected });
    }
  }
  for (const forbidden of ['apaas-sso', 'apaas-auth', '@/api/unifiedUsert/sso', '@/api/unifiedUsert/apaasAuth']) {
    if (text.includes(forbidden)) {
      failures.push({ label: 'frontend adapter forbidden external dependency', file: adapter, pattern: forbidden });
    }
  }
}

assertAdapterExists();
assertNoPatterns(repoRoot, frontendFiles, forbiddenFrontendPatterns, 'frontend core directory consumer');
assertNoPatterns(backendRoot, backendFiles, forbiddenBackendPatterns, 'backend work order local directory');

const report = {
  generatedAt: new Date().toISOString(),
  passed: failures.length === 0,
  failures,
};
fs.writeFileSync(outputFile, JSON.stringify(report, null, 2));

if (!report.passed) {
  console.error(JSON.stringify(report, null, 2));
  process.exit(1);
}

console.log(JSON.stringify(report, null, 2));
```

- [ ] **Step 2: Run it to verify it fails on the current code**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "node 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-migration-static-check.cjs'"
```

Expected: FAIL. The report should include missing `src/api/system/directory.js`, old frontend imports from `@/api/unifiedUsert/sso` / `@/api/unifiedUsert/apaasAuth`, and the backend `apaasSso + "/sso/v1/getDeptUser"` call.

- [ ] **Step 3: Commit the failing regression script**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "git add -- 'codex/local-directory-migration-static-check.cjs' 'codex/local-directory-migration-static-check-results.json'; git commit -m 'test: add local directory migration static check'"
```

Expected: commit succeeds with only the new check and generated report staged.

---

### Task 2: Frontend Local Directory Adapter

**Files:**
- Create: `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\api\system\directory.js`

- [ ] **Step 1: Add the adapter**

Create `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\api\system\directory.js`:

```js
import request from '@/utils/request'
import { SINGLE_TENANT_ID } from './ruoyiCompat'

const DEFAULT_PAGE_SIZE = 999

function compact(value) {
  return Object.fromEntries(Object.entries(value).filter(([, item]) => item !== undefined && item !== null && item !== ''))
}

function asArray(value) {
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  if (value === undefined || value === null || value === '') return []
  return String(value).split(',').map((item) => item.trim()).filter(Boolean)
}

function pickRows(response) {
  if (Array.isArray(response?.rows)) return response.rows
  if (Array.isArray(response?.data?.list)) return response.data.list
  if (Array.isArray(response?.data)) return response.data
  if (Array.isArray(response)) return response
  return []
}

function directoryList(list, extra = {}) {
  return {
    code: 200,
    success: true,
    msg: '操作成功',
    data: {
      list,
      total: list.length,
      ...extra
    }
  }
}

function idOf(value) {
  if (value === undefined || value === null) return ''
  return String(value)
}

export function mapDirectoryUser(user = {}) {
  const userId = idOf(user.userId || user.user_id || user.id)
  const loginId = user.loginId || user.userName || user.account || user.login_id || ''
  const userName = user.nickName || user.realName || user.name || user.userName || loginId || userId
  const deptId = idOf(user.deptId || user.dept_id || user.dept?.deptId)
  const deptName = user.deptName || user.dept_name || user.dept?.deptName || ''
  const phone = user.phonenumber || user.phone || user.user_detail?.phone || ''
  return {
    ...user,
    id: userId,
    userId,
    user_id: userId,
    account: loginId,
    login_id: loginId,
    userName: user.userName || loginId,
    user_name: userName,
    nickName: user.nickName || userName,
    realName: user.realName || userName,
    name: userName,
    deptId,
    dept_id: deptId,
    deptName,
    dept_name: deptName,
    tenantId: user.tenantId || SINGLE_TENANT_ID,
    tenant_id: user.tenantId || SINGLE_TENANT_ID,
    photo: user.photo || user.avatar || '',
    avatar: user.avatar || user.photo || '',
    phone,
    phonenumber: phone,
    user_detail: {
      ...(user.user_detail || {}),
      phone
    },
    unique_login_id: {
      ...(user.unique_login_id || {}),
      login_id: loginId
    },
    dept_list: user.dept_list || (deptId ? [{ dept_id: deptId, deptinfo: { dept_name: deptName } }] : [])
  }
}

export function mapDirectoryDept(dept = {}) {
  const deptId = idOf(dept.deptId || dept.dept_id || dept.id)
  const deptCode = idOf(dept.deptCode || dept.dept_code || deptId)
  const parentId = idOf(dept.parentId || dept.parent_id || '0')
  const deptName = dept.deptName || dept.dept_name || dept.fullName || dept.label || dept.name || deptId
  return {
    ...dept,
    id: deptId,
    deptId,
    dept_id: deptId,
    deptCode,
    dept_code: deptCode,
    parentId,
    parent_id: parentId,
    deptName,
    dept_name: deptName,
    fullName: dept.fullName || deptName,
    label: dept.label || deptName,
    name: dept.name || deptName,
    tenantId: dept.tenantId || SINGLE_TENANT_ID,
    tenant_id: dept.tenantId || SINGLE_TENANT_ID,
    dept_photo: dept.dept_photo || '',
    son_dept: Array.isArray(dept.son_dept) ? dept.son_dept : [],
    users: Array.isArray(dept.users) ? dept.users : []
  }
}

export function mapDirectoryRole(role = {}) {
  const roleId = idOf(role.roleId || role.role_id || role.id || role.ruuid)
  const roleName = role.roleName || role.role_name || role.name || roleId
  const roleKey = role.roleKey || role.roleAlias || role.role_alias || ''
  return {
    ...role,
    id: roleId,
    roleId,
    role_id: roleId,
    ruuid: roleId,
    name: roleName,
    roleName,
    role_name: roleName,
    roleKey,
    roleAlias: roleKey,
    tenantId: role.tenantId || SINGLE_TENANT_ID,
    tenant_id: role.tenantId || SINGLE_TENANT_ID
  }
}

async function loadUsersByIds(ids) {
  const users = await Promise.all(ids.map((id) => request({
    url: `/system/user/${encodeURIComponent(id)}`,
    method: 'get'
  }).then((response) => response?.data?.user || response?.data || null).catch(() => null)))
  return users.filter(Boolean).map(mapDirectoryUser)
}

async function loadDeptsByIds(ids) {
  const depts = await Promise.all(ids.map((id) => request({
    url: `/system/dept/${encodeURIComponent(id)}`,
    method: 'get'
  }).then((response) => response?.data || null).catch(() => null)))
  return depts.filter(Boolean).map(mapDirectoryDept)
}

export async function getUserList(params = {}) {
  const ids = asArray(params.user_id || params.userId || params.id)
  if (ids.length > 0) {
    return directoryList(await loadUsersByIds(ids))
  }

  const keyword = params.keyword || params.user_name || params.name || params.realName || params.nickName
  const response = await request({
    url: '/system/user/selectUser',
    method: 'get',
    params: compact({
      pageNum: params.pageNum || params.current || params.page || 1,
      pageSize: params.pageSize || params.size || params.pagesize || DEFAULT_PAGE_SIZE,
      userName: params.account || params.userName || params.login_id,
      nickName: keyword,
      deptId: params.dept_id || params.deptId,
      status: params.status === undefined ? undefined : String(params.status)
    })
  })
  const list = pickRows(response).map(mapDirectoryUser)
  return directoryList(list, { total: Number(response?.total || list.length) })
}

export async function getDeptList(params = {}) {
  const ids = asArray(params.dept_id || params.deptId || params.id)
  if (ids.length > 0) {
    return directoryList(await loadDeptsByIds(ids))
  }

  const keyword = params.keyword || params.dept_name || params.deptName || params.fullName || params.name
  const response = await request({
    url: '/system/dept/list',
    method: 'get',
    params: compact({
      deptName: keyword,
      parentId: params.parent_id || params.parentId,
      status: params.status === undefined ? undefined : String(params.status)
    })
  })
  return directoryList(pickRows(response).map(mapDirectoryDept))
}

function buildDeptUserTree(depts, users) {
  const nodeById = new Map()
  const roots = []

  depts.map(mapDirectoryDept).forEach((dept) => {
    nodeById.set(dept.dept_id, { ...dept, son_dept: [], users: [] })
  })

  users.map(mapDirectoryUser).forEach((user) => {
    const node = nodeById.get(user.dept_id)
    if (node) node.users.push(user)
  })

  nodeById.forEach((node) => {
    const parent = nodeById.get(node.parent_id)
    if (parent && parent.dept_id !== node.dept_id) {
      parent.son_dept.push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
}

export async function getDeptUser(params = {}) {
  const [deptResponse, userResponse] = await Promise.all([
    getDeptList(params),
    getUserList({ ...params, page: 1, pagesize: DEFAULT_PAGE_SIZE, pageSize: DEFAULT_PAGE_SIZE })
  ])
  const depts = deptResponse.data.list || []
  const users = userResponse.data.list || []
  return directoryList(buildDeptUserTree(depts, users), { deptList: depts, userList: users })
}

export function deptUserList(params = {}) {
  return getUserList(params)
}

export async function roleList(params = {}) {
  const response = await request({
    url: '/system/role/list',
    method: 'get',
    params: compact({
      pageNum: params.pageNum || params.page || 1,
      pageSize: params.pageSize || params.pagesize || DEFAULT_PAGE_SIZE,
      roleName: params.roleName || params.name,
      roleKey: params.roleAlias || params.roleKey
    })
  })
  const list = pickRows(response).map(mapDirectoryRole)
  return directoryList(list, { total: Number(response?.total || list.length) })
}

export function usedSet() {
  return Promise.resolve({ code: 200, success: true, msg: '操作成功', data: true })
}

export function usedDel() {
  return Promise.resolve({ code: 200, success: true, msg: '操作成功', data: true })
}

export function usedGet() {
  return Promise.resolve(directoryList([]))
}

export function addressUserList() {
  return Promise.resolve(directoryList([]))
}

export function addressList() {
  return Promise.resolve(directoryList([]))
}

export function tagList() {
  return Promise.resolve(directoryList([]))
}

export function tagUserList() {
  return Promise.resolve(directoryList([]))
}
```

- [ ] **Step 2: Run the static check**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "node 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-migration-static-check.cjs'"
```

Expected: FAIL still, but failures should no longer include missing `src/api/system/directory.js` or missing adapter exports. Remaining failures should be old component imports and backend `apaasSso`.

- [ ] **Step 3: Commit the adapter**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "git -C 'D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui' add -- 'src/api/system/directory.js'; git -C 'D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui' commit -m 'feat: add local workflow directory adapter'"
```

Expected: commit succeeds in the frontend repository with only `src/api/system/directory.js`.

---

### Task 3: Migrate Frontend Directory Consumers

**Files:**
- Modify the frontend files listed in the File Structure section.

- [ ] **Step 1: Replace SSO user/department imports**

In all files currently importing from `@/api/unifiedUsert/sso`, replace directory-related imports with:

```js
import {
  getUserList,
  getDeptList,
  getDeptUser,
  deptUserList,
  usedSet,
  usedGet,
  usedDel,
  addressList,
  addressUserList,
  tagList,
  tagUserList
} from '@/api/system/directory'
```

Use only the named functions each file actually needs. For example:

```js
import { getUserList } from '@/api/system/directory'
```

```js
import { getDeptList } from '@/api/system/directory'
```

```js
import { getDeptUser, usedSet } from '@/api/system/directory'
```

- [ ] **Step 2: Replace flow role imports**

In both flow chooser files:

- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\processui\flowProp\components\choosePersonPanel.vue`
- `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\processui\flowProp\components\choosePersonPanelNotifyNode.vue`

Replace:

```js
import { roleList } from '@/api/unifiedUsert/apaasAuth'
```

with:

```js
import { roleList } from '@/api/system/directory'
```

Leave this existing consumer code unchanged because the adapter returns `ruuid` and `name`:

```js
roleList(data).then(res => {
  roleListArr.value = res.data.list || []
})
```

- [ ] **Step 3: Hide the external address-book management link**

In `D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui\src\components\personHome\addressSetingDialog.vue`, hide the personal-center address-book link while preserving the confirm/cancel buttons.

Add this script constant near `let active = ref<any>(1)`:

```ts
const showAddressBookSettings = false
```

Change the address-book link wrapper from:

```vue
<div class="saveTi flexRowAC" @click="goToMyAddress">
```

to:

```vue
<div v-if="showAddressBookSettings" class="saveTi flexRowAC" @click="goToMyAddress">
```

Keep `goToMyAddress` in the file for now so this is a narrow UI hide instead of a broader cleanup.

- [ ] **Step 4: Run the static check**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "node 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-migration-static-check.cjs'"
```

Expected: FAIL only on backend `WorkOrderServiceImpl.java` `apaasSso` patterns. No frontend core component should still import `@/api/unifiedUsert/sso` or `@/api/unifiedUsert/apaasAuth`.

- [ ] **Step 5: Build the frontend**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "npm run build" 
```

Working directory:

```text
D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui
```

Expected: PASS. Existing Sass and chunk-size warnings are acceptable. Import resolution errors are not acceptable.

- [ ] **Step 6: Commit frontend migration**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "git -C 'D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui' add -- 'src/components/ID2Name.vue' 'src/components/ID2HeadPic.vue' 'src/components/deptIdToName.vue' 'src/components/personHome/searchPerson.vue' 'src/components/personHome/addressSetingDialog.vue' 'src/components/personHome/deptUserTree.vue' 'src/components/personHome/recentlyUsePerson.vue' 'src/components/personHome/myAlwaysUse.vue' 'src/components/personHome/myTag.vue' 'src/components/processui/flowProp/components/choosePersonPanel.vue' 'src/components/processui/flowProp/components/choosePersonPanelNotifyNode.vue' 'src/components/VForm/components/form-designer/setting-panel/address-setting-selectScope.vue' 'src/components/VForm/components/form-designer/form-widget/field-widget/components/choosePersonOrDept.vue' 'src/pages/processui/views/page/workOrderManage/newWorkOrderDispatch.vue'; git -C 'D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui' commit -m 'feat: use local directory in workflow forms'"
```

Expected: commit succeeds in the frontend repository with only frontend migration files staged.

---

### Task 4: Backend Work Order Local Department Lookup

**Files:**
- Modify: `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms-clean\apaas-workflowforms\ruoyi-system\src\main\java\com\ruoyi\workorder\service\impl\WorkOrderServiceImpl.java`

- [ ] **Step 1: Add local department service imports**

Add imports near the existing `SysUser` and system service imports:

```java
import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.system.service.ISysDeptService;
```

- [ ] **Step 2: Inject local department service**

In the final field block, add:

```java
private final ISysDeptService sysDeptService;
```

Place it near:

```java
private final ISysUserService sysUserService;
```

- [ ] **Step 3: Remove the external SSO field**

Delete this field:

```java
@Value("${http.apaas-sso}")
private String apaasSso;
```

Keep `workFlowFormsUrl`; it is still used for work order type lookup.

- [ ] **Step 4: Add a local project-name helper**

Add this helper method near `addBasicInfo`:

```java
private String resolveLocalProjectName(String projectId) {
    if (projectId == null || projectId.trim().isEmpty()) {
        return "-";
    }
    try {
        SysDeptView dept = sysDeptService.selectDeptById(projectId);
        if (dept == null || dept.getDeptName() == null || dept.getDeptName().trim().isEmpty()) {
            return "-";
        }
        return dept.getDeptName();
    } catch (Exception e) {
        log.warn("本地部门信息获取失败, projectId={}", projectId, e);
        return "-";
    }
}
```

- [ ] **Step 5: Replace the external SSO block in `addBasicInfo`**

Replace the whole block from:

```java
OkHttpClient ssoClient = new OkHttpClient().newBuilder().build();
MediaType mediaType = MediaType.parse("application/json");
RequestBody body = RequestBody.create(
        mediaType,
        String.format(
                "{\"accessToken\":\"%s\"," + "\"tenant_id\":\"%s\",\"status\":1}",
                AuthorizationInterceptor.getToken(), workOrder.getTenantId()));
Request ssoRequest = new Request.Builder()
        .url(apaasSso + "/sso/v1/getDeptUser")
        .method("POST", body)
        .addHeader("AccessToken", AuthorizationInterceptor.getToken())
        .build();
try (Response response = ssoClient.newCall(ssoRequest).execute()) {
    ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false);
    // 定义响应结构
    ApiResponse<ApiResponse.DeptResponse> apiResponse = objectMapper.readValue(
            response.body().string(),
            new TypeReference<ApiResponse<ApiResponse.DeptResponse>>() {
            });

    if (apiResponse.getCode() == 200 && apiResponse.getData() != null) {
        List<ApiResponse.Dept> deptList = apiResponse.getData().getDeptList();
        String projectName = deptList.stream()
                .filter(dept -> dept.getDeptId().equals(workOrder.getProjectId()))
                .map(dept -> dept.getDeptName())
                .findFirst().orElse("-");
        table.addCell(createValueCell(projectName, font));
    }
} catch (IOException e) {
    throw new RuntimeException("部门信息获取失败", e);
}
```

with:

```java
table.addCell(createValueCell(resolveLocalProjectName(workOrder.getProjectId()), font));
```

- [ ] **Step 6: Run the static check**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "node 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-migration-static-check.cjs'"
```

Expected: PASS. The result file `codex/local-directory-migration-static-check-results.json` should contain `"passed": true`.

- [ ] **Step 7: Compile backend with the correct JDK**

First confirm Java version:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "Select-String -Path 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms-clean\apaas-workflowforms\pom.xml' -Pattern '<java.version>'"
```

Expected: `<java.version>1.8</java.version>`.

Then compile with Java 8:

```powershell
rtk pwsh -NoLogo -NoProfile -Command '$env:JAVA_HOME="C:\Users\oort\.jdks\corretto-1.8.0_442"; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -pl ruoyi-admin -am -DskipTests compile'
```

Working directory:

```text
D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms-clean\apaas-workflowforms
```

Expected: PASS.

- [ ] **Step 8: Commit backend migration**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "git -C 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms-clean\apaas-workflowforms' add -- 'ruoyi-system/src/main/java/com/ruoyi/workorder/service/impl/WorkOrderServiceImpl.java'; git -C 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms-clean\apaas-workflowforms' commit -m 'fix: use local dept lookup for work orders'"
```

Expected: commit succeeds in the clean backend repository with only `WorkOrderServiceImpl.java`.

---

### Task 5: Local Directory API Smoke Test

**Files:**
- Create: `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-api-smoke.cjs`

- [ ] **Step 1: Add the API smoke test**

Create `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-api-smoke.cjs`:

```js
const fs = require('fs');
const { sm2 } = require('D:/work/ide/WorkSpace/VLStream-Web/VLStream-ui/node_modules/sm-crypto');

const baseUrl = process.env.VLS_BACKEND_URL || 'http://127.0.0.1:18080';
const outputFile = 'D:/work/ide/WorkSpace/VLStream-Cloud/VLStream-Cloud-Backend-Server/vls-server/codex/local-directory-api-smoke-results.json';
const publicKey = '049787e408dea94acb3655acc5a7c7c7010bb9f140c84926c667ea616366082a118141c8dcb3e78a9d85d64fb765a250ff73448b18938f2219b94f782e28e1df64';

const results = [];
let token = '';

function push(name, ok, details) {
  results.push({ name, ok, details });
  console.log(`${ok ? 'PASS' : 'FAIL'} ${name}`);
  if (!ok) console.log(JSON.stringify(details, null, 2));
}

function headers() {
  const bearer = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
  return {
    Authorization: bearer,
    authorization: bearer,
    'blade-auth': bearer,
    accesstoken: token,
    requesttype: 'app',
    appid: '6551b0147c4649a894e86bf8de248da4',
    secretkey: '58f9eeefc65f4b318204ba21f39a8861',
  };
}

async function request(name, method, url, expected) {
  const res = await fetch(`${baseUrl}${url}`, { method, headers: token ? headers() : {
    Authorization: 'Basic c2FiZXI6c2FiZXJfc2VjcmV0',
    requesttype: 'app',
    appid: '6551b0147c4649a894e86bf8de248da4',
    secretkey: '58f9eeefc65f4b318204ba21f39a8861',
  } });
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch (_) {
    data = text;
  }
  const ok = expected(data, res);
  push(name, ok, { status: res.status, data });
  if (!ok) throw new Error(`${name} failed`);
  return data;
}

async function main() {
  try {
    const encryptedPassword = sm2.doEncrypt('Codex@123456', publicKey, 0);
    const params = new URLSearchParams({
      grantType: 'password',
      tenantId: '000000',
      account: 'admin',
      password: encryptedPassword,
    });
    const login = await request('login admin', 'POST', `/blade-auth/token?${params.toString()}`, (data, res) => res.ok && data?.code === 200 && data?.data?.accessToken);
    token = login.data.accessToken;

    const users = await request('list selectable local users', 'GET', '/system/user/selectUser?pageNum=1&pageSize=20', (data, res) => res.ok && data?.code === 200 && Array.isArray(data?.rows));
    const firstUser = users.rows[0];
    push('selectable user has local id/name fields', !!(firstUser?.userId && (firstUser.nickName || firstUser.userName)), firstUser);

    if (firstUser?.userId) {
      await request('read local user detail', 'GET', `/system/user/${encodeURIComponent(firstUser.userId)}`, (data, res) => res.ok && data?.code === 200 && data?.data?.user?.userId);
    }

    const depts = await request('list local departments', 'GET', '/system/dept/list', (data, res) => res.ok && data?.code === 200 && Array.isArray(data?.data));
    const firstDept = depts.data[0];
    push('department has local id/name fields', !!(firstDept?.deptId && firstDept.deptName), firstDept);

    if (firstDept?.deptId) {
      await request('read local department detail', 'GET', `/system/dept/${encodeURIComponent(firstDept.deptId)}`, (data, res) => res.ok && data?.code === 200 && data?.data?.deptId);
    }

    const roles = await request('list local roles', 'GET', '/system/role/list?pageNum=1&pageSize=20', (data, res) => res.ok && data?.code === 200 && Array.isArray(data?.rows));
    const firstRole = roles.rows[0];
    push('role has local id/name fields', !!(firstRole?.roleId && firstRole.roleName), firstRole);
  } finally {
    const report = {
      baseUrl,
      generatedAt: new Date().toISOString(),
      passed: results.every((item) => item.ok),
      results,
    };
    fs.writeFileSync(outputFile, JSON.stringify(report, null, 2));
    if (!report.passed) process.exit(1);
  }
}

main().catch((error) => {
  push('unexpected error', false, { message: error.message, stack: error.stack });
  fs.writeFileSync(outputFile, JSON.stringify({
    baseUrl,
    generatedAt: new Date().toISOString(),
    passed: false,
    results,
  }, null, 2));
  process.exit(1);
});
```

- [ ] **Step 2: Run the API smoke test**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "node 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-api-smoke.cjs'"
```

Expected: PASS and `codex/local-directory-api-smoke-results.json` contains `"passed": true`.

- [ ] **Step 3: Commit smoke test**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "git add -- 'codex/local-directory-api-smoke.cjs' 'codex/local-directory-api-smoke-results.json' 'codex/local-directory-migration-static-check-results.json'; git commit -m 'test: verify local directory APIs'"
```

Expected: commit succeeds in `vls-server` with only local test artifacts staged.

---

### Task 6: Real Frontend/Backend Verification

**Files:**
- Generate: `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\browser-local-directory-network-smoke-results.json`

- [ ] **Step 1: Restart backend if needed**

Run package/restart using existing scripts:

```powershell
rtk pwsh -NoLogo -NoProfile -File 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\run-backend-package.ps1'
rtk pwsh -NoLogo -NoProfile -File 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\start-backend-18080.ps1'
```

Expected: backend runs on `http://127.0.0.1:18080`.

- [ ] **Step 2: Restart frontend if needed**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -File 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\start-frontend-3002.ps1'
```

Expected: frontend runs on `http://127.0.0.1:3002`.

- [ ] **Step 3: Use the in-app browser to verify key routes**

Open these routes with a valid admin token:

```text
http://127.0.0.1:3002/bus/vls-ui/system/users
http://127.0.0.1:3002/bus/vls-ui/system/roles
http://127.0.0.1:3002/bus/vls-ui/system/depts
http://127.0.0.1:3002/bus/vls-ui/active-safety/settings/work-orders
http://127.0.0.1:3002/bus/vls-ui/active-safety/work-orders/pending
http://127.0.0.1:3002/bus/vls-ui/active-safety/work-orders/my
```

Expected:

- Pages render without blocking errors.
- Browser console has no new `apaas-sso` or `apaas-auth` user/dept request failures.
- Network requests for chooser user/dept/role data go to `/system/user/selectUser`, `/system/dept/list`, `/system/user/{id}`, `/system/dept/{id}`, or `/system/role/list`.

- [ ] **Step 4: Exercise chooser UI**

In a page that opens `addressSetingDialog.vue` or `ChoosePerson`, perform:

1. Open personnel selection.
2. Select a local user from organization tree or user search.
3. Select a local department if the dialog mode allows departments.
4. Confirm.

Expected:

- Selected user cards show local display name from `nickName` or `userName`.
- Selected department cards show local `deptName`.
- No request goes to `/apaas-sso/sso/v1/getUserList`, `/apaas-sso/sso/v1/getDeptList`, `/apaas-sso/sso/v1/getDeptUser`, or `/apaas-auth/admin/v1/roleList`.

- [ ] **Step 5: Save browser verification report**

Write `D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\browser-local-directory-network-smoke-results.json` with this shape:

```json
{
  "generatedAt": "2026-07-09T00:00:00.000Z",
  "frontendUrl": "http://127.0.0.1:3002",
  "backendUrl": "http://127.0.0.1:18080",
  "passed": true,
  "checkedRoutes": [],
  "forbiddenRequests": [],
  "consoleErrors": []
}
```

Use the actual timestamp, checked routes, and captured errors.

- [ ] **Step 6: Final full verification**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "node 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-migration-static-check.cjs'"
rtk pwsh -NoLogo -NoProfile -Command "node 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\vls-server\codex\local-directory-api-smoke.cjs'"
rtk pwsh -NoLogo -NoProfile -Command "npm run build"
```

For `npm run build`, use working directory:

```text
D:\work\ide\WorkSpace\VLStream-Web\VLStream-ui
```

Then compile backend after reading `<java.version>`:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "Select-String -Path 'D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms-clean\apaas-workflowforms\pom.xml' -Pattern '<java.version>'"
rtk pwsh -NoLogo -NoProfile -Command '$env:JAVA_HOME="C:\Users\oort\.jdks\corretto-1.8.0_442"; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -pl ruoyi-admin -am -DskipTests compile'
```

For Maven, use working directory:

```text
D:\work\ide\WorkSpace\VLStream-Cloud\VLStream-Cloud-Backend-Server\apaas-workflowforms-clean\apaas-workflowforms
```

Expected: all checks pass.

- [ ] **Step 7: Commit verification artifacts**

Run:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "git add -- 'codex/local-directory-api-smoke-results.json' 'codex/local-directory-migration-static-check-results.json' 'codex/browser-local-directory-network-smoke-results.json'; git commit -m 'test: record local directory verification'"
```

Expected: commit succeeds in `vls-server` with only verification output staged.

---

## Self-Review

Spec coverage:

- Frontend core user/dept directory calls move off old SSO: Task 2 and Task 3.
- Backend work order external SSO department lookup moves local: Task 4.
- Role candidate list moves off `apaas-auth`: Task 3.
- Old external ID compatibility remains out of scope: adapter and backend only use local IDs.
- Optional recent/contact/tag/address capabilities are degraded to empty local-success stubs: Task 2 and Task 3.
- Tests live under `vls-server\codex`: Task 1, Task 5, Task 6.
- Real frontend/backend verification is included: Task 6.

Placeholder scan:

- The plan contains no incomplete sections, no unspecified file paths, and no open-ended test commands.

Type and field consistency:

- Adapter exports match current component function names.
- Adapter user fields include both local RuoYi names and legacy chooser aliases.
- Adapter department fields include both local RuoYi names and legacy tree aliases.
- Flow role options keep `ruuid` and `name`, so existing `<el-option>` bindings remain valid.
