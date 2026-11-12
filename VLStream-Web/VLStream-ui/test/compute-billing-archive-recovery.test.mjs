import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse, compileScript, compileTemplate } from 'vue/compiler-sfc'
import * as Vue from 'vue'
import { renderToString } from '@vue/server-renderer'
import { billingCurrency, paidRunState, paidRunActive, paidRunErrorPresentation, paidRunQuoteText } from '../src/utils/computeBillingPresentation.mjs'

const source = readFileSync(new URL('../src/views/AIComputingScheduling/components/ComputeBillingPanel.vue', import.meta.url), 'utf8')
const compiled = compileScript(parse(source).descriptor, { id: 'archive-recovery-test' }).content
const executable = compiled.replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm, '').replace('export default', 'return')
const create = new Function('__deps', `
  const uiText = value => value;
  const phrase = (value, params) => value.replace(/\\{(\\w+)\\}/g, (_, key) => String(params[key]));
  const {computed,onMounted,onUnmounted,ref,useRouter,ElMessageBox,getComputeBillingCapabilities,
    checkComputeBillingAccount,quoteComputePaidRun,quoteComputeArchiveRecovery,listComputePaidRuns,
    acceptComputePaidRun,stopComputePaidRun,refreshComputeHealth,captureComputeIdentity,
    requireComputePlatformAuthorization,isComputeAuthorizationCurrent,billingCurrency,paidRunState,
    paidRunActive,paidRunErrorPresentation,paidRunQuoteText,window,setInterval,clearInterval}=__deps;
  ${executable}
`)
const template = compileTemplate({ source: parse(source).descriptor.template.content, id: 'billing-presentation-test' })
assert.deepEqual(template.errors, [])
const render = new Function('Vue', template.code
  .replace(/^import \{(.*?)\} from "vue"$/gm, (_, names) => `const {${names.replace(/ as /g, ': ')}} = Vue`)
  .replace('export function render', 'function render') + '\nreturn render')(Vue)
const renderPanel = async instance => {
  const bindings = Object.fromEntries(Object.entries(instance).map(([key, value]) => [key,
    value && Object.prototype.hasOwnProperty.call(value, 'value') ? value.value : value]))
  const app = Vue.createSSRApp({ setup: () => bindings, render })
  app.component('el-alert', { props: ['title', 'type'], render() { return Vue.h('aside', { 'data-alert-type': this.type }, this.title) } })
  app.component('el-button', { props: ['disabled'], render() { return Vue.h('button', { disabled: this.disabled }, this.$slots.default?.()) } })
  return renderToString(app)
}
const defer = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const flush = () => new Promise(resolve => setImmediate(resolve))
const original = () => ({ runId: 'source-1', nodeId: '-6000', state: 'COMPLETED', chargedQuota: '4', remainingQuota: '0', quotaUnit: 'quota', archiveRecoveryAvailable: true, archiveRecovery: false, archiveRecoveryJobIds: [], archiveRecoverySourceRunId: null })
const quote = () => ({ ...original(), runId: 'recovery-1', state: 'QUOTED', archiveRecoveryAvailable: false, archiveRecovery: true, archiveRecoverySourceRunId: 'source-1', archiveRecoveryJobIds: ['job-1'], reserveQuota: '5', maximumRunSeconds: 60, hourlyPrice: '13.00', currency: 'CNY', quoteUntilMs: Date.now() + 60000 })

