import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse, compileScript } from 'vue/compiler-sfc'
import { transformSync } from 'esbuild'
import { webcrypto } from 'node:crypto'
import axios from 'axios'
import { eventWorkOrderActor, eventWorkOrderIntent, clearEventWorkOrderIntent,
  rememberConfirmedWorkOrder, findPendingWorkOrderIntent, markPreviousOrderStop } from '../src/utils/eventWorkOrderIntent.mjs'

const source = readFileSync(new URL('../src/views/events/page/eventManagement/components/flowForm.vue', import.meta.url), 'utf8')
const ts = compileScript(parse(source).descriptor, { id: 'event-workorder-test' }).content
const js = transformSync(ts, { loader: 'ts', format: 'esm' }).code
  .replace(/^import\s[\s\S]*?from\s+["'][^"']+["'];\s*$/gm, '').replace(/export\s*\{[^}]+\};?\s*$/m, '')
const create = new Function('__deps', `
  const {_defineComponent, uiText, ref, reactive, onMounted, listModel, startEventWorkOrder, stopEventWorkOrder,
    useUserStore, ElMessage, ElMessageBox, event_item_list, resolveWorkOrderAppContext,
    AuthManager, eventWorkOrderActor, eventWorkOrderIntent, clearEventWorkOrderIntent,
    rememberConfirmedWorkOrder, findPendingWorkOrderIntent, markPreviousOrderStop, window} = __deps;
  ${js}
  return stdin_default;
`)

test('event work-order API uses the dedicated path while generic start remains unchanged', () => {
  const api = readFileSync(new URL('../src/api/processui/index.ts', import.meta.url), 'utf8')
  const js = transformSync(api, { loader: 'ts', format: 'esm' }).code
    .replace(/^import\s[\s\S]*?from\s+["'][^"']+["'];\s*$/gm, '').replace(/export\s*\{[^}]+\};?\s*$/m, '')
  const sent = []
  const methods = new Function('request', 'apaasServiceUrl', 'axios', `${js}\nreturn { startProcess, startEventWorkOrder }`)
    (config => { sent.push(config) }, (_, path) => path, axios)
  const body = { processDefId: 'fixture' }
  const identity = { token: 'fixture-local', platformToken: 'fixture-platform' }
  let current = true
  methods.startProcess(body); methods.startEventWorkOrder(body, identity, () => { if (!current) throw Error('changed') })
  assert.deepEqual(sent.map(config => [config.url, config.data, config.method]),
    [['/workflow/process/start', body, 'post'], ['/workflow/process/start-work-order', body, 'post']])
  assert.equal(sent[1].localAuthToken, 'fixture-local')
  assert.equal(sent[1].platformAuthToken, 'fixture-platform')
  current = false
  assert.throws(() => sent[1].transformRequest[0]('payload'))
})

test('identity changed in an async interceptor aborts before work-order transport', async () => {
  const api = readFileSync(new URL('../src/api/processui/index.ts', import.meta.url), 'utf8')
  const js = transformSync(api, { loader: 'ts', format: 'esm' }).code
    .replace(/^import\s[\s\S]*?from\s+["'][^"']+["'];\s*$/gm, '').replace(/export\s*\{[^}]+\};?\s*$/m, '')
  let sent = 0, current = true
  const request = axios.create({ adapter: async config => { sent++; return { config, data: {}, status: 200, statusText: 'OK', headers: {} } } })
  request.interceptors.request.use(async config => { await Promise.resolve(); current = false; return config })
  const methods = new Function('request', 'apaasServiceUrl', 'axios', `${js}\nreturn { startEventWorkOrder, stopEventWorkOrder }`)
    (request, (_, path) => path, axios)
  const identity = { token: 'fixture-local', platformToken: 'fixture-platform' }
  const guard = () => { if (!current) throw Error('identity changed') }
  await assert.rejects(methods.startEventWorkOrder({ requestId: 'fixture' }, identity, guard), /identity changed/)
  current = true
  await assert.rejects(methods.stopEventWorkOrder({ procInsId: 'fixture' }, identity, guard), /identity changed/)
  assert.equal(sent, 0)
})

