import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { canAccessPage, filterMenuRoutes, isManagedMenuRoute, permissionForPage } from '../src/utils/menuAccess.mjs'
import { hiddenGrantedMenuIds, selectedMenuIds, orderedVisibleGrants, changedVisibleGrants, preserveGrantableHiddenGrants } from '../src/utils/menuGrant.mjs'
import TreeStore from '../node_modules/element-plus/es/components/tree/src/model/tree-store.mjs'
import { cameraRtcIdFromUrl, squareCameraRtcConfig } from '../src/utils/squareCameraRtc.mjs'

test('each application route is either deliberately public or has a page permission', async () => {
  const source = await readFile(new URL('../src/router/index.js', import.meta.url), 'utf8')
  const routes = [...source.matchAll(/path:\s*'([^']+)'/g)].map(match => match[1])
  const baseline = new Set(['/login', '/', '/workspace', '/user-profile', '/cloud-platform', '/compute-safety'])
  assert.deepEqual(routes.filter(path => !baseline.has(path) && !isManagedMenuRoute(path)), [])
})

test('existing permissions remain valid and new page keys match their full paths', () => {
  assert.equal(permissionForPage('/system/roles'), 'system:role:list')
  assert.equal(permissionForPage('/event-management'), 'active-safety:events:list')
  assert.equal(permissionForPage('/rtsp/rtspDevice'), 'vls:menu:rtsp:rtspDevice')
  assert.equal(canAccessPage('/system/roles', ['system:role:list']), true)
  assert.equal(canAccessPage('/system/roles', ['vls:menu:system:roles']), false)
  assert.equal(canAccessPage('/system/roles', ['*:*:*']), true)
  assert.equal(canAccessPage('/system/roles', null), false)
})

test('filtered navigation preserves only granted leaves and their ancestors', () => {
  const tree = [{ path: 'group', children: [
    { path: '/algorithm-management' },
    { path: 'training', children: [{ path: '/data-management' }, { path: '/algorithm-training' }] }
  ] }]
  assert.deepEqual(filterMenuRoutes(tree, ['vls:menu:data-management']), [
    { path: 'group', children: [{ path: 'training', children: [{ path: '/data-management' }] }] }
  ])
  assert.deepEqual(filterMenuRoutes(tree, []), [])
})

test('deep links inherit the required visible page permission', () => {
  assert.equal(canAccessPage('/channel/list/index/:deviceId/:parentChannelId?', ['vls:menu:gbmanger:device']), true)
  assert.equal(canAccessPage('/channel/list/index/:deviceId/:parentChannelId?', ['vls:menu:video-square']), false)
  assert.equal(canAccessPage('/dataset-annotation', ['vls:menu:data-management']), true)
  assert.equal(canAccessPage('/dataset-annotation', ['vls:menu:algorithm-training']), false)
  assert.equal(canAccessPage('/workOrderDetails', ['active-safety:workorder:pending']), true)
  assert.equal(canAccessPage('/workOrderDetails', []), false)
})

test('legacy workflow list grants expose only their own work-order page and details', () => {
  const pages = [
    ['/active-safety/work-orders/my', 'active-safety:workorder:my', 'workflow:workorder:selectPageOwnWorkOrderList'],
    ['/active-safety/work-orders/pending', 'active-safety:workorder:pending', 'workflow:workorder:selectPageTodoWorkOrderList'],
    ['/active-safety/work-orders/completed', 'active-safety:workorder:completed', 'workflow:workorder:selectPageFinishedWorkOrderList'],
    ['/active-safety/work-orders/claimable', 'active-safety:workorder:claimable', 'workflow:workorder:claimWorkOrderList']
  ]
  const tree = [{ path: 'work-orders', children: pages.map(([path]) => ({ path })) }]
  for (const [path, page, action] of pages) {
    assert.equal(canAccessPage(path, [action]), true)
    assert.equal(canAccessPage('/workOrderDetails', [action]), true)
    assert.equal(canAccessPage(path, [page]), true)
    assert.equal(canAccessPage(path, [page, action]), true)
    assert.equal(canAccessPage(path, []), false)
    assert.deepEqual(filterMenuRoutes(tree, [action]), [{ path: 'work-orders', children: [{ path }] }])
    assert.equal(canAccessPage('/active-safety/settings/work-orders', [action]), false)
  }
})

test('legacy VLS devices use their own permission, never an unrelated protocol grant', () => {
  const legacy = 'vls:menu:legacy-device-management'
  assert.equal(permissionForPage('/device-management'), legacy)
  for (const path of ['/device-management', '/device-edit', '/device-config', '/device-ai-event', '/camera-settings']) {
    assert.equal(canAccessPage(path, [legacy]), true, path)
    assert.equal(canAccessPage(path, ['vls:menu:custom:device', 'vls:menu:vlstream:device']), false, path)
    assert.equal(canAccessPage(path, []), false, path)
  }
})

