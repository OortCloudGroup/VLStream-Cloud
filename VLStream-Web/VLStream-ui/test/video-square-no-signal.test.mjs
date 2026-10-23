import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { computed, createSSRApp, defineComponent, effectScope, h, nextTick, proxyRefs, reactive, ref, watch, compile } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { compileScript, parse } from 'vue/compiler-sfc'

const flush = async () => { for (let index = 0; index < 5; index++) { await Promise.resolve(); await nextTick() } }
const deferred = () => { let resolve; const promise = new Promise(value => { resolve = value }); return { promise, resolve } }
const internalFailure = 'upstream INTERNAL_DIAGNOSTIC connection failed after authentication and preview allocation'
const fakeComponent = name => defineComponent({
  name, props: { compactStatus: { type: Boolean, default: false }, description: String },
  setup: (props, { slots }) => () => h('div', { 'data-player': name, 'data-compact-status': String(props.compactStatus) }, props.description || slots.default?.()),
})
const components = Object.fromEntries(['VlsDevicePlayer', 'RtspDevicePlayer', 'SquareProtocolDevicePlayer', 'CameraRtcPlayer', 'RtcPlayer', 'Hikvision', 'ElSelect', 'ElOption', 'ElEmpty'].map(name => [name, fakeComponent(name)]))
components.ElButton = defineComponent({ setup: (_, { slots }) => () => h('button', slots.default?.()) })

function harness(path, values, dependencies = {}) {
  const descriptor = parse(readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')).descriptor
  const script = compileScript(descriptor, { id: `no-signal-${path}` }).content
    .replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm, '').replace(/import\.meta\.env/g, '__testEnv').replace('export default', 'return')
  const unmounts = [], mounts = [], events = [], routes = []
  const deps = { computed, reactive, ref, watch, nextTick, __testEnv: {}, location: { protocol: 'http:' }, ...components,
    onBeforeUnmount: action => unmounts.push(action), onMounted: action => mounts.push(action),
    useRouter: () => ({ push: route => routes.push(route) }),
    translatePhrase: value => value, translateUiMessage: value => value,
    document: { addEventListener() {}, removeEventListener() {} },
    ...dependencies,
  }
  const component = new Function('__deps', `with (__deps) { ${script} }`)(deps)
  const defaults = Object.fromEntries(Object.entries(component.props).filter(([, value]) => Object.hasOwn(value, 'default')).map(([key, value]) => [key, typeof value.default === 'function' ? value.default() : value.default]))
  const props = reactive({ ...defaults, ...values })
  const scope = effectScope()
  const instance = scope.run(() => component.setup(props, { expose() {}, emit: (...event) => events.push(event) }))
  const render = compile(descriptor.template.content)
  return { component, props, instance, events, routes, mounts,
    html: () => {
      const context = proxyRefs({ ...props, ...instance, $tp: value => value })
      const app = createSSRApp({ render: () => render(context, []) })
      for (const name of Object.keys(components)) app.component(name, deps[name])
      app.directive('loading', { getSSRProps: () => ({}) })
      return renderToString(app)
    },
    close: () => { unmounts.forEach(action => action()); scope.stop() },
  }
}

test('VLStream offline and failed preview render no signal only when the square opts in', async () => {
  const calls = []
  const deps = { getMqttDeviceDetail: async id => { calls.push(id); throw new Error(internalFailure) }, getMqttDeviceStreams() {}, createMqttDevicePreview() {}, monitorDevicePreview: () => () => {}, parseCameraRtcConfig: value => value }
  const compact = harness('components/VlsDevicePlayer.vue', { device: { id: '11', online: false }, compactStatus: true }, deps)
  await flush()
  assert.match(await compact.html(), /无信号/)
  assert.equal(calls.length, 0, 'offline guard does not initiate playback')
  assert.equal(compact.instance.error.value, '设备离线，不允许播放')
  compact.props.device = { id: '11', online: true }; await flush()
  const failedHtml = await compact.html()
  assert.match(failedHtml, /无信号/)
  assert.equal(failedHtml.includes(internalFailure), false)
  assert.equal(compact.instance.error.value, internalFailure)
  compact.close()
  const legacy = harness('components/VlsDevicePlayer.vue', { device: { id: '11', online: false } }, deps)
  await flush()
  assert.equal(legacy.props.compactStatus, false)
  assert.match(await legacy.html(), /设备离线，不允许播放/)
  legacy.close()
})