async function harness(options = {}) {
  const messages = [], starts = [], mounts = []
  const values = options.values || new Map()
  const storage = { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) }
  const deps = {
    _defineComponent: value => value, uiText: text => text, ref: value => ({ value }), reactive: value => value,
    onMounted: fn => mounts.push(fn), listModel: async () => ({ code: 200, rows: [{ modelKey: 'fixture-model', modelName: 'Fixture', definitionId: 'fixture-def' }] }),
    startEventWorkOrder: async data => { starts.push(data); return options.start ? options.start(data) : { code: 200, data: { workOrder: { procInsId: 'instance-1' } } } },
    useUserStore: () => ({ userInfo: {} }), ElMessage: { warning: msg => messages.push(msg), error: msg => messages.push(msg), success: msg => messages.push(msg) },
    ElMessageBox: { confirm: async () => { if (options.cancel) throw 'cancel' } },
    stopEventWorkOrder: options.stop || (async () => ({ code: 200 })),
    event_item_list: async () => ({ code: 200, data: { list: [] } }), resolveWorkOrderAppContext: async () => ({ appId: 'fixture-app' }),
    AuthManager: class { getIdentitySnapshot() { return { tenantId: 'tenant-1' } } isIdentityCurrent() { return true } },
    eventWorkOrderActor, eventWorkOrderIntent, clearEventWorkOrderIntent, rememberConfirmedWorkOrder, findPendingWorkOrderIntent, markPreviousOrderStop,
    window: { localStorage: storage, sessionStorage: { getItem: () => JSON.stringify({ userId: 'user-1' }) }, crypto: webcrypto }
  }
  let exposed
  const component = create(deps)
  const vm = component.setup({ listObj: { id: 'event-0', name: 'fixture', ...options.event } }, { expose: value => { exposed = value } })
  vm.formRef_1.value = { validate: async () => true }
  await mounts[0]()
  // Mount callback starts an async loader without returning it.
  await new Promise(resolve => setImmediate(resolve))
  vm.form.processKey = 'fixture-model'
  vm.workConfirm('fixture-model')
  return { vm, exposed, starts, messages, values }
}

test('event work order preserves existing payload and returns the confirmed server work order', async () => {
  const h = await harness()
  const result = await h.exposed.addWorkorderFn()
  assert.equal(result.procInsId, 'instance-1')
  assert.equal(h.starts.length, 1)
  assert.equal(h.starts[0].processDefId, 'fixture-def')
  assert.equal(h.starts[0].workOrderBo.eventNumber, 'event-0')
})

test('unconfirmed and failed creation never returns an order for feedback', async () => {
  for (const start of [async () => ({ code: 403 }), async () => ({ code: 200, data: { workOrder: {} } }), async () => { throw Error('transport') }]) {
    const h = await harness({ start })
    assert.equal(await h.exposed.addWorkorderFn(), null)
    assert.equal(h.exposed.form.work_order_data, undefined)
    assert.ok(h.messages.some(message => message.includes('未确认')))
  }
})

test('cancelling or failing to stop the old order prevents creation of a new one', async () => {
  const previous = { processKey: 'old', procInsId: 'old-instance', processStatus: 'running' }
  for (const options of [{ cancel: true }, { stop: async () => ({ code: 403 }) }]) {
    const h = await harness({ ...options, event: { work_order_data: previous } })
    assert.ok((await h.exposed.addWorkorderFn()) == null)
    assert.equal(h.starts.length, 0)
  }
})