function harness(options = {}) {
  const calls = { capabilities: 0, list: 0, account: 0, normalQuote: 0, recoveryQuote: [], accept: [], stop: [], health: 0, confirmations: [], route: [], proof: 0 }
  const mounts = [], unmounts = [], events = new Map(), timers = [], emitted = []
  let identity = 1, rows = [original()]
  const deps = {
    computed: fn => ({ get value() { return fn() } }), ref: value => ({ value }),
    onMounted: fn => mounts.push(fn), onUnmounted: fn => unmounts.push(fn),
    useRouter: () => ({ push: async value => { calls.route.push(value) } }),
    ElMessageBox: { confirm: async (...args) => { calls.confirmations.push(args); return options.confirm ? options.confirm(...args) : 'confirm' }, alert: async () => {} },
    getComputeBillingCapabilities: async () => { calls.capabilities++; return { data: options.capabilities || { accountAvailable: true, paidAdmissionEnabled: options.enabled !== false } } },
    checkComputeBillingAccount: async () => { calls.account++; return { data: {} } },
    quoteComputePaidRun: async () => { calls.normalQuote++; return { data: quote() } },
    quoteComputeArchiveRecovery: async id => { calls.recoveryQuote.push(id); return options.quote ? options.quote(id) : { data: quote() } },
    listComputePaidRuns: async () => { calls.list++; return { data: rows } },
    acceptComputePaidRun: async id => { calls.accept.push(id); return options.accept ? options.accept(id) : { data: { ...quote(), state: 'FUNDING' } } },
    stopComputePaidRun: async id => { calls.stop.push(id); return {} },
    refreshComputeHealth: async () => { calls.health++; return { data: 'fixture' } },
    captureComputeIdentity: () => { const current = identity; return { assertCurrent() { if (current !== identity) throw Error('fixture identity changed') } } },
    requireComputePlatformAuthorization: async () => { calls.proof++; return options.proof ? options.proof() : { identity, platformToken: 'fixture-proof' } },
    isComputeAuthorizationCurrent: proof => proof.identity === identity,
    billingCurrency, paidRunState, paidRunActive, paidRunErrorPresentation, paidRunQuoteText,
    window: { addEventListener: (name, fn) => events.set(name, fn), removeEventListener: name => events.delete(name) },
    setInterval: fn => { timers.push(fn); return timers.length }, clearInterval() {}
  }
  const resources = options.resources || [{ id: '-6000', name: 'Fixture official', provider: 'autodl', configurationManaged: true, billingType: 'POINTS', paidAdmissionEnabled: true }]
  const instance = create(deps).setup({ resources }, { expose() {}, emit: (...args) => emitted.push(args) })
  return { instance, calls, mounts, unmounts, timers, emitted,
    mount: () => mounts[0](), setRows: value => { rows = value },
    changeIdentity: () => { identity++; events.get('modelHubAuthChanged')?.() } }
}

test('mount and polling read capabilities/runs only; pending archive never automatically quotes or accepts', async () => {
  const h = harness()
  await h.mount()
  h.timers[0]()
  await flush()
  assert.equal(h.calls.capabilities, 1)
  assert.equal(h.calls.list, 2)
  assert.equal(h.calls.account, 0)
  assert.equal(h.calls.proof, 0)
  assert.equal(h.calls.normalQuote, 0)
  assert.deepEqual(h.calls.recoveryQuote, [])
  assert.deepEqual(h.calls.accept, [])
})

test('missing merchant configuration is explained without contacting the account or hiding the balance entry', async () => {
  const reason = '平台计费缺配置：merchant-id 商户身份'
  const h = harness({ capabilities: { accountAvailable: false, paidAdmissionEnabled: false, unavailableReason: reason } })
  await h.mount()
  const html = await renderPanel(h.instance)
  assert.match(html, /平台账户配置待确认/)
  assert.match(html, /<button[^>]*disabled[^>]*>查询余额/)
  assert.ok(html.includes(reason))
  assert.doesNotMatch(html, /暂未开放|尚未启用|READ_ONLY|PAID|OFF/)
  assert.equal(h.calls.list, 0)
  assert.equal(h.calls.account, 0)
  assert.equal(h.calls.proof, 0)
  assert.deepEqual(h.calls.accept, [])
})

