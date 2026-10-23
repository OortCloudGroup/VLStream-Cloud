import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { computed, effectScope, nextTick, reactive, ref, watch } from 'vue'
import { parse, compileScript } from 'vue/compiler-sfc'

const utility = async name => import(`data:text/javascript;base64,${Buffer.from(readFileSync(new URL(`../src/utils/${name}.js`, import.meta.url))).toString('base64')}`)
const { createPtzCapabilitiesController, canUsePtzMovement } = await utility('onvifPtzCapabilities')
const { createPtzHoldController } = await utility('onvifPtzHold')
const source = readFileSync(new URL('../src/views/VideoAggregation/components/VideoSquarePtzControls.vue', import.meta.url), 'utf8')
const compiled = compileScript(parse(source).descriptor, { id: 'square-ptz-test' }).content
const executable = compiled.replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm, '').replace('export default', 'return')
const component = new Function('__deps', `const {computed,ref,watch,onMounted,onBeforeUnmount,useRouter,PTZControl,getPtzCapabilities,onvifPZTStart,onvifPZTEnd,createPtzCapabilitiesController,canUsePtzMovement,createPtzHoldController,checkPermi,VIDEO_PROTOCOLS,window,document}=__deps;${executable}`)
const supported = { data: { status: 'supported', continuousPanTilt: true } }
const defer = () => { let resolve; const promise = new Promise(r => { resolve = r }); return { promise, resolve } }

function harness(options = {}) {
  const requests = [], starts = [], stops = [], unmounts = [], routes = []
  const props = reactive({ source: { protocol: 'ONVIF', rowId: 11 }, visible: true, viewOnly: false })
  const scope = effectScope()
  const instance = scope.run(() => component({ computed, ref, watch, onMounted() {}, onBeforeUnmount: fn => unmounts.push(fn),
    useRouter: () => ({ push: value => routes.push(value) }), PTZControl: {}, VIDEO_PROTOCOLS: [{ value: 'ONVIF', path: '/onvif' }],
    checkPermi: () => options.permitted !== false, createPtzCapabilitiesController, canUsePtzMovement,
    createPtzHoldController: settings => createPtzHoldController({ ...settings, setTimer: () => 1, clearTimer() {} }),
    getPtzCapabilities: params => { requests.push(params); return options.capability ? options.capability(params) : Promise.resolve(supported) },
    onvifPZTStart: params => { starts.push(params); return options.start ? options.start(params) : Promise.resolve() },
    onvifPZTEnd: params => { stops.push(params); return Promise.resolve() },
    window: { removeEventListener() {} }, document: { hidden: false, removeEventListener() {} },
  }).setup(props, { expose() {} }))
  return { props, instance, requests, starts, stops, routes,
    close: () => { unmounts.forEach(fn => fn()); scope.stop() } }
}

test('selected-source capability gates movement, and release stops the same actual source', async () => {
  const h = harness()
  await nextTick()
  assert.equal(h.instance.canMove.value, true)
  await h.instance.move('up')
  await h.instance.hold.settled()
  assert.deepEqual(h.starts, [{ id: 11, direction: 'upper', controSpeed: .1 }])
  await h.instance.stop()
  assert.deepEqual(h.stops, [{ id: 11 }])
  h.close()
})

test('source change rejects stale capability and stops late movement before another source can move', async () => {
  const pending = defer(), start = defer()
  const h = harness({ capability: params => params.id === 12 ? pending.promise : Promise.resolve(supported), start: () => start.promise })
  await nextTick()
  h.instance.move('left')
  await Promise.resolve()
  h.props.source = { protocol: 'ONVIF', rowId: 12 }
  await nextTick()
  h.instance.move('right')
  assert.equal(h.starts.length, 1)
  h.props.source = { protocol: 'VLSTREAM', rowId: 11 }
  await nextTick()
  pending.resolve(supported)
  start.resolve()
  await h.instance.hold.settled()
  await nextTick()
  assert.equal(h.instance.canMove.value, false)
  assert.deepEqual(h.stops, [{ id: 11 }])
  h.close()
})

test('view-only or missing control permission cannot move, and visibility loss stops an active hold', async () => {
  const denied = harness({ permitted: false })
  await nextTick()
  denied.instance.move('up')
  assert.equal(denied.starts.length, 0)
  denied.close()
  const h = harness()
  await nextTick()
  await h.instance.move('left')
  await h.instance.hold.settled()
  h.props.visible = false
  await nextTick()
  await h.instance.hold.settled()
  assert.deepEqual(h.stops, [{ id: 11 }])
  h.props.viewOnly = true
  h.props.visible = true
  await nextTick()
  h.instance.move('up')
  h.instance.openManagement('camera-management')
  assert.equal(h.starts.length, 1)
  assert.equal(h.routes.length, 0)
  h.close()
})