test('RTSP and ONVIF failures hide internal text while keeping protocol endpoints and retry behavior', async () => {
  const calls = []
  const player = harness('components/RtspDevicePlayer.vue', { deviceId: '12', protocol: 'ONVIF', compactStatus: true }, {
    request: async request => { calls.push(request); throw new Error(internalFailure) },
    ZLMRTCClient: { Endpoint() {}, Events: {} }, resolveZlmPlaybackUrl: value => value,
  })
  await flush()
  const html = await player.html()
  assert.match(html, /无信号/)
  assert.equal(html.includes('重新连接'), false)
  assert.equal(html.includes(internalFailure), false)
  assert.equal(player.instance.error.value, internalFailure)
  assert.equal(calls[0].url, '/onvif/device/12/preview')
  await player.instance.connect('12')
  assert.equal(calls.length, 2)
  assert.equal(calls[1].method, 'post')
  player.props.compactStatus = false
  assert.ok((await player.html()).includes(internalFailure))
  assert.match(await player.html(), /重新连接/)
  player.close()
})

test('a late RTSP lease is still released after switching sources and cannot restore the old preview', async () => {
  const first = deferred(), requests = []
  const player = harness('components/RtspDevicePlayer.vue', { deviceId: '11', protocol: 'RTSP', compactStatus: true }, {
    request: request => { requests.push(request); return request.url.endsWith('/11/preview') ? first.promise : request.method === 'delete' ? Promise.resolve() : Promise.reject(new Error(internalFailure)) },
    ZLMRTCClient: { Endpoint() {}, Events: {} }, resolveZlmPlaybackUrl: value => value,
  })
  await flush()
  player.props.deviceId = '12'; await flush()
  first.resolve({ data: { sessionId: 'old-lease', streamInfo: { rtc: 'unused' } } }); await flush()
  assert.ok(requests.some(value => value.method === 'delete' && value.url === '/rtsp/RtspDevice/11/preview/old-lease'))
  assert.equal(player.instance.error.value, internalFailure)
  assert.match(await player.html(), /无信号/)
  player.close()
})

test('custom and pushed protocol errors use compact display without changing request or cleanup', async () => {
  const calls = []
  const player = harness('views/VideoAggregation/components/SquareProtocolDevicePlayer.vue', { source: { protocol: 'CUSTOM', rowId: '17' }, compactStatus: true }, {
    request: async request => { calls.push(request); throw new Error(internalFailure) },
    getCustomDevice() {}, startPlay() {}, ZLMRTCClient: { Events: {} }, resolveZlmPlaybackUrl: value => value,
    ensureOPlayer() {}, createOPlayerOptions() {}, getCustomStreamType() {}, parseCameraRtcConfig() {},
  })
  await flush()
  assert.match(await player.html(), /无信号/)
  assert.equal((await player.html()).includes(internalFailure), false)
  assert.deepEqual(calls[0].params, { lease: true })
  assert.equal(calls[0].url, '/custom/device/17/preview')
  assert.equal(player.instance.error.value, internalFailure)
  await player.instance.connect()
  assert.equal(calls.length, 2)
  player.close()
})