test('role save preserves partial parents and exact 19-digit menu IDs', () => {
  assert.deepEqual(selectedMenuIds(['1900000000000000701'], ['900000', '1900000000000000700']),
    ['1900000000000000701', '900000', '1900000000000000700'])
  assert.deepEqual(hiddenGrantedMenuIds([{ id: '900000', children: [{ id: '1900000000000000701' }] }],
    ['1900000000000000701', '1900000000000000702']), ['1900000000000000702'])
})

test('real tree round trips parent-only, partial and empty grants without cascading to new children', () => {
  const nodes = [{ id: '900000', children: [
    { id: '1900000000000000701' }, { id: '1900000000000000702' }
  ] }]
  for (const grants of [[], ['900000'], ['1900000000000000701'], ['900000', '1900000000000000701']]) {
    const store = new TreeStore({ key: 'id', data: nodes, props: { children: 'children' } })
    store.initialize()
    const visible = orderedVisibleGrants(nodes, grants)
    visible.forEach(id => store.setChecked(id, true, false))
    const selection = selectedMenuIds(store.getCheckedKeys(false), store.getHalfCheckedKeys())
    assert.deepEqual(new Set(changedVisibleGrants(visible, selection, selection)), new Set(grants))
    assert.equal(store.getNode('1900000000000000702').checked, false)
  }
})

test('explicit child selection includes its half-selected ancestors', () => {
  const original = ['1900000000000000701']
  const initial = ['900000', '1900000000000000701']
  const after = ['900000', '1900000000000000701', '1900000000000000702']
  assert.deepEqual(new Set(changedVisibleGrants(original, initial, after, ['900000', '1900000000000000702'])),
    new Set(['900000', '1900000000000000701', '1900000000000000702']))
})

test('an assigned but unrendered grantable node survives an untouched save', () => {
  const tree = [{ id: '900000', children: [{ id: '1900000000000000701' }] }]
  const original = ['900000', '1900000000000000701', '1900000000000000702', '1900000000000000703']
  const grantable = ['900000', '1900000000000000701', '1900000000000000702']
  const visible = orderedVisibleGrants(tree, original)
  const store = new TreeStore({ key: 'id', data: tree, props: { children: 'children' } })
  store.initialize(); visible.forEach(id => store.setChecked(id, true, false))
  const selection = selectedMenuIds(store.getCheckedKeys(false), store.getHalfCheckedKeys())
  const unchanged = changedVisibleGrants(visible, selection, selection)
  assert.deepEqual(new Set(preserveGrantableHiddenGrants(tree, original, grantable, unchanged)),
    new Set(['900000', '1900000000000000701', '1900000000000000702']))
})

test('square CameraRTC consumes only the verified target ID and configured signaling endpoint', () => {
  assert.deepEqual(squareCameraRtcConfig('camera-1', 'wss://fixture.invalid/signal'),
    { cameraId: 'camera-1', socketUrl: 'wss://fixture.invalid/signal' })
  assert.throws(() => squareCameraRtcConfig('camera/other', 'wss://fixture.invalid/signal'))
})

test('CameraRTC raw paths keep the legacy target for valid device URLs and reject ambiguous paths', () => {
  assert.equal(cameraRtcIdFromUrl('http://146.56.220.167:8082/videocall/AETY-00-XOKU-L1A2-00000002'), 'AETY-00-XOKU-L1A2-00000002')
  assert.equal(cameraRtcIdFromUrl('https://camera.example.com/gateway/VideoCall/CAM-1?token=fixture'), 'CAM-1')
  assert.equal(cameraRtcIdFromUrl('https://camera.example.com/gate%20way/%E8%AE%BE%E5%A4%87/videocall/CAM-1'), 'CAM-1')
  assert.equal(cameraRtcIdFromUrl('https://camera.example.com/gateway/videocall//CAM-1/extra'), 'CAM-1')
  for (const path of ['/videocall/A/videocall/B', '/videocall/A/videocall', '/gateway/../videocall/A',
    '/gateway/./videocall/A', '/videocall/A%2FB', '/%76ideocall/A', '/videocall/A%5cB',
    '/%2e%2e/videocall/A', '/foo%2fbar/videocall/A', '/foo%255cbar/videocall/A']) {
    assert.throws(() => cameraRtcIdFromUrl(`https://camera.example.com${path}`), path)
  }
})
