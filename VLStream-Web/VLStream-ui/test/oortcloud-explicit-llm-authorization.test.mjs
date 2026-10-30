import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse, compileScript } from 'vue/compiler-sfc'

// Run the actual SFC setup with synthetic sessions and API stubs only.
// This test never reads browser storage, real credentials, or the network.
const source = readFileSync(new URL('../src/layout/OortCloudPopover.vue', import.meta.url), 'utf8')
const compiled = compileScript(parse(source).descriptor, { id: 'explicit-llm-authorization-test' }).content
const executable = compiled.replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm, '').replace('export default', 'return')
const makeComponent = new Function('__deps', `
  const {uiText,computed,onBeforeUnmount,onMounted,reactive,ref,watch,Link,Refresh,SwitchButton,
    ElMessage,ElMessageBox,getModelHubUserInfo,logoutModelHubSession,authorizeOortCloudLlm,
    getPlatformAccessToken,getOortCloudAccountStats,getOortCloudQuotaConfig,
    getOortCloudSubscriptionPlans,getOortCloudSubscriptions,getOortCloudTokenKey,
    getOortCloudTokenList,getOortCloudUsageLogs,clearModelHubAuth,getModelHubAccessToken,
    openOortCloudModelHub,openOortCodexPricing,startModelHubLogin,getOortCloudSession,
    redirectToPlatformLogin,getOortCloudTopupInfo,PLATFORM_ORIGIN,authManager,window}=__deps;
  ${executable}
`)
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
const flush = () => new Promise(resolve => setImmediate(resolve))

function harness(options = {}) {
  const calls = { list: 0, key: 0, post: 0, account: 0, usage: 0, confirm: 0 }
  const mounts = [], unmounts = [], watchers = [], messages = [], listeners = new Map(), posted = []
  const props = { tenantMode: options.mode || 'single' }
  let token = options.loggedOut ? '' : 'fixture-session-a'
  let tenantId = 'fixture-tenant'
  let localIdentity = { token: 'fixture-local-session', platformToken: '', tenantId: 'fixture-local-tenant', revision: 1 }
  const session = () => ({ accessToken: token, tenantId, usesPlatformSession: props.tenantMode === 'multi' })
  const deps = {
    uiText: value => value,
    computed: getter => ({ get value() { return getter() } }),
    ref: value => ({ value }), reactive: value => value,
    onMounted: fn => mounts.push(fn), onBeforeUnmount: fn => unmounts.push(fn), watch: (_, fn) => watchers.push(fn),
    Link: {}, Refresh: {}, SwitchButton: {},
    ElMessage: Object.fromEntries(['success', 'warning', 'error'].map(type => [type, value => messages.push({ type, value })])),
    ElMessageBox: { confirm: (...args) => { calls.confirm++; return options.confirm ? options.confirm(...args) : Promise.resolve('confirm') } },
    getOortCloudSession: session,
    authManager: {
      getIdentitySnapshot: () => ({ ...localIdentity }),
      isIdentityCurrent: identity => Object.keys(localIdentity).every(key => identity[key] === localIdentity[key])
    },
    getModelHubUserInfo: async (_, current) => ({ code: 200, data: { userId: current.accessToken, userName: 'Fixture account' } }),
    getOortCloudAccountStats: async () => { calls.account++; return { total_credits: 120, used_credits: 20, remaining_credits: 100 } },
    getOortCloudSubscriptions: async () => ({ subscriptions: [] }),
    getOortCloudSubscriptionPlans: async () => [], getOortCloudQuotaConfig: async () => ({ credits_per_cny: 25 }),
    getOortCloudUsageLogs: async () => { calls.usage++; return { items: [], total: 0 } },
    getOortCloudTokenList: async () => { calls.list++; return options.list ? options.list() : [{ id: 'fixture-disabled', status: 0 }, { id: 'fixture-enabled', status: 1 }] },
    getOortCloudTokenKey: async id => { calls.key++; assert.equal(id, 'fixture-enabled'); return options.key ? options.key() : 'fixture-api-identity' },
    authorizeOortCloudLlm: async (data, identity, guard) => {
      assert.equal(deps.authManager.isIdentityCurrent(identity), true)
      assert.equal(guard(), true)
      calls.post++; posted.push(data); return options.post ? options.post(data) : { code: 200 }
    },
    logoutModelHubSession: async () => { token = '' }, clearModelHubAuth: () => { token = '' },
    getModelHubAccessToken: () => token, getPlatformAccessToken: () => '',
    openOortCloudModelHub() {}, openOortCodexPricing() {}, startModelHubLogin() {}, redirectToPlatformLogin() {},
    getOortCloudTopupInfo: async () => ({}), PLATFORM_ORIGIN: 'https://fixture.invalid',
    window: { addEventListener: (name, fn) => listeners.set(name, fn), removeEventListener: name => listeners.delete(name) }
  }
  const instance = makeComponent(deps).setup(props, { expose() {} })
  return { instance, calls, posted, messages, mounts, unmounts, watchers, props,
    switchSession: value => { token = value }, switchTenant: value => { tenantId = value },
    switchLocalIdentity: () => { localIdentity = { ...localIdentity, token: 'fixture-new-local-session', revision: 2 } },
    authChanged: () => listeners.get('modelHubAuthChanged')(),
    mount: () => mounts[0]() }
}

