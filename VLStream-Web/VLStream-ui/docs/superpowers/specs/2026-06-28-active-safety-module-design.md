# Active Safety Module Design

## Goal

Add a static `主动安全` top-level module to `VLStream-ui` that reproduces the selected reference menus and page content inside the current application shell.

The existing global header in `src/layout/index.vue` remains in use. The reference system header with `陵水数投`, `事件管理`, `运营管理平台`, notification icons, avatar, and username is explicitly out of scope.

## Current Context

The frontend is a Vue 3, Vite, Element Plus application.

Static routes live in `src/router/index.js`. The global top navigation, sidebar selection, and `menuRoutesMap` live in `src/layout/index.vue`.

The current project has no local implementation for the reference active-safety, work-order, or active-safety settings pages. `src/views/DecisionAI/EventManagement.vue` is a separate existing page and should not be replaced.

The reference page uses Element Plus menu, table, tree, tabs, pagination, and simple settings controls. The new module should use Element Plus directly and keep implementation local to `src/views/ActiveSafety`.

## Confirmed Scope

Add a new top-level menu item:

- `主动安全`

Place it immediately after the existing `决策式AI` top menu.

When users click `主动安全`, open `事件管理 > 主动安全` by default.

Add this sidebar structure inside the `主动安全` top-level module:

- `事件管理`
  - `主动安全`
- `工单管理`
  - `我的工单`
  - `待办工单`
  - `已办工单`
  - `可接工单`
- `系统设置`
  - `主动安全`
  - `工单设置`

The following reference entries are excluded:

- `概览`
- `事件管理 > 事件拍传`
- `系统设置 > 事件拍传`
- `系统设置 > 事件类型`

## Chosen Approach

Use a dedicated static module under `src/views/ActiveSafety`.

This keeps the new pages separate from current `DecisionAI`, `VideoAggregation`, `System`, and existing event-management pages. It also allows the module to reproduce the reference content area without changing the current global application shell.

## Rejected Approaches

### Replace Existing Event Management

Do not replace `src/views/DecisionAI/EventManagement.vue`. That route already serves the current `决策式AI` menu and has different behavior and styling.

### Full Reference Layout

Do not copy the reference system header. The user clarified that this header area is not required.

### Backend Integration

Do not call the reference site's APIs or infer remote endpoints. Use local mock data for this pass.

## Navigation

Add routes under the existing authenticated `Layout` children:

- `/active-safety/events/secure` -> `事件管理 > 主动安全`
- `/active-safety/work-orders/my` -> `工单管理 > 我的工单`
- `/active-safety/work-orders/pending` -> `工单管理 > 待办工单`
- `/active-safety/work-orders/completed` -> `工单管理 > 已办工单`
- `/active-safety/work-orders/claimable` -> `工单管理 > 可接工单`
- `/active-safety/settings/secure` -> `系统设置 > 主动安全`
- `/active-safety/settings/work-orders` -> `系统设置 > 工单设置`

Add these routes to `menuRoutesMap` under the new top-menu key, for example `active-safety`.

The active top menu should resolve from all `/active-safety/**` routes.

## Pages And Content

### Event Secure Page

The `事件管理 > 主动安全` page reproduces the reference `/secure` page content.

Main content:

- A left filter panel with `区域`, `分组`, and `标签` tabs.
- A delete action.
- A status filter with `全部`, `正在处理`, and `已完成`.
- An Element Plus table with these headers:
  - `序号`
  - `事件时间`
  - `状态`
  - `事件位置`
  - `事件类型`
  - `抓拍照片`
  - `录制视频`
  - `设备名称`
  - `设备编号`
  - `事件描述`
  - `标签`
  - `完成时间`
  - `操作`
- Empty state and pagination should match the reference when filters produce no rows.

Use local mock rows. Rows should include at least a mix of `正在处理` and `已完成` records so status filtering can be verified.

### Work Order Pages

Use one shared work-order table component with per-page column configuration.

`我的工单` headers:

- `序号`
- `工单编号`
- `工单标题`
- `工单来源`
- `流程版本`
- `发起人`
- `工单状态`
- `提交时间`
- `操作`