test('CameraRTC compact status retains full diagnostics, retries and cleanup', async () => {
  let session
  class Session {
    constructor(options) { this.options = options; this.retries = 0; this.stops = 0; session = this }
    start() {} retryNow() { this.retries++ } stop() { this.stops++ }
  }
  const player = harness('components/CameraRtcPlayer.vue', { deviceId: 'camera-current', socketUrl: 'ws://fixture', compactStatus: true }, { CameraRtcSession: Session, parseCameraRtcTurnUrls: () => [] })
  player.instance.videoElement.value = { paused: false, muted: true, volume: 1 }
  player.mounts.forEach(action => action())
  assert.equal(session.options.nativeControlsOnAutoplayFailure, false)
  assert.match(await player.html(), /正在连接…/)
  for (const state of ['failed', 'recovering', 'retrying']) {
    session.options.onStatus({ state, message: internalFailure, retryCount: 2 })
    const html = await player.html()
    assert.match(html, /无信号/)
    assert.equal(html.includes(internalFailure), false)
    assert.equal(player.instance.status.message, internalFailure)
  }
  const diagnostic = { event: 'fixture-diagnostic', message: internalFailure }
  session.options.onDiagnostic(diagnostic)
  assert.deepEqual(player.events.at(-1), ['diagnostic', diagnostic])
  assert.equal(player.events.some(event => event[0] === 'status-change' && event[1].message === internalFailure), true)
  player.instance.retryNow(); assert.equal(session.retries, 1)
  player.props.compactStatus = false
  await nextTick()
  assert.equal(session.nativeControlsOnAutoplayFailure, true)
  assert.ok((await player.html()).includes(internalFailure))
  const originalSession = session
  player.instance.videoElement.value.controls = true
  player.props.compactStatus = true; await nextTick()
  assert.equal(session, originalSession, 'presentation changes do not recreate signaling')
  assert.equal(session.nativeControlsOnAutoplayFailure, false)
  assert.equal(player.instance.videoElement.value.controls, false)
  player.close(); assert.equal(session.stops, 1)
  const legacy = harness('components/CameraRtcPlayer.vue', { deviceId: 'camera-default', socketUrl: 'ws://fixture' }, { CameraRtcSession: Session, parseCameraRtcTurnUrls: () => [] })
  legacy.instance.videoElement.value = { paused: false, muted: true, volume: 1 }
  legacy.mounts.forEach(action => action())
  assert.equal(session.options.nativeControlsOnAutoplayFailure, true)
  legacy.close()
})

test('video square alone opts each real protocol branch into compact display and uses no signal fallback', async () => {
  const deps = { VIDEO_PROTOCOLS: [{ value: 'VLSTREAM', path: '/device', label: 'VLStream' }], videoSourceKey: source => `${source.protocol}:${source.rowId}`,
    defaultVideoSource: sources => sources?.[0], canPreviewVideoSource: source => source.status === 'ONLINE', videoSourceOnline: source => source.status === 'ONLINE', videoSourceState: source => source.status || 'UNKNOWN', customStatusDetail: () => 'UNKNOWN' }
  for (const [source, name] of [[{ protocol: 'VLSTREAM', rowId: '1', status: 'ONLINE' }, 'VlsDevicePlayer'], [{ protocol: 'ONVIF', rowId: '2', playType: '1' }, 'RtspDevicePlayer'], [{ protocol: 'CUSTOM', rowId: '3' }, 'SquareProtocolDevicePlayer']]) {
    const player = harness('views/VideoAggregation/components/VideoSquareDevicePlayer.vue', { device: { deviceName: 'controlled', sources: [source] } }, deps)
    await flush()
    assert.ok((await player.html()).includes(`data-player="${name}" data-compact-status="true"`))
    assert.equal(player.events.at(-1)[0], 'source-change')
    player.close()
  }
  const missing = harness('views/VideoAggregation/components/VideoSquareDevicePlayer.vue', { device: { sources: [] } }, deps)
  assert.match(await missing.html(), /无信号/)
  missing.close()
  const offline = harness('views/VideoAggregation/components/VideoSquareDevicePlayer.vue', { device: { sources: [{ protocol: 'VLSTREAM', rowId: '1', status: 'OFFLINE' }] } }, deps)
  assert.match(await offline.html(), /无信号/)
  assert.equal((await offline.html()).includes('来源离线'), false)
  offline.close()
})