test('feedback is submitted only after a requested work order is confirmed', async () => {
  const feedbackSource = readFileSync(new URL('../src/views/events/page/eventManagement/eventPhoneFBack.vue', import.meta.url), 'utf8')
  const feedbackTs = compileScript(parse(feedbackSource).descriptor, { id: 'event-feedback-test' }).content
  const feedbackJs = transformSync(feedbackTs, { loader: 'ts', format: 'esm' }).code
    .replace(/^import\s[\s\S]*?from\s+["'][^"']+["'];\s*$/gm, '').replace(/export\s*\{[^}]+\};?\s*$/m, '')
  const build = new Function('__deps', `
    const {_defineComponent, uiText, ref, computed, watch, MultiImageUpload, CommonExpressions,
      FeedbackRecord, WorkOrderRecord, eventDetail, addFeedback, addWorkOrderFeedback, feedbackList, useUserStore,
      ElMessage, FlowForm, AuthManager} = __deps;
    ${feedbackJs}
    return stdin_default;
  `)
  const submitted = []
  let current = true
  const component = build({
    _defineComponent: value => value, uiText: value => value, ref: value => ({ value }), computed: value => value,
    watch() {}, MultiImageUpload: {}, CommonExpressions: {}, FeedbackRecord: {}, WorkOrderRecord: {}, FlowForm: {},
    eventDetail: async () => ({}), feedbackList: async () => ({ code: 200, data: { list: [] } }),
    addFeedback: async body => { submitted.push({ ...body }); return { code: 200 } },
    addWorkOrderFeedback: async (body, identity, guard) => { guard(); submitted.push({ ...body }); return { code: 200 } },
    useUserStore: () => ({}), ElMessage: { success() {}, error() {} },
    AuthManager: class { getIdentitySnapshot() { return { tenantId: 'tenant-1' } } isIdentityCurrent() { return current } }
  })
  const vm = component.setup({ data: {}, visible: false }, { expose() {}, emit() {} })
  vm.formRef.value = { validate: async () => true }
  vm.formData.value.work_order_status = 1
  vm.flowFormRef.value = { addWorkorderFn: async () => null }
  await vm.addFeedbackForm()
  assert.equal(submitted.length, 0)
  vm.flowFormRef.value = { addWorkorderFn: async () => { current = false; return { procInsId: 'confirmed' } } }
  await vm.addFeedbackForm()
  assert.equal(submitted.length, 0)
  current = true
  vm.flowFormRef.value = { addWorkorderFn: async () => { vm.formData.value.work_order_status = 0; return { procInsId: 'confirmed' } } }
  await vm.addFeedbackForm()
  assert.equal(submitted.length, 0)
  vm.formData.value.work_order_status = 1
  vm.flowFormRef.value = { addWorkorderFn: async () => ({ procInsId: 'confirmed' }), clearConfirmedIntent() {} }
  await vm.addFeedbackForm()
  assert.equal(submitted.length, 1)
  assert.equal(submitted[0].work_order_data.procInsId, 'confirmed')
})

test('work-order feedback aborts before transport when identity changes in an interceptor', async () => {
  const api = readFileSync(new URL('../src/api/smartCity/events/index.ts', import.meta.url), 'utf8')
  const js = transformSync(api, { loader: 'ts', format: 'esm' }).code
    .replace(/^import\s[\s\S]*?from\s+["'][^"']+["'];\s*$/gm, '').replace(/export\s*\{[^}]+\};?\s*$/m, '')
  let sent = 0, current = true
  const request = axios.create({ adapter: async config => { sent++; return { config, data: {}, status: 200, statusText: 'OK', headers: {} } } })
  request.interceptors.request.use(async config => { await Promise.resolve(); current = false; return config })
  const methods = new Function('request', 'axios', `${js}\nreturn { addWorkOrderFeedback }`)(request, axios)
  await assert.rejects(methods.addWorkOrderFeedback({ id: 'event', accessToken: 'fixture' },
    { token: 'local', platformToken: 'platform' }, () => { if (!current) throw Error('identity changed') }), /identity changed/)
  assert.equal(sent, 0)
})

test('a lost response and page refresh reuse the same ID, while changed fields are blocked', async () => {
  const values = new Map()
  const first = await harness({ values, start: async () => { throw Error('response lost') } })
  assert.equal(await first.exposed.addWorkorderFn(), null)
  const originalId = first.starts[0].requestId
  assert.match(originalId, /^[0-9a-f-]{36}$/)
  const refreshed = await harness({ values })
  const result = await refreshed.exposed.addWorkorderFn()
  assert.equal(result.procInsId, 'instance-1')
  assert.equal(refreshed.starts[0].requestId, originalId)
  const changed = await harness({ values, event: { name: 'changed payload' } })
  assert.equal(await changed.exposed.addWorkorderFn(), null)
  assert.equal(changed.starts.length, 0)
  assert.ok(changed.messages.some(message => message.includes('上次建单结果未确认')))
})