`待办工单` headers:

- `序号`
- `工单编号`
- `工单标题`
- `所属分类`
- `工单来源`
- `流程版本`
- `发起人`
- `提交时间`
- `操作`

`已办工单` headers:

- `序号`
- `工单编号`
- `工单标题`
- `所属分类`
- `工单来源`
- `流程版本`
- `发起人`
- `工单状态`
- `接收时间`
- `操作`

`可接工单` headers:

- `序号`
- `工单编号`
- `工单标题`
- `所属分类`
- `工单来源`
- `流程版本`
- `发起人`
- `接收时间`
- `操作`

The first pass can use empty table data to match the observed reference pages, with pagination showing `共 0 条`. If mock rows are added for interaction checks, keep them easy to filter and visually close to the reference.

### Secure Settings Page

The `系统设置 > 主动安全` page reproduces the reference `/sysSecure` content.

Main content:

- Three settings rows:
  - `区域 自动派单`
  - `分组 自动派单`
  - `标签 自动派单`
- A compact selector or switch-like control for each setting.
- A simple `全部` control, matching the reference text.

The controls are local-only and should not call any backend.

### Work Order Settings Page

The `系统设置 > 工单设置` page reproduces the reference `/sysWork` content.

Main content:

- A `新建` action.
- At least one local model card or row named `环保模型`.
- A simple create/edit dialog can be shown for light interaction, but persistence is local mock state only.

## Visual Style

Inside the `主动安全` module content area:

- Match the reference sidebar grouping and blue active state as closely as possible while remaining inside the existing global shell.
- Use a pale gray page background and white content panels.
- Use Element Plus table styling with light header background, striped rows, small status tags, and reference-like pagination.
- Keep content dense and operational.
- Avoid marketing layouts, hero sections, decorative images, and unrelated dashboard cards.

Because the global header is out of scope, visual matching starts below the current app header.

## Interactions

Use light local interaction:

- Top menu switches to the `主动安全` module.
- Sidebar routes navigate normally.
- Sidebar groups open and show only included child routes.
- Event page status tabs filter local mock data.
- Search/filter controls accept input locally.
- Pagination changes page state locally.
- Delete, new, and settings actions show local feedback or a simple local dialog.

No backend calls are required.

## Data Flow

Create static mock data under the active-safety module, for example:

- `src/views/ActiveSafety/data/mockData.js`

Keep data close to the pages using it. Do not add a global store unless a later requirement needs cross-module persistence.

Use computed values in components for filtering, pagination, and page-specific table rows.

## Testing

Generated test or verification files must be placed under project-root `codex/`.

Add static verification scripts under `codex/` to check:

- `src/layout/index.vue` contains the `主动安全` top menu after `决策式AI`.
- `src/router/index.js` contains all `/active-safety/**` routes.
- The active-safety menu config does not include `概览`, `事件拍传`, or `事件类型`.
- Active-safety components contain the required table headers and settings labels.

Run build verification:

```powershell
rtk pwsh -NoLogo -NoProfile -Command "npm run build"
```

This project does not contain a root `pom.xml`, so no JDK selection is required for this frontend-only implementation.

If browser screenshots or smoke-test notes are produced, store them under `codex/`.

## Acceptance Criteria

- The top navigation shows `主动安全` immediately after `决策式AI`.
- Clicking `主动安全` opens `事件管理 > 主动安全`.
- The module sidebar contains exactly the confirmed groups and children.
- The excluded menu entries are not visible in the new module.
- The seven routes render without console-level runtime failures.
- The event secure page visually matches the reference content area closely enough for menu, filters, table, empty/data state, and pagination.
- The work-order pages show the correct reference table structures.
- The two settings pages show the reference labels and local-only controls/content.
- `npm run build` succeeds.

## Out Of Scope

- Reproducing the reference top header.
- Reproducing `概览`.
- Reproducing any `事件拍传` page.
- Reproducing `事件类型`.
- Calling reference remote APIs.
- Backend changes.
- Replacing current `DecisionAI/EventManagement.vue`.