function noAuthorization(h) {
  assert.equal(h.calls.list, 0)
  assert.equal(h.calls.key, 0)
  assert.equal(h.calls.post, 0)
}

test('mount, account opening, and SSO authChanged retain account reads without implicit LLM authorization', async () => {
  const h = harness()
  await h.mount()
  await h.instance.handlePopoverShow()
  await h.authChanged()
  assert.equal(h.calls.account, 3)
  assert.equal(h.calls.usage, 3)
  assert.equal(h.instance.account.remainingCredits, 100)
  assert.equal(h.instance.isLoggedIn.value, true)
  noAuthorization(h)
  assert.equal(h.calls.confirm, 0)
})

test('template offers the explicit action only in the logged-in single-tenant account', () => {
  assert.match(source, /v-if="!usesPlatformSession"[^>]+@click="handleAuthorizeLlm"/)
  assert.match(source, /确认授权/)
  assert.match(source, /已有视觉复核身份会被替换/)
})

test('no key or token-list read occurs until confirmation; confirmed action uses the captured account once', async () => {
  const confirmation = deferred()
  const h = harness({ confirm: () => confirmation.promise })
  await h.mount()
  const action = h.instance.handleAuthorizeLlm()
  await flush()
  noAuthorization(h)
  confirmation.resolve('confirm')
  await action
  assert.deepEqual([h.calls.list, h.calls.key, h.calls.post], [1, 1, 1])
  assert.deepEqual(h.posted[0], { platformUserId: 'fixture-session-a', platformUserName: 'Fixture account', apiKey: 'fixture-api-identity' })
  assert.equal(h.instance.llmAuthorizationSynced.value, true)
  assert.equal(h.instance.llmAuthorizationLoading.value, false)
})

test('cancel or closing the confirmation performs no credential reads or POST', async () => {
  for (const reason of ['cancel', 'close']) {
    const h = harness({ confirm: () => Promise.reject(reason) })
    await h.mount()
    await h.instance.handleAuthorizeLlm()
    noAuthorization(h)
    assert.equal(h.messages.length, 0)
    assert.equal(h.instance.llmAuthorizationLoading.value, false)
  }
})

test('logged-out and multi-tenant sessions cannot trigger this single-tenant authorization', async () => {
  for (const options of [{ loggedOut: true }, { mode: 'multi' }]) {
    const h = harness(options)
    await h.mount()
    await h.instance.handleAuthorizeLlm()
    noAuthorization(h)
    assert.equal(h.calls.confirm, 0)
  }
})

test('duplicate clicks while confirming or posting create only one request', async () => {
  const confirmation = deferred(), post = deferred()
  const h = harness({ confirm: () => confirmation.promise, post: () => post.promise })
  await h.mount()
  const first = h.instance.handleAuthorizeLlm()
  await h.instance.handleAuthorizeLlm()
  assert.equal(h.calls.confirm, 1)
  confirmation.resolve('confirm')
  await flush()
  assert.equal(h.calls.post, 1)
  await h.instance.handleAuthorizeLlm()
  post.resolve({ code: 200 })
  await first
  await h.instance.handleAuthorizeLlm()
  await h.instance.loadAccount()
  assert.equal(h.calls.post, 1)
  assert.equal(h.calls.confirm, 1)
})