test('the legacy RTC real callbacks emit only observed failure or playback and keep its retry path', () => {
  const descriptor = parse(readFileSync(new URL('../src/components/rtcPlayer/index.vue', import.meta.url), 'utf8')).descriptor
  const script = descriptor.script.content.replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm, '').replace('export default', 'return')
  const endpoints = [], events = [], retries = [], timers = []
  class Endpoint {
    constructor(options) { this.options = options; this.handlers = new Map(); endpoints.push(this) }
    on(name, action) { this.handlers.set(name, action) }
  }
  const types = { WEBRTC_ICE_CANDIDATE_ERROR: 'ice-error', WEBRTC_ON_REMOTE_STREAMS: 'remote-stream', WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED: 'offer-failed', WEBRTC_ON_LOCAL_STREAM: 'local-stream' }
  const component = new Function('__deps', `with (__deps) { ${script} }`)({ ZLMRTCClient: { Endpoint, Events: types }, resolveZlmPlaybackUrl: value => value, console: { log() {}, error() {} }, setTimeout: (action, delay) => { timers.push({ action, delay }); return 1 } })
  const context = { $refs: { videoElement: {} }, $emit: (...event) => events.push(event), pause: () => retries.push('pause'), play: url => retries.push(url) }
  context.eventcallbacK = component.methods.eventcallbacK.bind(context)
  component.methods.play.call(context, 'http://controlled-preview')
  assert.equal(endpoints[0].options.zlmsdpUrl, 'http://controlled-preview')
  assert.equal(endpoints[0].options.recvOnly, true)
  endpoints[0].handlers.get('local-stream')({})
  assert.deepEqual(events, [], 'ordinary waiting/local stream does not become failure')
  endpoints[0].handlers.get('ice-error')({})
  assert.deepEqual(events.at(-1), ['status-change', { state: 'failed', message: 'ICE 协商出错' }])
  endpoints[0].handlers.get('remote-stream')({ streams: [] })
  assert.deepEqual(events.at(-1), ['status-change', { state: 'playing', message: '播放成功' }])
  endpoints[0].handlers.get('offer-failed')({ code: -400, msg: '流不存在' })
  assert.equal(events.at(-1)[1].state, 'failed')
  assert.equal(timers[0].delay, 100)
  timers[0].action()
  assert.deepEqual(retries, ['pause', 'http://controlled-preview'])
})

test('VLStream receives actual RTC status events, masks only observed failure, and clears it on playing', async () => {
  let sendStatus
  const rtc = defineComponent({ emits: ['status-change'], setup: (_, { emit }) => { sendStatus = value => emit('status-change', value); return () => h('video') } })
  const player = harness('components/VlsDevicePlayer.vue', { device: { id: '11', online: true }, compactStatus: true }, {
    getMqttDeviceDetail: async () => ({ data: { device: { online: true } } }),
    getMqttDeviceStreams: async () => ({ data: [{ id: 'stream-1' }] }),
    createMqttDevicePreview: async () => ({ data: { rtc: 'http://controlled-preview' } }),
    monitorDevicePreview: () => () => {}, parseCameraRtcConfig: value => value, RtcPlayer: rtc,
  })
  await flush()
  assert.equal((await player.html()).includes('rtc-no-signal'), false)
  sendStatus({ state: 'connecting', message: 'ordinary waiting' })
  assert.equal(player.instance.rtcStatus.value, '')
  sendStatus({ state: 'failed', message: internalFailure })
  const failed = await player.html()
  assert.ok(failed.includes('rtc-no-signal'))
  assert.match(failed, /无信号/)
  assert.equal(failed.includes(internalFailure), false)
  assert.equal(player.instance.rtcUrl.value, 'http://controlled-preview')
  sendStatus({ state: 'playing', message: '播放成功' })
  assert.equal((await player.html()).includes('rtc-no-signal'), false)
  sendStatus({ state: 'failed' }); player.props.compactStatus = false
  assert.equal((await player.html()).includes('rtc-no-signal'), false)
  player.close()
})

