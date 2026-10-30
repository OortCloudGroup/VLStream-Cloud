import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse, compileScript } from 'vue/compiler-sfc'
import { billingCurrency, paidRunState, paidRunActive, paidRunQuoteText } from '../src/utils/computeBillingPresentation.mjs'

const source = readFileSync(new URL('../src/views/AIComputingScheduling/components/ComputeBillingPanel.vue', import.meta.url), 'utf8')
const compiled = compileScript(parse(source).descriptor, { id: 'archive-recovery-test' }).content
const executable = compiled.replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm, '').replace('export default', 'return')
const create = new Function('__deps', `
  const {computed,onMounted,onUnmounted,ref,useRouter,ElMessageBox,getComputeBillingCapabilities,
    checkComputeBillingAccount,quoteComputePaidRun,quoteComputeArchiveRecovery,listComputePaidRuns,
    acceptComputePaidRun,stopComputePaidRun,refreshComputeHealth,captureComputeIdentity,
    requireComputePlatformAuthorization,isComputeAuthorizationCurrent,billingCurrency,paidRunState,
    paidRunActive,paidRunQuoteText,window,setInterval,clearInterval}=__deps;
  ${executable}
`)
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
    getComputeBillingCapabilities: async () => { calls.capabilities++; return { data: { accountAvailable: true, paidAdmissionEnabled: options.enabled !== false } } },
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
    billingCurrency, paidRunState, paidRunActive, paidRunQuoteText,
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

test('older pending artifacts and an active recovery stay visible beyond the recent five runs', async () => {
  const h = harness()
  const recent = Array.from({ length: 5 }, (_, index) => ({ ...original(), runId: `recent-${index}`, archiveRecoveryAvailable: false }))
  const active = { ...quote(), state: 'RUNNING' }
  h.setRows([...recent, { ...original(), runId: 'old-complete', archiveRecoveryAvailable: false }, original(), active])
  await h.mount()
  assert.deepEqual(h.instance.visibleRuns.value.map(run => run.runId), [...recent.map(run => run.runId), 'source-1', 'recovery-1'])
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