test('changed content cannot stop an old order while an earlier request is unresolved', async () => {
  const values = new Map()
  const first = await harness({ values, start: async () => { throw Error('response lost') } })
  await first.exposed.addWorkorderFn()
  let stops = 0
  const changed = await harness({ values, event: { name: 'changed', work_order_data: {
    processKey: 'old', procInsId: 'old-instance', processStatus: 'running'
  } }, stop: async () => { stops++; return { code: 200 } } })
  assert.equal(await changed.exposed.addWorkorderFn(), null)
  assert.equal(stops, 0)
  assert.equal(changed.starts.length, 0)
})

test('selecting an old order cannot bypass a different unconfirmed request', async () => {
  const values = new Map()
  const first = await harness({ values, start: async () => { throw Error('response lost') } })
  await first.exposed.addWorkorderFn()
  const old = await harness({ values, event: { work_order_data: { processKey: 'fixture-model', procInsId: 'old-instance' } } })
  assert.equal(await old.exposed.addWorkorderFn(), null)
  assert.equal(old.starts.length, 0)
  assert.ok(old.messages.some(message => message.includes('上次建单结果未确认')))
})

test('a confirmed old-order stop is not repeated when the new-order response was lost', async () => {
  const values = new Map(), old = { processKey: 'old', procInsId: 'old-instance', processStatus: 'running' }
  let stops = 0
  const first = await harness({ values, event: { work_order_data: old }, stop: async () => { stops++; return { code: 200 } },
    start: async () => { throw Error('response lost') } })
  assert.equal(await first.exposed.addWorkorderFn(), null)
  const retry = await harness({ values, event: { work_order_data: old }, stop: async () => { stops++; return { code: 200 } } })
  assert.equal((await retry.exposed.addWorkorderFn()).procInsId, 'instance-1')
  assert.equal(retry.starts[0].requestId, first.starts[0].requestId)
  assert.equal(stops, 1)
})

test('an unconfirmed old-order stop prevents another stop or new order', async () => {
  const values = new Map(), old = { processKey: 'old', procInsId: 'old-instance', processStatus: 'running' }
  let stops = 0
  const first = await harness({ values, event: { work_order_data: old }, stop: async () => { stops++; throw Error('response lost') } })
  assert.equal(await first.exposed.addWorkorderFn(), null)
  const retry = await harness({ values, event: { work_order_data: old }, stop: async () => { stops++; return { code: 200 } } })
  assert.equal(await retry.exposed.addWorkorderFn(), null)
  assert.equal(retry.starts.length, 0)
  assert.equal(stops, 1)
  const stale = await harness({ values, event: { work_order_data: { ...old, processStatus: 'canceled' } } })
  assert.equal(await stale.exposed.addWorkorderFn(), null)
  assert.equal(stale.starts.length, 0)
})

test('confirmed feedback can clear an intent after refresh without storing event contents', async () => {
  const values = new Map()
  const first = await harness({ values })
  const result = await first.exposed.addWorkorderFn()
  const saved = [...values.values()].join('')
  assert.ok(!saved.includes('fixture-def'))
  assert.ok(!saved.includes('event-0'))
  const refreshed = await harness({ values, event: { work_order_data: { ...result, processKey: 'fixture-model' } } })
  assert.equal((await refreshed.exposed.addWorkorderFn()).procInsId, result.procInsId)
  refreshed.exposed.clearConfirmedIntent()
  assert.equal(values.size, 0)
})

test('one event cannot reuse an operation ID across different actors or tenants', async () => {
  const values = new Map()
  const storage = { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) }
  const payload = { processDefId: 'fixture', variables: { a: 1 } }
  const a = await eventWorkOrderIntent(storage, webcrypto, { tenantId: 'a', userId: '1' }, 'event', payload, true)
  const b = await eventWorkOrderIntent(storage, webcrypto, { tenantId: 'b', userId: '1' }, 'event', payload, true)
  const c = await eventWorkOrderIntent(storage, webcrypto, { tenantId: 'a', userId: '2' }, 'event', payload, true)
  assert.equal(new Set([a.requestId, b.requestId, c.requestId]).size, 3)
})

test('unsafe numeric actor or event IDs are rejected before creating an intent', async () => {
  assert.throws(() => eventWorkOrderActor({ userId: 9007199254740992 }, null, 'tenant'))
  const h = await harness({ event: { id: 9007199254740992 } })
  assert.equal(await h.exposed.addWorkorderFn(), null)
  assert.equal(h.starts.length, 0)
  assert.equal(h.values.size, 0)
})