test('base-only identity availability lists and polls original runs and permits explicit normal stop without merchant access', async () => {
  const h = harness({ capabilities: { identityAvailable: true, accountAvailable: false, paidAdmissionEnabled: false,
    unavailableReason: '平台计费缺配置：api-key 商户凭据' } })
  h.setRows([{ ...original(), state: 'RUNNING', archiveRecoveryAvailable: false }])
  await h.mount()
  assert.equal(h.calls.list, 1)
  let html = await renderPanel(h.instance)
  assert.match(html, /<button[^>]*disabled[^>]*>查询余额/)
  assert.match(html, /停止并结算/)
  assert.doesNotMatch(html, /<button[^>]*disabled[^>]*>停止并结算/)
  h.timers[0]()
  await flush()
  assert.equal(h.calls.list, 2)
  assert.equal(h.calls.account, 0)
  assert.equal(h.calls.proof, 0)
  await h.instance.stop(original())
  assert.equal(h.calls.proof, 1)
  assert.deepEqual(h.calls.stop, ['source-1'])
  assert.equal(h.calls.account, 0)
  assert.equal(h.calls.normalQuote, 0)
  assert.deepEqual(h.calls.accept, [])
})

test('explicit identity unavailability prevents original run polling even if an old account flag is true', async () => {
  const h = harness({ capabilities: { identityAvailable: false, accountAvailable: true, paidAdmissionEnabled: false } })
  await h.mount()
  h.timers[0]()
  await flush()
  assert.equal(h.calls.list, 0)
  assert.equal(h.calls.account, 0)
  assert.equal(h.calls.proof, 0)
})

test('configured billing starts only after current platform authorization and explicit quote confirmation', async () => {
  const confirmation = defer(), h = harness({ confirm: (_message, title) => title === '平台余额' ? 'confirm' : confirmation.promise })
  await h.mount()
  const action = h.instance.start({ id: '-6000', paidAdmissionEnabled: true })
  await flush()
  assert.equal(h.calls.proof, 1)
  assert.equal(h.calls.normalQuote, 1)
  assert.deepEqual(h.calls.accept, [])
  assert.equal(h.calls.confirmations[0][1], '平台余额')
  assert.match(h.calls.confirmations[1][0], /13.00 CNY/)
  confirmation.resolve('confirm')
  await action
  assert.deepEqual(h.calls.accept, ['recovery-1'])
  assert.equal(h.calls.account, 0)
})

test('missing platform identity cannot quote or accept even with complete billing configuration', async () => {
  const h = harness({ proof: async () => null })
  await h.mount()
  await h.instance.start({ id: '-6000', paidAdmissionEnabled: true })
  assert.equal(h.calls.normalQuote, 0)
  assert.deepEqual(h.calls.accept, [])
  assert.equal(h.calls.confirmations.length, 0)
})

test('unavailable target reports its specific configuration reason without quoting or accepting', async () => {
  const h = harness()
  await h.mount()
  await h.instance.start({ id: '-6000', paidAdmissionEnabled: false, paidUnavailableReason: '官方算力供应商调用身份未配置' })
  assert.equal(h.instance.error.value, '官方算力供应商调用身份未配置')
  assert.equal(h.calls.normalQuote, 0)
  assert.deepEqual(h.calls.accept, [])
})

test('the panel keeps active reconciliation warnings and hides completed releases', async () => {
  const h = harness()
  h.setRows([{ ...original(), state: 'RELEASING', archiveRecoveryAvailable: false, startFailureCode: 'NO_STOCK', errorCode: 'RELEASE_RECEIPT_PENDING' }])
  await h.mount()
  const pending = await renderPanel(h.instance)
  assert.match(pending, /data-alert-type="warning"[^>]*>状态确认暂未完成.*当前资源被占用/)
  assert.match(pending, /停止并结算/)
  assert.doesNotMatch(pending, /已完成释放/)
  h.setRows([{ ...original(), state: 'CANCELLED', archiveRecoveryAvailable: false, startFailureCode: 'NO_STOCK', errorCode: null }])
  await h.instance.refreshRuns()
  const cancelled = await renderPanel(h.instance)
  assert.doesNotMatch(cancelled, /<aside|停止并结算|状态确认暂未完成/)
  assert.deepEqual(h.calls.accept, [])
  assert.deepEqual(h.calls.stop, [])
})