test('actual native-control templates opt out only for compact square display', async () => {
  const hasNativeControls = html => /<video\b[^>]*\scontrols(?:[\s=>])/.test(html)
  for (const [path, values, extra] of [
    ['components/RtspDevicePlayer.vue', { deviceId: '11', protocol: 'ONVIF' }, { ZLMRTCClient: { Events: {} }, resolveZlmPlaybackUrl: value => value }],
    ['views/VideoAggregation/components/SquareProtocolDevicePlayer.vue', { source: { protocol: 'CUSTOM', rowId: '17' } }, { getCustomDevice() {}, startPlay() {}, ZLMRTCClient: { Events: {} }, resolveZlmPlaybackUrl: value => value, ensureOPlayer() {}, createOPlayerOptions() {}, getCustomStreamType() {}, parseCameraRtcConfig() {} }],
  ]) {
    const player = harness(path, { ...values, compactStatus: true }, { request: () => new Promise(() => {}), ...extra })
    assert.equal(hasNativeControls(await player.html()), false)
    player.props.compactStatus = false
    assert.equal(hasNativeControls(await player.html()), true)
    player.close()
  }
  const rtcTemplate = parse(readFileSync(new URL('../src/components/rtcPlayer/index.vue', import.meta.url), 'utf8')).descriptor.template.content
  const rtcRender = compile(rtcTemplate)
  for (const compactStatus of [undefined, false, true]) {
    const html = await renderToString(createSSRApp({ render: () => rtcRender({ compactStatus }, []) }))
    assert.equal(hasNativeControls(html), !compactStatus)
  }
  const camera = harness('components/CameraRtcPlayer.vue', { deviceId: 'controlled', socketUrl: 'ws://fixture', compactStatus: true }, { CameraRtcSession: class {}, parseCameraRtcTurnUrls: () => [] })
  camera.instance.status.state = 'playing'
  assert.equal(hasNativeControls(await camera.html()), false)
  assert.equal((await camera.html()).includes('camera-rtc-controls'), false)
  camera.props.compactStatus = false
  assert.ok((await camera.html()).includes('camera-rtc-controls'))
  camera.close()
  const vls = harness('components/VlsDevicePlayer.vue', { device: { id: '11', online: true }, compactStatus: true }, {
    getMqttDeviceDetail: async () => ({ data: { device: { online: true } } }), getMqttDeviceStreams: async () => ({ data: [{ id: 'stream-1' }] }),
    createMqttDevicePreview: async () => ({ data: { rtc: 'http://controlled-preview' } }), monitorDevicePreview: () => () => {}, parseCameraRtcConfig: value => value,
  })
  await flush()
  assert.ok((await vls.html()).includes('data-player="RtcPlayer" data-compact-status="true"'))
  vls.close()
})

test('compact failed canvases contain only no signal, with no management or reconnect controls', async () => {
  const protocolDeps = { VIDEO_PROTOCOLS: [{ value: 'VLSTREAM', path: '/device', label: 'VLStream' }], videoSourceKey: source => `${source.protocol}:${source.rowId}`,
    defaultVideoSource: sources => sources?.[0], canPreviewVideoSource: () => false, videoSourceOnline: () => false, videoSourceState: () => 'OFFLINE', customStatusDetail: () => 'UNKNOWN' }
  const fallback = harness('views/VideoAggregation/components/VideoSquareDevicePlayer.vue', { device: { sources: [{ protocol: 'VLSTREAM', rowId: '1' }] } }, protocolDeps)
  const fallbackHtml = await fallback.html()
  assert.match(fallbackHtml, /无信号/)
  assert.equal(fallbackHtml.includes('前往设备页'), false)
  assert.equal(fallbackHtml.includes('<button'), false)
  fallback.close()
  for (const [path, values, extra] of [
    ['components/RtspDevicePlayer.vue', { deviceId: '11' }, { ZLMRTCClient: { Events: {} }, resolveZlmPlaybackUrl: value => value }],
    ['views/VideoAggregation/components/SquareProtocolDevicePlayer.vue', { source: { protocol: 'CUSTOM', rowId: '17' } }, { getCustomDevice() {}, startPlay() {}, ZLMRTCClient: { Events: {} }, resolveZlmPlaybackUrl: value => value, ensureOPlayer() {}, createOPlayerOptions() {}, getCustomStreamType() {}, parseCameraRtcConfig() {} }],
  ]) {
    const player = harness(path, { ...values, compactStatus: true }, { request: async () => { throw new Error(internalFailure) }, ...extra })
    await flush()
    const html = await player.html()
    assert.match(html, /无信号/)
    assert.equal(html.includes('前往设备页'), false)
    assert.equal(html.includes('重新连接'), false)
    assert.equal(html.includes('<button'), false)
    player.props.compactStatus = false
    assert.match(await player.html(), /重新连接/)
    player.close()
  }
  const camera = harness('components/CameraRtcPlayer.vue', { deviceId: 'controlled', socketUrl: 'ws://fixture', compactStatus: true }, { CameraRtcSession: class {}, parseCameraRtcTurnUrls: () => [] })
  for (const state of ['failed', 'retrying', 'recovering']) {
    camera.instance.status.state = state
    const html = await camera.html()
    assert.match(html, /无信号/)
    assert.equal(html.includes('重新连接'), false)
    assert.equal(html.includes('camera-rtc-spinner'), false)
    assert.equal(html.includes('<button'), false)
  }
  camera.instance.status.state = 'failed'; camera.props.compactStatus = false
  assert.match(await camera.html(), /重新连接/)
  camera.close()
})
