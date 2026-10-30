import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import axios, { CanceledError } from 'axios'

// Exercise the real API function, Axios transforms, and production auth-header
// function against synthetic adapters. No browser credentials or network access.
const apiSource = readFileSync(new URL('../src/api/llmReview.js', import.meta.url), 'utf8')
const requestSource = readFileSync(new URL('../src/utils/request.js', import.meta.url), 'utf8')
const apiFactory = new Function('request', 'authManager', 'CanceledError',
  apiSource.replace(/^import .*$/gm, '').replace(/^export /gm, '') + '\nreturn { authorizeOortCloudLlm, getLlmProviders };')
const headerFunction = requestSource.slice(requestSource.indexOf('export const applyAuthHeaders ='), requestSource.indexOf('\n}', requestSource.indexOf('export const applyAuthHeaders =')) + 2)
const headersFactory = new Function('__deps', `
  const {isTrustedApiRequest,clearIdentityHeaders,captureRequestIdentity,isSessionRedirecting,
    getLocalSessionToken,sessionExpiryError,getPlatformAccessToken,isLocalEventReadRequest,getBaseURL,
    window,isMultiTenantMode,replaceHeader,sessionValue}=__deps;
  ${headerFunction.replace('export const', 'const')}
  return applyAuthHeaders;
`)

function harness() {
  let current = { token: 'fixture-local-a', platformToken: 'fixture-platform-a', tenantId: 'fixture-tenant-a', revision: 1 }
  let sessionCurrent = true
  const sent = []
  const authManager = {
    getIdentitySnapshot: () => ({ ...current }),
    isIdentityCurrent: snapshot => Object.keys(current).every(key => snapshot[key] === current[key])
  }
  const request = axios.create({ baseURL: 'https://fixture.invalid', adapter: async config => {
    sent.push(config)
    return { data: { code: 200 }, status: 200, statusText: 'OK', headers: {}, config }
  } })
  const applyAuthHeaders = headersFactory({
    isTrustedApiRequest: () => true, clearIdentityHeaders() {},
    captureRequestIdentity: config => { config.vlsIdentity = { ...current } },
    isSessionRedirecting: () => false, getLocalSessionToken: () => current.token,
    sessionExpiryError: () => Error('fixture-session-expired'), getPlatformAccessToken: () => current.platformToken,
    isLocalEventReadRequest: () => false, getBaseURL: () => '',
    window: { location: { origin: 'https://fixture.invalid' } }, isMultiTenantMode: () => false,
    replaceHeader: (headers, name, value) => {
      for (const key of Object.keys(headers)) if (key.toLowerCase() === name.toLowerCase()) delete headers[key]
      if (value !== undefined) headers[name] = value
    },
    sessionValue: keys => keys.includes('tenantId') ? current.tenantId : ''
  })
  request.interceptors.request.use(config => { applyAuthHeaders(config); return config })
  const api = apiFactory(request, authManager, CanceledError)
  const identity = authManager.getIdentitySnapshot()
  return { api, request, identity, sent,
    guard: () => sessionCurrent,
    changeSession: () => { sessionCurrent = false },
    changeIdentity: () => { current = { token: 'fixture-local-b', platformToken: 'fixture-platform-b', tenantId: 'fixture-tenant-b', revision: 2 } } }
}

const payload = { platformUserId: 'fixture-user', platformUserName: 'Fixture', apiKey: 'fixture-api-identity' }

test('authorization binds local and platform identities while preserving JSON serialization and content type', async () => {
  const h = harness()
  await h.api.authorizeOortCloudLlm(payload, h.identity, h.guard)
  assert.equal(h.sent.length, 1)
  const config = h.sent[0]
  assert.equal(config.url, '/vlsLlmReview/oortcloud-authorization')
  assert.equal(config.method, 'post')
  assert.equal(config.headers.get('Authorization'), 'Bearer fixture-local-a')
  assert.equal(config.headers.get('blade-auth'), 'Bearer fixture-local-a')
  assert.equal(config.headers.get('accesstoken'), 'fixture-platform-a')
  assert.equal(config.headers.get('tenantId'), 'fixture-tenant-a')
  assert.match(config.headers.get('Content-Type'), /application\/json/)
  assert.equal(typeof config.data, 'string')
  assert.deepEqual(JSON.parse(config.data), payload)
  assert.equal(config.silentError, true)
})

test('missing or already-stale local identity prevents creating a request', () => {
  const h = harness()
  assert.throws(() => h.api.authorizeOortCloudLlm(payload), error => axios.isCancel(error))
  h.changeIdentity()
  assert.throws(() => h.api.authorizeOortCloudLlm(payload, h.identity, h.guard), error => axios.isCancel(error))
  assert.equal(h.sent.length, 0)
})

test('switch after API call but before asynchronous interceptors cancels before the adapter', async () => {
  const h = harness()
  const pending = h.api.authorizeOortCloudLlm(payload, h.identity, h.guard)
  h.changeIdentity()
  await assert.rejects(pending, error => axios.isCancel(error))
  assert.equal(h.sent.length, 0)
})

test('switch during an async interceptor is rechecked after it and before serialization/transport', async () => {
  const h = harness()
  h.request.interceptors.request.use(async config => {
    await Promise.resolve()
    h.changeIdentity()
    return config
  })
  await assert.rejects(h.api.authorizeOortCloudLlm(payload, h.identity, h.guard), error => axios.isCancel(error))
  assert.equal(h.sent.length, 0)
})

test('independent OortCloud session change before transport also cancels the request', async () => {
  const h = harness()
  const pending = h.api.authorizeOortCloudLlm(payload, h.identity, h.guard)
  h.changeSession()
  await assert.rejects(pending, error => axios.isCancel(error))
  assert.equal(h.sent.length, 0)
})

test('empty captured platform token remains empty instead of taking a later session token', async () => {
  const h = harness()
  // A synthetic manager allows this fixed local-only snapshot for the dispatch check.
  const api = apiFactory(h.request, { isIdentityCurrent: () => true }, CanceledError)
  await api.authorizeOortCloudLlm(payload, { ...h.identity, platformToken: '' })
  assert.equal(h.sent[0].platformAuthToken, '')
  assert.equal(h.sent[0].headers.get('accesstoken'), 'fixture-local-a')
})

test('other LLM API calls retain their existing request behavior', async () => {
  const h = harness()
  await h.api.getLlmProviders()
  assert.equal(h.sent.length, 1)
  assert.equal(h.sent[0].method, 'get')
  assert.equal(h.sent[0].url, '/vlsLlmReview/providers')
  assert.equal(h.sent[0].localAuthToken, undefined)
})
