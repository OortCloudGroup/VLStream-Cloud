// Actual Axios interceptors and AuthManager, isolated storage/adapters; no backend or production traffic.
import assert from 'node:assert/strict'
import path from 'node:path'
import { mkdtempSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { fileURLToPath, pathToFileURL } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const { createServer } = await import(pathToFileURL(path.join(root, 'node_modules/vite/dist/node/index.js')))
const { default: axios } = await import(pathToFileURL(path.join(root, 'node_modules/axios/index.js')))
const storage = () => {
  const values = new Map()
  return { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, String(value)),
    removeItem: key => values.delete(key), clear: () => values.clear() }
}
globalThis.sessionStorage = storage()
globalThis.localStorage = storage()
globalThis.sessionTestMessages = []
globalThis.sessionTestRedirects = []
globalThis.window = { sessionStorage, localStorage, clearTimeout,
  location: { origin: 'https://fixture.invalid:9443', href: 'https://fixture.invalid:9443/bus/vls-ui/workspace?tab=2#list',
    search: '', hash: '', replace: url => sessionTestRedirects.push(url) } }
globalThis.location = window.location
const mocks = {
  'element-plus': `const message=value=>{globalThis.sessionTestMessages.push(value);return {close(){}}}; for(const type of ['error','warning','success','info']) message[type]=value=>{globalThis.sessionTestMessages.push({type,value});return {close(){}}}; export const ElMessage=message; export const ElMessageBox={alert(){throw Error('unexpected dialog')}}`,
  'file-saver': 'export const saveAs=()=>{}',
  '@/i18n': 'export const currentLocale={value:"zh-CN"}; export const translateUiMessage=v=>v',
  '@/config': 'export default {headers:{}}',
  '@/store/modules/useErrorMsg': 'export const useErrorMsgStoreHook=()=>({addErrorMsg(){}})'
}
const server = await createServer({ root, configFile: false, base: '/bus/vls-ui/',
  ssr: { noExternal: ['element-plus', 'file-saver'] },
  resolve: { alias: { '@': path.join(root, 'src') } },
  plugins: [{ name: 'session-test-mocks', enforce: 'pre',
    resolveId: id => id in mocks ? '\0mock:' + id : undefined,
    load: id => {
      if (id.startsWith('\0mock:')) return mocks[id.slice(6)]
      const normalized = id.replaceAll('\\', '/')
      for (const [suffix, mock] of [['/src/i18n/index.js', '@/i18n'], ['/src/config/index.ts', '@/config'],
        ['/src/store/modules/useErrorMsg.ts', '@/store/modules/useErrorMsg']]) {
        if (normalized.endsWith(suffix)) return mocks[mock]
      }
    } }],
  cacheDir: mkdtempSync(path.join(tmpdir(), 'vls-session-test-')), logLevel: 'error',
  server: { middlewareMode: true, hmr: false }, appType: 'custom' })
