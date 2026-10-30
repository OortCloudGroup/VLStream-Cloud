import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import axios from 'axios'

// Production API, identity wrapper and auth-header function on synthetic Axios
// adapters. No browser session, supplier, wallet, or credential access occurs.
const apiSource = readFileSync(new URL('../src/api/computeBilling.js', import.meta.url), 'utf8')
const identitySource = readFileSync(new URL('../src/utils/computeIdentityRequest.js', import.meta.url), 'utf8')
const requestSource = readFileSync(new URL('../src/utils/request.js', import.meta.url), 'utf8')
const strip = source => source.replace(/^import .*$/gm, '').replace(/^export /gm, '')
const identityFactory = new Function('request', 'authManager', 'getOortCloudSession', 'isMultiTenantMode', 'window',
  strip(identitySource) + '\nreturn { currentIdentityRequest, captureComputeIdentity, computePathPart };')
const apiFactory = new Function('currentIdentityRequest', 'computePathPart', 'captureComputeIdentity', 'axios',
  strip(apiSource) + '\nreturn { quoteComputeArchiveRecovery, acceptComputePaidRun, listComputePaidRuns };')
const headerFunction = requestSource.slice(requestSource.indexOf('export const applyAuthHeaders ='), requestSource.indexOf('\n}', requestSource.indexOf('export const applyAuthHeaders =')) + 2)
const headersFactory = new Function('__deps', `
  const {isTrustedApiRequest,clearIdentityHeaders,captureRequestIdentity,isSessionRedirecting,
    getLocalSessionToken,sessionExpiryError,getPlatformAccessToken,isLocalEventReadRequest,getBaseURL,
    window,isMultiTenantMode,replaceHeader,sessionValue}=__deps;
  ${headerFunction.replace('export const', 'const')}
  return applyAuthHeaders;
`)
const defer = () => {
  let resolve
  const promise = new Promise(yes => { resolve = yes })
  return { promise, resolve }
}
const flush = () => new Promise(resolve => setImmediate(resolve))

function harness(options = {}) {
  let identity = { token: 'fixture-local-a', platformToken: 'fixture-bound-platform', tenantId: 'fixture-tenant-a', revision: 1 }
  let proof = 'fixture-independent-sso-a'
  const sent = [], events = new Map()
  const request = axios.create({ baseURL: 'https://fixture.invalid', adapter: async config => {
    sent.push(config)
    if (options.pending) await options.pending.promise
    return { data: { code: 200, data: {} }, status: 200, statusText: 'OK', headers: {}, config }
  } })
  const authManager = {
    getIdentitySnapshot: () => ({ ...identity }),
    isIdentityCurrent: snapshot => Object.keys(identity).every(key => identity[key] === snapshot[key])
  }
  const fakeWindow = { location: { origin: 'https://fixture.invalid' }, addEventListener: (name, fn) => events.set(name, fn) }
  const helper = identityFactory(request, authManager, () => ({ accessToken: proof }), () => false, fakeWindow)
  const applyHeaders = headersFactory({
    isTrustedApiRequest: () => true, clearIdentityHeaders() {},
    captureRequestIdentity: config => { config.vlsIdentity = { ...identity } },
    isSessionRedirecting: () => false, getLocalSessionToken: () => identity.token,
    sessionExpiryError: () => Error('fixture-expired'), getPlatformAccessToken: () => identity.platformToken,
    isLocalEventReadRequest: () => false, getBaseURL: () => '', window: fakeWindow, isMultiTenantMode: () => false,
    replaceHeader: (headers, name, value) => {
      for (const key of Object.keys(headers)) if (key.toLowerCase() === name.toLowerCase()) delete headers[key]
      if (value !== undefined) headers[name] = value
    }, sessionValue: keys => keys.includes('tenantId') ? identity.tenantId : ''
  })
  request.interceptors.request.use(config => { applyHeaders(config); return config })
  const api = apiFactory(helper.currentIdentityRequest, helper.computePathPart, helper.captureComputeIdentity, axios)
  return { api, request, sent,
    changeIdentity: () => { identity = { token: 'fixture-local-b', platformToken: 'fixture-bound-b', tenantId: 'fixture-tenant-b', revision: 2 } },
    changeProof: () => { proof = 'fixture-independent-sso-b'; events.get('modelHubAuthChanged')() } }
}