for (const phase of ['confirm', 'list', 'key']) {
  test(`account switch during ${phase} stops the pending flow before authorization POST`, async () => {
    const pending = deferred()
    const h = harness({ [phase]: () => pending.promise })
    await h.mount()
    const action = h.instance.handleAuthorizeLlm()
    await flush()
    h.switchSession('fixture-session-b')
    await h.authChanged()
    pending.resolve(phase === 'list' ? [{ id: 'fixture-enabled', status: 1 }] : 'fixture-result')
    await action
    assert.equal(h.calls.post, 0)
    assert.equal(h.instance.llmAuthorizationSynced.value, false)
    assert.equal(h.instance.account.userId, 'fixture-session-b')
    if (phase !== 'key') assert.equal(h.calls.key, 0)
  })
}

test('tenant or mode change invalidates confirmation even when the token is unchanged', async () => {
  for (const change of [h => h.switchTenant('fixture-other-tenant'), h => { h.props.tenantMode = 'multi' }, h => h.switchLocalIdentity()]) {
    const confirmation = deferred()
    const h = harness({ confirm: () => confirmation.promise })
    await h.mount()
    const action = h.instance.handleAuthorizeLlm()
    change(h)
    confirmation.resolve('confirm')
    await action
    noAuthorization(h)
  }
})

test('auth reset or unmount invalidates a pending request, including switching away and back', async () => {
  for (const unmount of [false, true]) {
    const key = deferred()
    const h = harness({ key: () => key.promise })
    await h.mount()
    const action = h.instance.handleAuthorizeLlm()
    await flush()
    if (unmount) h.unmounts[0]()
    else {
      h.switchSession('fixture-session-b'); await h.authChanged()
      h.switchSession('fixture-session-a'); await h.authChanged()
    }
    key.resolve('fixture-api-identity')
    await action
    assert.equal(h.calls.post, 0)
    assert.equal(h.instance.llmAuthorizationSynced.value, false)
  }
})

test('late POST completion cannot mark a different account authorized or show success', async () => {
  const post = deferred()
  const h = harness({ post: () => post.promise })
  await h.mount()
  const action = h.instance.handleAuthorizeLlm()
  await flush()
  assert.equal(h.calls.post, 1)
  h.switchSession('fixture-session-b')
  await h.authChanged()
  post.resolve({ code: 200 })
  await action
  assert.equal(h.instance.llmAuthorizationSynced.value, false)
  assert.equal(h.messages.length, 0)
})

test('missing enabled identity or empty key never sends an authorization POST', async () => {
  for (const options of [{ list: () => [] }, { key: () => '' }]) {
    const h = harness(options)
    await h.mount()
    await h.instance.handleAuthorizeLlm()
    assert.equal(h.calls.post, 0)
    assert.equal(h.instance.llmAuthorizationSynced.value, false)
  }
})

test('authorization failures never echo a remote payload or automatically retry', async () => {
  for (const options of [
    { post: () => Promise.reject({ response: { data: { msg: 'fixture-sensitive-payload' } } }) },
    { post: () => ({ code: 500, msg: 'fixture-sensitive-payload' }) },
    { key: () => Promise.reject(Error('fixture-sensitive-payload')) }
  ]) {
    const h = harness(options)
    await h.mount()
    await h.instance.handleAuthorizeLlm()
    assert.equal(h.instance.llmAuthorizationSynced.value, false)
    assert.equal(h.instance.llmAuthorizationLoading.value, false)
    assert.equal(h.messages.length, 1)
    assert.equal(h.messages[0].type, 'error')
    assert.doesNotMatch(h.messages[0].value, /fixture-sensitive-payload/)
    const posts = h.calls.post
    await h.authChanged()
    assert.equal(h.calls.post, posts)
  }
})