test('the panel template distinguishes an explicit rejected start from an unknown result without message inference', async () => {
  const h = harness()
  h.setRows([{ ...original(), state: 'CANCELLING', archiveRecoveryAvailable: false, startFailureCode: 'START_REJECTED', errorCode: null }])
  await h.mount()
  const rejected = await renderPanel(h.instance)
  assert.match(rejected, /data-alert-type="warning"[^>]*>本次开机被供应商拒绝/)
  assert.doesNotMatch(rejected, /当前资源被占用/)
  h.setRows([{ ...original(), state: 'STARTING', archiveRecoveryAvailable: false, errorCode: 'UNKNOWN', msg: 'NO_STOCK 当前资源被占用' }])
  await h.instance.refreshRuns()
  const unknown = await renderPanel(h.instance)
  assert.match(unknown, /data-alert-type="warning"[^>]*>状态确认暂未完成.*请勿重复提交启停操作/)
  assert.match(unknown, /停止并结算/)
  assert.doesNotMatch(unknown, /当前资源被占用|被供应商拒绝/)
  h.setRows([{ ...original(), state: 'STARTING', archiveRecoveryAvailable: false, errorCode: null }])
  await h.instance.refreshRuns()
  assert.doesNotMatch(await renderPanel(h.instance), /<aside/)
})

test('completed history without recovery is hidden while active recovery stays visible', async () => {
  const h = harness()
  const recent = Array.from({ length: 5 }, (_, index) => ({ ...original(), runId: `recent-${index}`, archiveRecoveryAvailable: false }))
  const active = { ...quote(), state: 'RUNNING' }
  h.setRows([...recent, { ...original(), runId: 'old-complete', archiveRecoveryAvailable: false }, original(), active])
  await h.mount()
  assert.deepEqual(h.instance.visibleRuns.value.map(run => run.runId), ['source-1', 'recovery-1'])
  assert.deepEqual(h.calls.recoveryQuote, [])
  assert.deepEqual(h.calls.accept, [])
})

test('explicit recovery quotes the source run and requires confirmation of new price/hold before accepting', async () => {
  const confirmation = defer()
  const h = harness({ confirm: () => confirmation.promise })
  await h.mount()
  const action = h.instance.recoverArchive(original())
  await flush()
  assert.deepEqual(h.calls.recoveryQuote, ['source-1'])
  assert.deepEqual(h.calls.accept, [])
  const message = h.calls.confirmations[0][0]
  for (const text of ['13.00 CNY/整实例小时', '新订单、新冻结 5 quota', '不会复用旧订单冻结', '不重新训练', '磁盘保留期限', '实际确认关机前仍计费']) assert.ok(message.includes(text))
  assert.doesNotMatch(message, /6\.46/)
  confirmation.resolve('confirm')
  await action
  assert.deepEqual(h.calls.accept, ['recovery-1'])
  assert.equal(h.calls.normalQuote, 0)
  assert.equal(h.calls.health, 0)
})

test('cancel retains the original quote for an explicit later confirmation, without duplicate quoting', async () => {
  let approved = false
  const h = harness({ confirm: () => approved ? 'confirm' : Promise.reject('cancel') })
  await h.mount()
  await h.instance.recoverArchive(original())
  assert.deepEqual(h.calls.accept, [])
  assert.equal(h.instance.recoveryActionLabel(original()), '继续确认归档恢复')
  approved = true
  await h.instance.recoverArchive(original())
  assert.deepEqual(h.calls.recoveryQuote, ['source-1'])
  assert.deepEqual(h.calls.accept, ['recovery-1'])
})

