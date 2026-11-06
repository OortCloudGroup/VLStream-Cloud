import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { computed, effectScope, nextTick, reactive, ref, watch } from 'vue'
import { compileScript, parse } from 'vue/compiler-sfc'
import { createPreviewStartQueue } from '../src/utils/previewStartQueue.mjs'
import { createPreviewSessionRegistry } from '../src/utils/previewSessionRegistry.mjs'

const wait = ms => new Promise(resolve => setTimeout(resolve, ms))
const flush = async () => { for (let index = 0; index < 5; index++) { await Promise.resolve(); await nextTick() } }

function load(path, values, dependencies) {
  const descriptor = parse(readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')).descriptor
  const script = compileScript(descriptor, { id: `pressure-${path}` }).content
    .replace(/^import\s+[\s\S]*?\s+from\s+['"][^'"]+['"];?\s*$/gm, '').replace(/import\.meta\.env/g, '({})').replace('export default', 'return')
  const unmounts = [], mounts = []
  const deps = { computed, reactive, ref, watch, nextTick, location: { protocol: 'http:' }, translatePhrase: value => value,
    onBeforeUnmount: action => unmounts.push(action), onMounted: action => mounts.push(action), usePreviewReconnect() {}, ...dependencies }
  const component = new Function('__deps', `with (__deps) { ${script} }`)(deps)
  const defaults = Object.fromEntries(Object.entries(component.props).filter(([, value]) => Object.hasOwn(value, 'default'))
    .map(([key, value]) => [key, typeof value.default === 'function' ? value.default() : value.default]))
  const props = reactive({ ...defaults, ...values })
  const scope = effectScope()
  const instance = scope.run(() => component.setup(props, { expose() {}, emit() {} }))
  return { props, instance, mounts, close: () => { unmounts.forEach(action => action()); scope.stop() } }
}

test('start queue caps concurrent starts and spaces them out', async () => {
  const queue = createPreviewStartQueue({ limit: 2, spacingMs: 30 })
  const startedAt = [], gates = []
  let running = 0, peak = 0
  const jobs = [0, 1, 2, 3, 4].map(index => queue.schedule(async () => {
    startedAt.push(Date.now()); running++; peak = Math.max(peak, running)
    await new Promise(resolve => gates.push(resolve)); running--; return index
  }))
  await wait(120)
  assert.equal(peak, 2, 'never more than the limit in flight')
  assert.equal(queue.pending, 3)
  while (gates.length) gates.shift()()
  await wait(5)
  while (gates.length || queue.pending || queue.active) { while (gates.length) gates.shift()(); await wait(10) }
  assert.deepEqual(await Promise.all(jobs), [0, 1, 2, 3, 4], 'results keep their own task')
  for (let index = 1; index < startedAt.length; index++) assert.ok(startedAt[index] - startedAt[index - 1] >= 25, `start ${index} was spaced`)
})

test('start queue skips cancelled work and still reports task errors', async () => {
  const queue = createPreviewStartQueue({ limit: 1, spacingMs: 5 })
  let release; const ran = []
  const first = queue.schedule(() => new Promise(resolve => { release = () => resolve('first') }))
  let cancelled = false
  const second = queue.schedule(async () => { ran.push('second'); return 'second' }, () => cancelled)
  const third = queue.schedule(async () => { throw new Error('boom') })
  cancelled = true
  await wait(10); release()
  assert.equal(await first, 'first')
  assert.equal(await second, undefined)
  assert.deepEqual(ran, [])
  await assert.rejects(third, /boom/)
  assert.equal(queue.active, 0)
  assert.equal(await queue.schedule(async () => 'after'), 'after', 'a failure does not block the queue')
})

test('session registry releases every live lease once and ignores released ones', () => {
  const released = []
  const registry = createPreviewSessionRegistry(session => { released.push(session.id); if (session.id === 'bad') throw new Error('network') })
  const a = { id: 'a' }, b = { id: 'b' }, bad = { id: 'bad' }
  registry.register(a); const forgetB = registry.register(b); registry.register(bad)
  forgetB()
  registry.releaseAll()
  assert.deepEqual(released.sort(), ['a', 'bad'], 'a lease released normally is not sent again; a failing one does not stop the rest')
  assert.equal(registry.size, 0)
  registry.releaseAll()
  assert.equal(released.length, 2)
})

test('RTSP thumbnails start through the queue, stay silent on failure and track their lease', async () => {
  const requests = [], scheduled = [], registered = [], unregistered = []
  const player = load('components/RtspDevicePlayer.vue', { deviceId: '11', protocol: 'ONVIF', compactStatus: true }, {
    request: async config => { requests.push(config); if (config.method === 'post') return { data: { sessionId: 'lease-1', streamInfo: { rtc: 'rtc://x' } } }; return {} },
    ZLMRTCClient: { Endpoint: class { on() {} close() {} }, Events: {} }, resolveZlmPlaybackUrl: value => value,
    schedulePreviewStart: (task, cancelled) => { scheduled.push(cancelled); return task() },
    registerPreviewSession: session => { registered.push(session); return () => unregistered.push(session.id) },
  })
  await flush()
  assert.equal(scheduled.length, 1, 'thumbnail start goes through the queue')
  assert.equal(requests[0].silentError, true, 'compact failures do not raise a toast')
  assert.deepEqual(registered, [{ deviceId: '11', base: '/onvif/device', id: 'lease-1' }])
  player.close(); await flush()
  assert.deepEqual(unregistered, ['lease-1'])
  const release = requests.find(config => config.method === 'delete')
  assert.equal(release.url, '/onvif/device/11/preview/lease-1')
  assert.equal(release.silentError, true)
})

test('the main window preview is not queued and keeps its error toast', async () => {
  const requests = [], scheduled = []
  const player = load('components/RtspDevicePlayer.vue', { deviceId: '11', protocol: 'ONVIF', compactStatus: false }, {
    request: async config => { requests.push(config); throw new Error('camera busy') },
    ZLMRTCClient: { Endpoint: class { on() {} close() {} }, Events: {} }, resolveZlmPlaybackUrl: value => value,
    schedulePreviewStart: task => { scheduled.push(1); return task() }, registerPreviewSession: () => () => {},
  })
  await flush()
  assert.equal(scheduled.length, 0)
  assert.equal(requests[0].silentError, false)
  player.close()
})

test('a thumbnail replaced while waiting in the queue never opens its camera session', async () => {
  const requests = []; let cancelled
  const player = load('components/RtspDevicePlayer.vue', { deviceId: '11', protocol: 'ONVIF', compactStatus: true }, {
    request: async config => { requests.push(config); return {} },
    ZLMRTCClient: { Endpoint: class { on() {} close() {} }, Events: {} }, resolveZlmPlaybackUrl: value => value,
    schedulePreviewStart: (task, isCancelled) => { cancelled = isCancelled; return Promise.resolve(undefined) },
    registerPreviewSession: () => () => {},
  })
  await flush()
  assert.deepEqual(requests.filter(config => config.method === 'post'), [])
  assert.equal(typeof cancelled, 'function')
  player.close()
})

test('custom thumbnails queue their start and register the lease for page unload', async () => {
  const requests = [], scheduled = [], registered = []
  const player = load('views/VideoAggregation/components/SquareProtocolDevicePlayer.vue', { source: { protocol: 'CUSTOM', rowId: '17' }, compactStatus: true }, {
    request: async config => { requests.push(config); return config.method === 'post' ? { data: { sessionId: 'c-1', playMode: 'DIRECT', url: 'rtsp://host/stream' } } : {} },
    getCustomDevice() {}, startPlay() {}, ZLMRTCClient: { Endpoint: class { on() {} close() {} }, Events: {} }, resolveZlmPlaybackUrl: value => value,
    ensureOPlayer() {}, createOPlayerOptions() {}, getCustomStreamType: () => 'rtsp', parseCameraRtcConfig: value => value,
    Hikvision: {}, CameraRtcPlayer: {},
    schedulePreviewStart: task => { scheduled.push(1); return task() },
    registerPreviewSession: session => { registered.push(session); return () => {} },
    document: { addEventListener() {}, removeEventListener() {} },
  })
  await flush()
  assert.equal(scheduled.length, 1)
  assert.deepEqual(registered, [{ base: '/custom/device', deviceId: '17', id: 'c-1' }])
  player.close()
})

test('thumbnail strip waits for status to settle and holds no previews while the tab is hidden', async () => {
  const listeners = {}
  const documentStub = { hidden: false, addEventListener: (name, handler) => { listeners[name] = handler }, removeEventListener() {} }
  globalThis.ResizeObserver = class { observe() {} disconnect() {} }
  const strip = load('views/VideoAggregation/components/VideoSquareThumbnailStrip.vue',
    { devices: [{ id: 'a', status: 'UNKNOWN' }, { id: 'b', status: 'ONLINE' }], selectedId: 'a', autoPreview: true },
    { useHoverScroll: () => ({ start() {}, stop() {} }), VideoSquareDevicePlayer: {}, document: documentStub })
  strip.mounts.forEach(action => action())
  const live = () => [...strip.instance.livePreviewIds.value].sort()
  assert.deepEqual(live(), [], 'nothing mounts before the catalogue settles')
  strip.props.devices = [{ id: 'a', status: 'ONLINE' }, { id: 'b', status: 'OFFLINE' }]; await flush()
  strip.props.devices = [{ id: 'a', status: 'ONLINE' }, { id: 'b', status: 'ONLINE' }]; await flush()
  await wait(450)
  assert.deepEqual(live(), ['b'], 'only the final state mounts, and the selected device keeps its own main player')
  strip.props.selectedId = 'b'; await flush(); await wait(450)
  assert.deepEqual(live(), ['a'])
  documentStub.hidden = true; listeners.visibilitychange(); await flush(); await wait(450)
  assert.deepEqual(live(), [], 'a hidden tab releases its thumbnail sessions')
  documentStub.hidden = false; listeners.visibilitychange(); await flush(); await wait(450)
  assert.deepEqual(live(), ['a'])
  strip.close()
  delete globalThis.ResizeObserver
})