let checks = 0
const check = (condition, message) => { assert.ok(condition, message); checks++ }
try {
  const vls = await server.ssrLoadModule('/src/utils/request.js')
  const { default: wvp } = await server.ssrLoadModule('/src/utils/wvpRequest.js')
  const { service } = await server.ssrLoadModule('/src/utils/service.ts')
  const { AuthManager } = await server.ssrLoadModule('/src/utils/auth.js')
  const expiry = await server.ssrLoadModule('/src/utils/sessionExpiry.js')
  const authCheckState = await server.ssrLoadModule('/src/utils/authCheckState.js')
  const { bootstrapRequestIdentity, sessionValue } = await server.ssrLoadModule('/src/utils/requestIdentity.mjs')
  const redirects = await server.ssrLoadModule('/src/utils/authRedirect.mjs')
  const platform = await server.ssrLoadModule('/src/utils/platformSession.js')
  const localEvents = await server.ssrLoadModule('/src/utils/localEventAccess.mjs')
  const { stripAuthCallbackParams } = await server.ssrLoadModule('/src/utils/authCallbackParams.mjs')
  const { ElMessage } = await server.ssrLoadModule('\0mock:element-plus')
  expiry.installSessionMessageGuard(ElMessage)
  const clients = [vls.default, vls.authRequest, vls.imageUploadRequest, wvp, service]
  const identity = (mode = 'single', token = 'fixture-local') => {
    localEvents.selectRouteAuthScope({})
    sessionStorage.clear(); localStorage.clear(); sessionTestMessages.length = 0; sessionTestRedirects.length = 0
    expiry.advanceIdentityRevision()
    for (const store of [sessionStorage, localStorage]) {
      for (const [key, value] of Object.entries({ accessToken: token, token, tenantId: 'fixture-tenant',
        platformAccessToken: 'fixture-platform', userInfo: '{}', wvpAccessToken: 'fixture-wvp' })) store.setItem(key, value)
    }
    sessionStorage.setItem('vlsTenantMode', mode)
  }
  const adapter = (data, status = 200) => async config => {
    const response = { config, data, status, headers: {}, statusText: 'fixture' }
    if (status >= 400) throw new axios.AxiosError('fixture HTTP failure', 'ERR_BAD_REQUEST', config, {}, response)
    return response
  }
  const expectExpired = promise => assert.rejects(promise, expiry.isSessionExpiryError)
  const mainSource = readFileSync(path.join(root, 'src/main.js'), 'utf8')
  const bootstrapSource = mainSource.slice(mainSource.indexOf('// VLStream token apaas'), mainSource.indexOf('const app = createApp(App)'))
  const bootstrapMain = () => new Function('installSessionMessageGuard', 'ElMessage', 'window', 'bootstrapRequestIdentity', bootstrapSource)(
    () => {}, {}, { addEventListener() {} }, bootstrapRequestIdentity)
  const persist = (store, suffix, platformToken = true) => {
    store.setItem('accessToken', 'fixture-' + suffix)
    store.setItem('tenantId', 'tenant-' + suffix)
    store.setItem('userInfo', JSON.stringify({ accessToken: 'fixture-' + suffix, tenantId: 'tenant-' + suffix }))
    if (platformToken) store.setItem('platformAccessToken', 'platform-' + suffix)
  }
  for (const mode of ['single', 'multi']) {
    identity(mode); sessionStorage.clear(); localStorage.clear()
    persist(localStorage, 'persisted', mode === 'multi')
    bootstrapMain()
    check(sessionValue(['tenantId']) === 'tenant-persisted' && sessionValue(['accessToken']) === 'fixture-persisted', 'fresh tab lost persisted tenant')
    check(sessionValue(['platformAccessToken']) === (mode === 'multi' ? 'platform-persisted' : ''), 'fresh tab lost platform or borrowed another identity')
  }
  identity(); persist(sessionStorage, 'active'); persist(localStorage, 'other-tab')
  bootstrapMain()
  check(sessionValue(['accessToken']) === 'fixture-active' && sessionValue(['tenantId']) === 'tenant-active'
    && localStorage.getItem('accessToken') === 'fixture-other-tab', 'bootstrap overwrote an active or other-tab account')
  for (const broken of ['', 'null', '{broken', '[]', JSON.stringify({ accessToken: 'different-account' })]) {
    identity(); sessionStorage.clear(); localStorage.clear(); persist(localStorage, 'persisted')
    localStorage.setItem('userInfo', broken)
    bootstrapMain()
    check(sessionValue(['accessToken']) === '' && sessionStorage.getItem('userInfo') === 'null'
      && localStorage.getItem('accessToken') === 'fixture-persisted', 'corrupt persisted identity was adopted or deleted')
  }
  identity(); sessionStorage.clear(); localStorage.clear(); bootstrapMain()
  check(sessionValue(['accessToken']) === '', 'empty storage fabricated identity')
  persist(localStorage, 'other-tab'); bootstrapMain()
  check(sessionValue(['accessToken']) === '', 'explicit sign-out sentinel fell back to another tab')
  identity(); sessionStorage.clear(); localStorage.clear(); persist(localStorage, 'persisted')
  bootstrapMain()
  const startupCalls = []
  vls.authRequest.defaults.adapter = async config => {
    startupCalls.push(config)
    if (config.url.endsWith('/mode')) return { config, status: 200, headers: {}, data: { code: 200, data: { tenantType: 'multi' } } }
    assert.equal(config.headers.get('tenantId'), 'tenant-persisted')
    assert.equal(config.headers.get('accesstoken'), 'platform-persisted')
    return { config, status: 200, headers: {}, data: { code: 200, data: { user: { tenantId: 'tenant-persisted' } } } }
  }
  check(Boolean(await new AuthManager().checkExternalPlatformLogin()), 'persisted multi login failed validation')
  check(startupCalls[0].url.endsWith('/mode') && !startupCalls[0].headers.get('Authorization')
    && startupCalls[1].url.endsWith('/user/info'), 'protected user/info ran before anonymous mode detection')
  for (const mode of ['single', 'multi']) {
    for (const client of clients) {
      for (const [status, code] of [[200, 401], [401, 401], [200, 4004]]) {
        identity(mode)
        await expectExpired(client.get('/fixture-protected', { adapter: adapter({ code, msg: 'fixture expired' }, status) }))
        ElMessage.error('page fallback must also be silent')
        check(sessionTestMessages.length === 0, 'expired request produced a toast: client=' + clients.indexOf(client) + ',mode=' + mode + ',status=' + status + ',code=' + code + ',redirects=' + sessionTestRedirects.length)
        check(sessionTestRedirects.length === 1 && sessionStorage.getItem('accessToken') === null
          && localStorage.getItem('platformAccessToken') === null && vls.getLocalSessionToken() === '', 'incomplete cleanup/redirect')
        const url = new URL(sessionTestRedirects[0])
        check(url.pathname === '/bus/vls-ui/login' && url.searchParams.get('redirect') === '/workspace?tab=2#list', 'base or return path lost')
      }
    }
  }
  for (const client of clients) {
    for (const [status, data] of [[403, { code: 403 }], [200, { code: 403, msg: 'token失效' }],
      [200, { code: 500, msg: 'token失效' }], [500, { code: 401 }], [503, { code: 4004 }]]) {
      identity()
      await client.get('/fixture-failure', { adapter: adapter(data, status) }).catch(() => {})
      check(!sessionTestRedirects.length && vls.getLocalSessionToken() === 'fixture-local', 'permission/server error logged out user')
      check(sessionTestMessages.length > 0, 'ordinary failure was silently swallowed: client=' + clients.indexOf(client) + ', status=' + status + ', code=' + data.code)
    }
    for (const code of ['ERR_CANCELED', 'ECONNABORTED', 'ERR_NETWORK']) {
      identity()
      await client.get('/fixture-transport', { adapter: config => Promise.reject(code === 'ERR_CANCELED'
        ? new axios.CanceledError('fixture cancel', config) : new axios.AxiosError('fixture timeout', code, config, {})) }).catch(() => {})
      check(!sessionTestRedirects.length && vls.getLocalSessionToken() === 'fixture-local', 'transport/cancel cleared identity')
      check(code === 'ERR_CANCELED' ? !sessionTestMessages.length : sessionTestMessages.length > 0, 'wrong transport/cancel toast')
    }
  }
  identity()
  await assert.rejects(vls.authRequest.post('/blade-auth/token', {}, { useBladeClientAuth: true,
    adapter: adapter({ code: 401, msg: 'fixture wrong password' }) }), error => !expiry.isSessionExpiryError(error))
  check(!sessionTestRedirects.length && vls.getLocalSessionToken() === 'fixture-local', 'bad password treated as expired session')

  // Concurrent responses from the same identity must settle with one redirect.
  identity()
  const pending = []
  const deferred = config => new Promise(resolve => pending.push(data => resolve({ config, data, status: 200, headers: {} })))
  const requests = clients.map(client => client.get('/fixture-concurrent', { adapter: deferred }).catch(error => error))
  while (pending.length < clients.length) await new Promise(resolve => setTimeout(resolve, 0))
  pending.forEach(finish => finish({ code: 401 }))
  check((await Promise.all(requests)).every(expiry.isSessionExpiryError) && sessionTestRedirects.length === 1, 'concurrent redirect storm')

  for (const sameToken of [false, true]) {
    identity()
    let finish
    const old = vls.default.get('/fixture-old', { adapter: config => new Promise(resolve => {
      finish = () => resolve({ config, data: { code: 401 }, status: 200, headers: {} })
    }) }).catch(error => error)
    while (!finish) await new Promise(resolve => setTimeout(resolve, 0))
    const manager = new AuthManager()
    manager.invalidatePendingIdentity()
    manager.saveUserToLocal({ accessToken: sameToken ? 'fixture-local' : 'fixture-new',
      tenantId: 'fixture-tenant', platformAccessToken: 'fixture-platform' })
    finish(); await old
    check(!sessionTestRedirects.length && vls.getLocalSessionToken() === (sameToken ? 'fixture-local' : 'fixture-new'), 'late response cleared new login')
  }

  identity('multi')
  // Preserve a different tab's persistent identity and prevent fallback into it.
  for (const [key, value] of Object.entries({ accessToken: 'other-tab', token: 'other-tab', tenantId: 'other-tenant',
    platformAccessToken: 'other-platform', userInfo: '{"accessToken":"other-tab"}' })) localStorage.setItem(key, value)
  await expectExpired(vls.default.get('/fixture-tab', { adapter: adapter({ code: 401 }) }))
  check(localStorage.getItem('accessToken') === 'other-tab' && vls.getLocalSessionToken() === '', 'other tab identity was cleared/adopted')
  check(new AuthManager().getCachedUserInfo() === null, 'signed out tab read another tab cache')

  identity('multi'); sessionStorage.removeItem('platformAccessToken')
  let sent = false
  await expectExpired(vls.default.get('/fixture-missing-platform', { adapter: () => { sent = true } }))
  check(!sent && sessionTestRedirects.length === 1 && !sessionTestMessages.length, 'missing platform state not handled silently')

  identity()
  const manager = new AuthManager()
  vls.authRequest.defaults.adapter = adapter({ code: 500 }, 503)
  await assert.rejects(manager.checkLocalToken())
  check(vls.getLocalSessionToken() === 'fixture-local' && !sessionTestRedirects.length, 'guard cleared login on service outage')
  vls.authRequest.defaults.adapter = adapter({ code: 401 })
  check(await manager.checkLocalToken() === null && sessionTestRedirects.length === 1, 'route verification did not expire')

  for (const base of ['/', '/bus/vls-ui/', '/custom/vls/']) {
    for (const bad of ['https://evil.invalid', '//evil.invalid', '/\\evil.invalid', '/%2f%2fevil.invalid', '/login?redirect=/login', 'javascript:alert(1)', '/bus/other/page']) {
      check(redirects.safeAuthReturnPath(bad, base, window.location.origin) === '/workspace', 'unsafe redirect accepted')
    }
    const url = new URL(redirects.appLoginUrl('/workspace?view=2#devices', base, window.location.origin))
    check(url.origin === window.location.origin && url.pathname === base + 'login', 'deployment path hardcoded')
  }
  check(!redirects.safeAuthReturnPath('/workspace?token=fixture&tab=2', '/bus/vls-ui/').includes('token='), 'credential in return URL')
  const platformUrl = new URL(platform.buildPlatformLoginUrl('//evil.invalid'))
  check(new URL(platformUrl.searchParams.get('redirect_uri')).origin === window.location.origin, 'platform return escaped origin')

  for (const client of clients) {
    identity()
    await expectExpired(client.get('/fixture-download', { responseType: 'blob',
      adapter: adapter(new Blob([JSON.stringify({ code: 401 })], { type: 'application/json' })) }))
    check(sessionTestRedirects.length === 1 && !sessionTestMessages.length, 'JSON blob expiry was downloaded or toasted')
  }

  // Execute the actual protected-route guard, with only routing/component loading stubbed.
  const routerSource = readFileSync(path.join(root, 'src/router/index.js'), 'utf8')
  const guardSource = routerSource.slice(routerSource.indexOf('router.beforeEach('), routerSource.indexOf('\nexport default router')).replaceAll('import.meta.env.BASE_URL', JSON.stringify('/bus/vls-ui/'))
  const managerForGuard = new AuthManager()
  managerForGuard.hasPlatformCallbackToken = () => false
  let guard
  const routerStub = { beforeEach: value => { guard = value }, resolve: fullPath => ({ fullPath, path: fullPath.split('?')[0], query: {}, hash: '' }) }
  new Function('router', 'authManager', 'stripAuthCallbackParams', 'consumeCapturedModelHubCallback', 'logSafeRequestError', 'safeAuthReturnPath', 'beginAuthCheck', 'failAuthCheck', 'selectRouteAuthScope', 'isLocalEventRoute',
    guardSource)(routerStub, managerForGuard, stripAuthCallbackParams, () => false, () => {}, redirects.safeAuthReturnPath,
    authCheckState.beginAuthCheck, authCheckState.failAuthCheck, localEvents.selectRouteAuthScope, localEvents.isLocalEventRoute)
  const route = { path: '/workspace', fullPath: '/workspace', meta: { requiresAuth: true } }
  identity()
  vls.authRequest.defaults.adapter = adapter({ code: 503 }, 503)
  let nextResult
  await guard(route, {}, value => { nextResult = value })
  check(nextResult === false && vls.getLocalSessionToken() === 'fixture-local' && !sessionTestRedirects.length, 'route guard logged out on outage')
  check(authCheckState.authCheckFailed.value && authCheckState.authCheckRetryPath.value === '/workspace', 'initial cancelled route has no visible recovery state')
  identity('multi')
  vls.authRequest.defaults.adapter = async config => ({ config, status: 200, headers: {}, data: config.url.endsWith('/mode')
    ? { code: 200, data: { tenantType: 'multi' } } : config.url.endsWith('/user/info')
      ? { code: 200, data: { user: { tenantId: 'fixture-tenant' } } }
      : { code: 500, msg: '平台会话已失效，请重新登录' } })
  await guard(route, {}, value => { nextResult = value })
  check(nextResult === false && authCheckState.authCheckFailed.value && authCheckState.authCheckRetryPath.value === '/workspace'
    && vls.getLocalSessionToken() === 'fixture-local' && !sessionTestRedirects.length, 'ambiguous validation 500 leaves a blank route or forces login')

  // Local event navigation executes the real local user-info validation, without SSO polling.
  const eventRoute = { path: '/event-management', fullPath: '/event-management',
    meta: { requiresAuth: true, authScope: localEvents.LOCAL_EVENT_AUTH_SCOPE } }
  check(routerSource.includes("meta: { title: '事件管理', icon: '事件', authScope: LOCAL_EVENT_AUTH_SCOPE }"), 'event route lost its local auth scope')
  for (const platformToken of ['', 'fixture-expired-platform']) {
    identity('multi')
    for (const storage of [sessionStorage, localStorage]) {
      if (platformToken) storage.setItem('platformAccessToken', platformToken)
      else storage.removeItem('platformAccessToken')
    }
    const calls = []
    vls.authRequest.defaults.adapter = async config => {
      calls.push(config)
      assert.ok(!config.url.includes('validatePlatformSession'), 'local event route called SSO validation')
      if (config.url.endsWith('/mode')) return { config, status: 200, headers: {}, data: { code: 200, data: { tenantType: 'multi' } } }
      assert.equal(config.headers.get('Authorization'), 'Bearer fixture-local')
      assert.equal(config.headers.get('accesstoken'), 'fixture-local')
      assert.equal(config.headers.get('tenantId'), 'fixture-tenant')
      return { config, status: 200, headers: {}, data: { code: 200, data: { user: { tenantId: 'fixture-tenant' } } } }
    }
    await guard(eventRoute, route, value => { nextResult = value })
    check(nextResult === undefined && !authCheckState.authCheckFailed.value && localEvents.isLocalEventScopeActive()
      && calls.length === 2 && calls[1].url.endsWith('/user/info'), 'valid local session was blocked by missing/expired platform state')
    check(!sessionTestRedirects.length && !sessionTestMessages.length, 'local event navigation caused SSO redirect/toast')
    const eventApi = await server.ssrLoadModule('/src/api/eventManagement.js')
    const eventCalls = []
    vls.default.defaults.adapter = async config => {
      eventCalls.push(config)
      assert.equal(config.headers.get('accesstoken'), 'fixture-local')
      assert.equal(config.headers.get('tenantId'), 'fixture-tenant')
      return { config, status: 200, headers: {}, data: { code: 200, data: { total: 1, records: [{ id: '23' }] } } }
    }
    await eventApi.getEventPage({ current: 1, size: 10 })
    await eventApi.getEventById('23')
    check(eventCalls.length === 2 && eventCalls.every(config => config.vlsLocalSessionOnly), 'event page/detail did not use local session transport')
    vls.default.defaults.adapter = undefined
    // Classification filters retain local federation; preview/write requests keep platform requirements.
    const classification = { method: 'get', url: '/device/classification/tree', baseURL: '/wvp-api', headers: { tenantid: 'forged' } }
    const { applyWvpHeaders } = await server.ssrLoadModule('/src/utils/wvpRequest.js')
    applyWvpHeaders(classification)
    check(classification.headers.accesstoken === 'fixture-local' && classification.headers['X-VLStream-Token'] === 'fixture-local'
      && classification.headers.tenantid === 'fixture-tenant', 'classification lost local federation or trusted tenant')
    if (!platformToken) {
      assert.throws(() => applyWvpHeaders({ method: 'post', url: '/rtsp/RtspDevice/7/preview', baseURL: '/wvp-api', headers: {} }), error => error.code === 'PLATFORM_AUTH_REQUIRED')
      checks++
    }
  }
  // The allowlist cannot turn a mutation, unrelated API or foreign URL into a local read.
  for (const base of ['', '/bus/apaas-vls-server', 'https://api.fixture.invalid/custom/vls']) {
    for (const api of ['/blade-system/user/info', '/vlsEventManagement/page', '/vlsEventManagement/23', '/system/platform-logo/current']) {
      check(localEvents.isLocalEventReadRequest({ method: 'get', url: api, localSessionOnly: true }, base, window.location.origin), 'configured base rejected local read')
    }
    for (const api of ['/task/v1/event_list', '/task/v1/event_item_list']) {
      check(localEvents.isLocalEventReadRequest({ method: 'post', url: api, localSessionOnly: true }, base, window.location.origin), 'configured base rejected local POST read')
    }
    for (const config of [
      { method: 'delete', url: '/vlsEventManagement/23' }, { method: 'post', url: '/task/v1/event_item_del' },
      { method: 'post', url: '/task/v1/event_item_save' }, { method: 'get', url: '/blade-system/user/list' },
      { method: 'get', url: 'https://foreign.invalid/vlsEventManagement/page' },
      { method: 'get', url: '/vlsEventManagement/page', requirePlatformIdentity: true }
    ]) check(!localEvents.isLocalEventReadRequest({ ...config, localSessionOnly: true }, base, window.location.origin), 'local read scope broadened to a mutation/foreign/privileged API')
  }
  identity('multi'); sessionStorage.removeItem('platformAccessToken'); localStorage.removeItem('platformAccessToken')
  let mutationSent = false
  await expectExpired(vls.default.delete('/vlsEventManagement/23', { localSessionOnly: true,
    adapter: () => { mutationSent = true } }))
  check(!mutationSent && sessionTestRedirects.length === 1, 'caller bypassed platform requirement with a mutation flag')
  for (const mode of ['single', 'multi']) {
    identity(mode); sessionStorage.removeItem('platformAccessToken'); localStorage.removeItem('platformAccessToken')
    const legacyEvents = await server.ssrLoadModule('/src/api/smartCity/events/index.ts')
    const legacyCalls = []
    service.defaults.adapter = async config => {
      legacyCalls.push(config)
      assert.equal(config.headers.get('Authorization'), 'Bearer fixture-local')
      assert.equal(config.headers.get('accesstoken'), 'fixture-local')
      return { config, status: 200, headers: {}, data: { code: 200, data: [] } }
    }
    await legacyEvents.eventList({ accessToken: 'obsolete-body-credential' })
    await legacyEvents.event_item_list({ eventId: '23' })
    check(legacyCalls.length === 2 && legacyCalls.every(config => config.vlsLocalSessionOnly)
      && !legacyCalls[0].data.includes('obsolete-body-credential'), 'single/legacy event reads required SSO or kept legacy body credential')
  }
  identity('multi'); localEvents.selectRouteAuthScope(eventRoute.meta)
  const layoutSource = readFileSync(path.join(root, 'src/layout/index.vue'), 'utf8')
  const userLoaderSource = layoutSource.slice(layoutSource.indexOf('const loadBladeUserInfo = async () => {'), layoutSource.indexOf('// Load info'))
  const profile = { value: {} }, tenant = { value: {} }, tenants = { value: [] }
  const authApi = await server.ssrLoadModule('/src/api/auth.js')
  vls.authRequest.defaults.adapter = async config => {
    assert.equal(config.url, '/blade-system/user/info')
    assert.equal(config.vlsLocalSessionOnly, true)
    return { config, status: 200, headers: {}, data: { code: 200, data: { userName: 'Local event user', tenantId: 'fixture-tenant' } } }
  }
  const loadProfile = new Function('authManager', 'getUserInfo', 'currentTenant', 'currentUser', 'tenantList', 'resolveAdminFlag', 'localEventRoute',
    userLoaderSource + '\nreturn loadBladeUserInfo')(new AuthManager(), authApi.getUserInfo, tenant, profile, tenants, () => false, { value: true })
  check(await loadProfile() && tenant.value.id === 'fixture-tenant' && profile.value.userName === 'Local event user', 'event header did not use trusted local user info')
  const forceSource = layoutSource.slice(layoutSource.indexOf('const forceLoadUserAndTenantInfo = async () => {'), layoutSource.indexOf('// Load userinfo'))
  const forceProfile = new Function('loadBladeUserInfo', 'loadTenantInfo', 'localEventRoute', forceSource + '\nreturn forceLoadUserAndTenantInfo')(
    loadProfile, () => { throw new Error('local event header called platform tenant service') }, { value: true })
  await forceProfile(); checks++
  const rightInfoSource = readFileSync(path.join(root, 'src/layout/RightInfoBar.vue'), 'utf8')
  const expiryCallbackSource = rightInfoSource.slice(rightInfoSource.indexOf('function handleAutomaticExpiry() {'), rightInfoSource.indexOf('async function endLocalSession()'))
  let platformLoginCalls = 0, destroyedSdk = 0
  const automaticSdkExpiry = new Function('isLocalEventScopeActive', 'loginAgain', 'window', 'emit', 'destroyed', `let disposed=false,changingIdentity=false,timer,bar={destroy:destroyed}; ${expiryCallbackSource}\nreturn handleAutomaticExpiry`)(
    localEvents.isLocalEventScopeActive, () => { platformLoginCalls++ }, { clearInterval() {} }, () => {}, () => { destroyedSdk++ })
  automaticSdkExpiry()
  check(!platformLoginCalls && destroyedSdk === 1, 'late SDK expiry cleared local event session')
  localEvents.selectRouteAuthScope(route.meta); automaticSdkExpiry()
  check(platformLoginCalls === 1, 'other routes lost SDK expiry behavior')
  for (const code of [401, 4004]) {
    identity('multi'); localEvents.selectRouteAuthScope(eventRoute.meta)
    await expectExpired(vls.default.get('/vlsEventManagement/page', { localSessionOnly: true, adapter: adapter({ code }) }))
    check(vls.getLocalSessionToken() === '' && sessionTestRedirects.length === 1 && !sessionTestMessages.length, 'expired local event session did not silently return to login')
  }
  for (const code of [403, 500]) {
    identity('multi'); localEvents.selectRouteAuthScope(eventRoute.meta)
    await vls.default.get('/vlsEventManagement/page', { localSessionOnly: true, adapter: adapter({ code }) }).catch(() => {})
    check(vls.getLocalSessionToken() === 'fixture-local' && !sessionTestRedirects.length && sessionTestMessages.length > 0, 'local permission/server error cleared or silently swallowed session')
  }
  identity('multi'); localEvents.selectRouteAuthScope(eventRoute.meta)
  const optionalPlatformConfig = { url: '/fixture-platform', platformIdentityOnly: true, headers: {} }
  vls.applyAuthHeaders(optionalPlatformConfig)
  check(expiry.isSessionExpiryError(vls.handleSessionFailure({ config: optionalPlatformConfig, status: 200, data: { code: 4004 } }))
    && vls.getLocalSessionToken() === 'fixture-local' && !sessionTestRedirects.length, 'late optional SSO response cleared local event session')
  localEvents.selectRouteAuthScope(route.meta)
  vls.handleSessionFailure({ config: optionalPlatformConfig, status: 200, data: { code: 4004 } })
  check(vls.getLocalSessionToken() === '' && sessionTestRedirects.length === 1, 'other routes lost their existing platform expiry handling')
  identity('multi')
  for (const storage of [sessionStorage, localStorage]) { storage.removeItem('accessToken'); storage.removeItem('token') }
  await guard(eventRoute, route, value => { nextResult = value })
  check(nextResult?.path === '/login' && nextResult.query.redirect === '/event-management', 'local events allowed unauthenticated navigation')
  identity()
  let finishGuard
  managerForGuard.checkExternalPlatformLogin = () => new Promise(resolve => { finishGuard = resolve })
  const checkingRoute = guard(route, {}, value => { nextResult = value })
  managerForGuard.saveUserToLocal({ accessToken: 'new-guard-login', tenantId: 'new-tenant' })
  finishGuard(null); await checkingRoute
  check(nextResult === false && !sessionTestRedirects.length && vls.getLocalSessionToken() === 'new-guard-login', 'late guard redirected newer login')
  console.log(JSON.stringify({ passed: true, checks, transport: 'synthetic Axios adapters only', clients: clients.length }))
} finally { await server.close() }