test('double clicks during quote, confirmation, and acceptance do not submit a second order', async () => {
  const pendingQuote = defer(), confirmation = defer(), accept = defer()
  const h = harness({ quote: () => pendingQuote.promise, confirm: () => confirmation.promise, accept: () => accept.promise })
  await h.mount()
  const action = h.instance.recoverArchive(original())
  await flush()
  await h.instance.recoverArchive(original())
  pendingQuote.resolve({ data: quote() })
  await flush()
  await h.instance.recoverArchive(original())
  confirmation.resolve('confirm')
  await flush()
  await h.instance.recoverArchive(original())
  accept.resolve({ data: { ...quote(), state: 'FUNDING' } })
  await action
  await h.instance.recoverArchive(original())
  assert.deepEqual(h.calls.recoveryQuote, ['source-1'])
  assert.equal(h.calls.confirmations.length, 1)
  assert.deepEqual(h.calls.accept, ['recovery-1'])
})

test('lost quote response checks lists and resumes only the unique original quote, never auto-accepts', async () => {
  const h = harness({ quote: () => Promise.reject(Error('fixture lost response')) })
  await h.mount()
  await h.instance.recoverArchive(original())
  assert.equal(h.instance.recoveryActionLabel(original()), '核对归档恢复订单')
  h.setRows([original(), quote()])
  await h.instance.recoverArchive(original())
  assert.deepEqual(h.calls.accept, [])
  assert.equal(h.calls.confirmations.length, 0)
  assert.equal(h.instance.recoveryActionLabel(original()), '继续确认归档恢复')
  await h.instance.recoverArchive(original())
  assert.deepEqual(h.calls.recoveryQuote, ['source-1'])
  assert.deepEqual(h.calls.accept, ['recovery-1'])
})

test('ambiguous quote matches or a different source cannot silently select or accept a recovery', async () => {
  for (const rows of [[quote(), { ...quote(), runId: 'recovery-2' }], [{ ...quote(), archiveRecoverySourceRunId: 'another-source' }]]) {
    const h = harness({ quote: () => Promise.reject(Error('fixture lost response')) })
    await h.mount()
    await h.instance.recoverArchive(original())
    h.setRows([original(), ...rows])
    await h.instance.recoverArchive(original())
    assert.deepEqual(h.calls.accept, [])
    assert.equal(h.calls.recoveryQuote.length, 1)
    assert.equal(h.instance.recoveryAttempts.value['source-1'].phase, 'QUOTE_UNKNOWN')
  }
})

test('unknown acceptance retains the new run ID and only queries it without retrying POST', async () => {
  const h = harness({ accept: () => Promise.reject(Error('fixture lost response')) })
  await h.mount()
  await h.instance.recoverArchive(original())
  assert.equal(h.instance.recoveryAttempts.value['source-1'].quote.runId, 'recovery-1')
  h.setRows([original(), quote()])
  await h.instance.recoverArchive(original())
  assert.deepEqual(h.calls.accept, ['recovery-1'])
  assert.equal(h.instance.recoveryAttempts.value['source-1'].phase, 'ACCEPT_UNKNOWN')
  h.setRows([{ ...original(), archiveRecoveryAvailable: false }, { ...quote(), state: 'FUNDING' }])
  await h.instance.recoverArchive(original())
  assert.equal(h.instance.recoveryAttempts.value['source-1'].phase, 'OBSERVED')
  assert.deepEqual(h.calls.accept, ['recovery-1'])
})

for (const stage of ['proof', 'quote', 'confirm']) {
  test(`identity change during ${stage} prevents subsequent recovery writes and ignores old results`, async () => {
    const pending = defer()
    const h = harness({ [stage]: () => pending.promise })
    await h.mount()
    const action = h.instance.recoverArchive(original())
    await flush()
    h.changeIdentity()
    pending.resolve(stage === 'proof' ? { identity: 1 } : stage === 'quote' ? { data: quote() } : 'confirm')
    await action
    assert.deepEqual(h.calls.accept, [])
    assert.deepEqual(h.instance.recoveryAttempts.value, {})
    if (stage === 'proof') assert.deepEqual(h.calls.recoveryQuote, [])
  })
}