test('recovery quote calls only the source-run endpoint with no financial body', async () => {
  const h = harness()
  await h.api.quoteComputeArchiveRecovery('source-1')
  assert.equal(h.sent.length, 1)
  const config = h.sent[0]
  assert.equal(config.url, '/vlsCompute/billing/runs/source-1/archive-recovery-quote')
  assert.equal(config.method, 'post')
  assert.equal(config.data, undefined)
  assert.equal(config.headers.get('Authorization'), 'Bearer fixture-local-a')
  assert.equal(config.headers.get('accesstoken'), 'fixture-independent-sso-a')
  assert.equal(config.headers.get('tenantId'), 'fixture-tenant-a')
  assert.equal(config.silentError, true)
})

test('explicit acceptance preserves the original accept contract and JSON serialization', async () => {
  const h = harness()
  await h.api.acceptComputePaidRun('recovery-1')
  const config = h.sent[0]
  assert.equal(config.url, '/vlsCompute/billing/runs/recovery-1/accept')
  assert.match(config.headers.get('Content-Type'), /application\/json/)
  assert.deepEqual(JSON.parse(config.data), { acceptBillingPermissions: true })
  assert.equal(config.localAuthToken, 'fixture-local-a')
  assert.equal(config.platformAuthToken, 'fixture-independent-sso-a')
})

test('list remains a GET and does not call account check or acceptance', async () => {
  const h = harness()
  await h.api.listComputePaidRuns()
  assert.equal(h.sent.length, 1)
  assert.equal(h.sent[0].method, 'get')
  assert.equal(h.sent[0].url, '/vlsCompute/billing/runs')
})

test('identity change before identity-wrapper dispatch prevents sending', async () => {
  const h = harness()
  const pending = h.api.acceptComputePaidRun('recovery-1')
  h.changeIdentity()
  await assert.rejects(pending, { code: 'COMPUTE_IDENTITY_CHANGED' })
  assert.equal(h.sent.length, 0)
})

test('identity change inside a deferred Axios interceptor is stopped before transport', async () => {
  const h = harness()
  h.request.interceptors.request.use(async config => {
    await Promise.resolve()
    h.changeIdentity()
    return config
  })
  await assert.rejects(h.api.acceptComputePaidRun('recovery-1'), { code: 'COMPUTE_IDENTITY_CHANGED' })
  assert.equal(h.sent.length, 0)
})

test('independent SSO change during dispatch also prevents acceptance', async () => {
  const h = harness()
  h.request.interceptors.request.use(async config => {
    await Promise.resolve()
    h.changeProof()
    return config
  })
  await assert.rejects(h.api.acceptComputePaidRun('recovery-1'), { code: 'COMPUTE_IDENTITY_CHANGED' })
  assert.equal(h.sent.length, 0)
})

test('an already sent request remains sent when its late response is discarded after a switch', async () => {
  const pending = defer(), h = harness({ pending })
  const action = h.api.acceptComputePaidRun('recovery-1')
  await flush()
  assert.equal(h.sent.length, 1)
  h.changeIdentity()
  pending.resolve()
  await assert.rejects(action, { code: 'COMPUTE_IDENTITY_CHANGED' })
  assert.equal(h.sent.length, 1)
})

test('invalid source IDs cannot redirect the recovery request outside the original endpoint', () => {
  const h = harness()
  for (const id of ['', '../other', 'a/b', '%2f', 'https://other.invalid', 'a?query']) {
    assert.throws(() => h.api.quoteComputeArchiveRecovery(id), { code: 'COMPUTE_DESTINATION_INVALID' })
  }
  assert.equal(h.sent.length, 0)
})
