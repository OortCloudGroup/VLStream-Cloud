import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { extractAuthCallbackParams, stripAuthCallbackParams } from '../src/utils/authCallbackParams.mjs'

const intentSource = readFileSync(new URL('../src/utils/oortCloudLoginIntent.mjs', import.meta.url), 'utf8')
const hubSource = readFileSync(new URL('../src/utils/modelHubAuth.js', import.meta.url), 'utf8')
const authSource = readFileSync(new URL('../src/utils/auth.js', import.meta.url), 'utf8')
function intentModule() {
  return new Function(intentSource.replace(/^export /gm, '') + ';return {markOortCloudLogin,getOortCloudLoginIntent,cancelOortCloudLoginIntent,finishOortCloudLoginIntent}')()
}
const storage = () => {
  const map = new Map()
  return { getItem: k => map.get(k) ?? null, setItem: (k, v) => map.set(k, String(v)), removeItem: k => map.delete(k) }
}
function callbackHarness() {
  const login = intentModule(), sessionStorage = storage(), localStorage = storage()
  const window = { location: new URL('https://fixture.invalid/bus/vls-ui/llm-provider-management'),
    history: { replaceState: (_s, _t, url) => { window.location = new URL(url, window.location.origin) } },
    dispatchEvent() {} }
  const env = { BASE_URL: '/bus/vls-ui/' }
  const hub = new Function('window', 'sessionStorage', 'localStorage', 'env', 'extractAuthCallbackParams', 'stripAuthCallbackParams', 'markOortCloudLogin', 'cancelOortCloudLoginIntent', 'CustomEvent',
    hubSource.replace(/^import .*$/gm, '').replace(/import\.meta\.env/g, 'env').replace(/^export /gm, '')
      + ';return {startModelHubLogin,capturePendingModelHubCallback,clearModelHubAuth}')(
    window, sessionStorage, localStorage, env, extractAuthCallbackParams, stripAuthCallbackParams,
    login.markOortCloudLogin, login.cancelOortCloudLoginIntent, class {})
  return { login, hub, window, sessionStorage, localStorage }
}
const session = { accessToken: 'fixture-platform', tenantId: 'fixture-tenant', usesPlatformSession: false }

test('single-tenant callback keeps a login intent until layout initialization', () => {
  const h = callbackHarness()
  h.hub.startModelHubLogin(null, { returnToCurrent: true })
  h.window.location = new URL('https://fixture.invalid/bus/vls-ui/llm-provider-management?accessToken=fixture-platform&tenantId=fixture-tenant')
  h.hub.capturePendingModelHubCallback()
  assert.ok(h.login.getOortCloudLoginIntent(session))
  assert.equal(h.sessionStorage.getItem('modelHubTenantId'), session.tenantId)
  assert.equal(h.window.location.search, '')
  // Simulate a full reload: remembered credentials do not create an intent.
  assert.equal(intentModule().getOortCloudLoginIntent(session), null)
  h.hub.clearModelHubAuth()
  assert.equal(h.login.getOortCloudLoginIntent(session), null)
})

test('cancelled, failed, unrequested and expired callbacks cannot restore authorization', () => {
  for (const kind of ['cancelled', 'failed', 'unrequested', 'expired']) {
    const h = callbackHarness()
    if (kind !== 'unrequested') h.hub.startModelHubLogin(null, { returnToCurrent: true })
    if (kind === 'expired') h.sessionStorage.setItem('modelHubAuthPendingAt', Date.now() - 31 * 60 * 1000)
    const query = kind === 'cancelled' ? '?error=cancelled' : '?accessToken=fixture-platform&tenantId=fixture-tenant' + (kind === 'failed' ? '&error=failed' : '')
    h.window.location = new URL('https://fixture.invalid/bus/vls-ui/llm-provider-management' + query)
    h.hub.capturePendingModelHubCallback()
    assert.equal(h.login.getOortCloudLoginIntent(session), null, kind)
  }
})

test('intent belongs to its exact cloud session, tenant and mode', () => {
  const login = intentModule()
  login.markOortCloudLogin(session)
  for (const change of [{ accessToken: 'other' }, { tenantId: 'other' }, { usesPlatformSession: true }]) {
    assert.equal(login.getOortCloudLoginIntent({ ...session, ...change }), null)
  }
})

test('multi-tenant login marks intent only after successful local session exchange', async () => {
  const method = authSource.slice(authSource.indexOf('  async checkUrlToken('), authSource.indexOf('  /* * item after userinterface'))
  for (const succeeds of [true, false]) {
    const login = intentModule()
    const check = new Function('extractAuthCallbackParams', 'markOortCloudLogin', `return ({${method}}).checkUrlToken`)(extractAuthCallbackParams, login.markOortCloudLogin)
    const manager = { cleanUrlToken() {}, clearAllTokens() {},
      exchangePlatformToken: async () => succeeds ? { platformAccessToken: session.accessToken, tenantId: session.tenantId } : null }
    await check.call(manager, 'https://fixture.invalid/?accessToken=fixture-platform&tenantId=fixture-tenant')
    assert.equal(Boolean(login.getOortCloudLoginIntent({ ...session, usesPlatformSession: true })), succeeds)
  }
})