test('a late accepted response does not overwrite the next account or auto retry an already sent request', async () => {
  const accept = defer()
  const h = harness({ accept: () => accept.promise })
  await h.mount()
  const action = h.instance.recoverArchive(original())
  await flush()
  assert.deepEqual(h.calls.accept, ['recovery-1'])
  h.changeIdentity()
  accept.resolve({ data: { ...quote(), state: 'FUNDING' } })
  await action
  assert.deepEqual(h.instance.recoveryAttempts.value, {})
  assert.deepEqual(h.instance.runs.value, [])
  assert.equal(h.emitted.length, 0)
})

test('unmount before confirmation finishes prevents acceptance', async () => {
  const confirmation = defer()
  const h = harness({ confirm: () => confirmation.promise })
  await h.mount()
  const action = h.instance.recoverArchive(original())
  await flush()
  h.unmounts[0]()
  confirmation.resolve('confirm')
  await action
  assert.deepEqual(h.calls.accept, [])
})

test('disabled admission or unavailable recovery does not request a new quote', async () => {
  for (const [options, run] of [[{ enabled: false }, original()], [{}, { ...original(), archiveRecoveryAvailable: false }]]) {
    const h = harness(options)
    await h.mount()
    await h.instance.recoverArchive(run)
    assert.deepEqual(h.calls.recoveryQuote, [])
    assert.deepEqual(h.calls.accept, [])
  }
})

test('malformed or mismatched quote data never reaches confirmation or accept', async () => {
  for (const changes of [{ archiveRecovery: false }, { archiveRecoverySourceRunId: 'other' }, { nodeId: '-999' }, { runId: 'source-1' }, { archiveRecoveryJobIds: [] }, { reserveQuota: 5 }, { maximumRunSeconds: 0 }, { hourlyPrice: 13 }, { quoteUntilMs: Date.now() - 1 }]) {
    const h = harness({ quote: () => ({ data: { ...quote(), ...changes } }) })
    await h.mount()
    await h.instance.recoverArchive(original())
    assert.equal(h.calls.confirmations.length, 0)
    assert.deepEqual(h.calls.accept, [])
  }
})

test('a quote expiring while confirmation is open cannot be accepted', async () => {
  const value = quote(), confirmation = defer()
  const h = harness({ quote: () => ({ data: value }), confirm: () => confirmation.promise })
  await h.mount()
  const action = h.instance.recoverArchive(original())
  await flush()
  value.quoteUntilMs = Date.now() - 1
  confirmation.resolve('confirm')
  await action
  assert.deepEqual(h.calls.accept, [])
})

test('recovery run cannot route to new training while a normal running order still can', async () => {
  const h = harness()
  await h.mount()
  await h.instance.prepareTraining({ ...quote(), state: 'RUNNING' })
  assert.equal(h.calls.health, 0)
  assert.deepEqual(h.calls.route, [])
  await h.instance.prepareTraining({ ...original(), state: 'RUNNING' })
  assert.equal(h.calls.health, 1)
  assert.deepEqual(h.calls.route, [{ path: '/algorithm-training', query: { nodeId: '-6000' } }])
})

test('stop copy limits archive-timeout behavior to official/recovery runs and preserves other suppliers', async () => {
  const h = harness()
  await h.mount()
  await h.instance.stop(original())
  assert.match(h.calls.confirmations[0][0], /到期保留原磁盘和产物/)
  const other = harness({ resources: [{ id: '-6000', provider: 'aliyun', configurationManaged: false, billingType: 'POINTS' }] })
  await other.mount()
  await other.instance.stop(original())
  assert.doesNotMatch(other.calls.confirmations[0][0], /到期保留原磁盘/)
  assert.match(other.calls.confirmations[0][0], /等待产物回存后请求停止/)
})
