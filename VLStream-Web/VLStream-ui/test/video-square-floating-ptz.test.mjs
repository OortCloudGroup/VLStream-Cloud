import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createSSRApp, effectScope, h, nextTick as flushTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as Vue from 'vue'
import { renderToString } from '@vue/server-renderer'
import { compileScript, compileTemplate, parse } from 'vue/compiler-sfc'
import { clampFloatingPanelPosition, createFloatingPanelDrag } from '../src/utils/videoSquareFloatingPanel.mjs'

function eventTarget() {
    const listeners = new Map()
    return {
        addEventListener(type, fn) { if (!listeners.has(type)) listeners.set(type, new Set()); listeners.get(type).add(fn) },
        removeEventListener(type, fn) { listeners.get(type)?.delete(fn) },
        fire(type, event = {}) { for (const listener of [...(listeners.get(type) || [])]) listener(event) },
        count: () => [...listeners.values()].reduce((sum, values) => sum + values.size, 0),
    }
}
function dragHarness() {
    let position = { x: 400, y: 20 }
    const bounds = { width: 900, height: 600, panelWidth: 300, panelHeight: 400 }
    const target = eventTarget(), captures = new Set(), changes = []
    const handle = { setPointerCapture: id => captures.add(id), hasPointerCapture: id => captures.has(id), releasePointerCapture: id => captures.delete(id) }
    const drag = createFloatingPanelDrag({ readBounds: () => bounds, readPosition: () => position,
        writePosition: value => { position = value }, eventTarget: target, onDragChange: value => changes.push(value) })
    const pointer = overrides => ({ pointerId: 1, button: 0, isPrimary: true, clientX: 420, clientY: 40, currentTarget: handle, preventDefault() {}, ...overrides })
    return { drag, bounds, target, captures, changes, pointer, position: () => position }
}

test('floating positions stay inside desktop, narrow, short and oversized bounds', () => {
    const bounds = { width: 900, height: 600, panelWidth: 300, panelHeight: 400 }
    assert.deepEqual(clampFloatingPanelPosition({ x: -100, y: 500 }, bounds), { x: 8, y: 192 })
    assert.deepEqual(clampFloatingPanelPosition({ x: 999, y: -1 }, { width: 220, height: 100, panelWidth: 204, panelHeight: 84 }), { x: 8, y: 8 })
    assert.deepEqual(clampFloatingPanelPosition({ x: 999, y: 999 }, { width: 100, height: 60, panelWidth: 300, panelHeight: 400 }), { x: 0, y: 0 })
    assert.deepEqual(clampFloatingPanelPosition({ x: NaN, y: Infinity }, { width: 310, height: 410, panelWidth: 300, panelHeight: 400 }), { x: 5, y: 5 })
})

test('only one primary handle pointer drags, clips at edges and releases capture/listeners', () => {
    const t = dragHarness()
    assert.equal(t.drag.start(t.pointer({ button: 2 })), false)
    assert.equal(t.drag.start(t.pointer({ isPrimary: false })), false)
    assert.equal(t.target.count(), 0)
    assert.equal(t.drag.start(t.pointer()), true)
    assert.equal(t.target.count(), 4)
    assert.ok(t.captures.has(1))
    assert.equal(t.drag.start(t.pointer({ pointerId: 2 })), false)
    t.target.fire('pointermove', t.pointer({ pointerId: 2, clientX: 9999 }))
    assert.deepEqual(t.position(), { x: 400, y: 20 })
    t.target.fire('pointermove', t.pointer({ clientX: 9999, clientY: -500 }))
    assert.deepEqual(t.position(), { x: 592, y: 8 })
    t.target.fire('pointerup', t.pointer({ pointerId: 2 }))
    assert.equal(t.drag.isDragging(), true)
    t.target.fire('pointerup', t.pointer())
    assert.equal(t.drag.isDragging(), false)
    assert.equal(t.target.count(), 0)
    assert.equal(t.captures.size, 0)
    assert.deepEqual(t.changes, [true, false])
})

test('resize, cancel, blur, keyboard and destruction never leave active listeners', () => {
    const t = dragHarness()
    t.drag.start(t.pointer())
    t.bounds.width = 360; t.bounds.height = 440
    t.drag.fit()
    assert.deepEqual(t.position(), { x: 52, y: 20 })
    t.target.fire('pointercancel', t.pointer())
    assert.equal(t.target.count(), 0)
    t.drag.start(t.pointer())
    t.target.fire('blur')
    assert.equal(t.target.count(), 0)
    t.drag.moveByKey({ key: 'ArrowLeft', preventDefault() {}, stopPropagation() {} })
    assert.deepEqual(t.position(), { x: 42, y: 20 })
    t.drag.moveByKey({ key: 'ArrowUp', shiftKey: true, preventDefault() {}, stopPropagation() {} })
    assert.deepEqual(t.position(), { x: 42, y: 8 })
    t.drag.start(t.pointer())
    t.drag.destroy()
    assert.equal(t.target.count(), 0)
    assert.equal(t.captures.size, 0)
    assert.equal(t.drag.start(t.pointer()), false)
})

const source = readFileSync(new URL('../src/views/VideoAggregation/components/VideoSquareFloatingPtz.vue', import.meta.url), 'utf8')
const descriptor = parse(source).descriptor
const compiled = compileScript(descriptor, { id: 'floating-ptz-test' })
const executable = compiled.content.replace(/^import\s+.*?from\s+['"].*?['"];?\s*$/gm, '').replace('export default', 'return')
const component = new Function('__deps', `const {nextTick,onBeforeUnmount,onMounted,ref,watch,VideoSquarePtzControls,ptzCloseIcon='test-close.svg',clampFloatingPanelPosition,createFloatingPanelDrag,window,ResizeObserver}=__deps;${executable}`)

function wrapperRender() {
    const template = compileTemplate({ source: descriptor.template.content, filename: 'VideoSquareFloatingPtz.vue', id: 'floating-ptz-test', compilerOptions: { bindingMetadata: compiled.bindings } })
    assert.deepEqual(template.errors, [])
    const renderSource = template.code.replace(/import\s*\{([^}]+)\}\s*from\s*['"]vue['"]/g, (_, names) => `const {${names.replace(/\bas\b/g, ':')}}=__vue`).replace('export function render', 'return function render')
    return new Function('__vue', renderSource)(Vue)
}

function componentHarness(visible = true) {
    const mounted = [], unmounts = [], observed = [], observers = [], emitted = [], target = eventTarget()
    const props = reactive({ visible, source: { protocol: 'ONVIF', rowId: 11 }, viewOnly: true })
    const bounds = { width: 900, height: 600, panelWidth: 300, panelHeight: 400 }
    class Observer {
        constructor(fn) { this.callback = fn; this.disconnected = false; observers.push(this) }
        observe(value) { observed.push(value) }
        disconnect() { this.disconnected = true }
    }
    const scope = effectScope()
    const instance = scope.run(() => component({ ...Vue, ref, watch, VideoSquarePtzControls: {}, clampFloatingPanelPosition, createFloatingPanelDrag,
        onMounted: fn => mounted.push(fn), onBeforeUnmount: fn => unmounts.push(fn), window: target, ResizeObserver: Observer,
    }).setup(props, { expose() {}, emit: (...args) => emitted.push(args) }))
    const host = { get clientWidth() { return bounds.width }, get clientHeight() { return bounds.height } }
    instance.panel.value = { parentElement: host, get offsetWidth() { return props.visible ? bounds.panelWidth : 0 }, get offsetHeight() { return props.visible ? bounds.panelHeight : 0 } }
    mounted.forEach(fn => fn())
    return { props, bounds, instance, target, observers, observed, emitted, close() { unmounts.forEach(fn => fn()); scope.stop() } }
}

test('actual wrapper setup places right, clamps observed parent resize, and cancels drag on hide/unmount', async () => {
    const t = componentHarness()
    assert.deepEqual(t.instance.position.value, { x: 592, y: 8 })
    assert.equal(t.observed.length, 2)
    const capture = new Set()
    const event = { pointerId: 1, button: 0, isPrimary: true, clientX: 0, clientY: 0, currentTarget: {
        setPointerCapture: id => capture.add(id), hasPointerCapture: id => capture.has(id), releasePointerCapture: id => capture.delete(id),
    }, preventDefault() {} }
    t.instance.startDrag(event)
    assert.equal(t.instance.dragging.value, true)
    assert.equal(t.target.count(), 5)
    t.props.visible = false; await flushTick()
    assert.equal(t.instance.dragging.value, false)
    assert.equal(t.target.count(), 1)
    assert.equal(capture.size, 0)
    t.props.visible = true; await flushTick(); await flushTick()
    t.bounds.width = 390; t.bounds.height = 450; t.observers[0].callback()
    assert.deepEqual(t.instance.position.value, { x: 82, y: 8 })
    t.instance.startDrag(event)
    t.instance.close()
    assert.deepEqual(t.emitted, [['close']])
    assert.equal(t.instance.dragging.value, false)
    t.instance.startDrag(event)
    t.close()
    assert.equal(t.target.count(), 0)
    assert.equal(t.observers[0].disconnected, true)
    assert.equal(capture.size, 0)
})

test('initial hidden wrapper waits for measurable bounds before first right-side placement', async () => {
    const t = componentHarness(false)
    assert.deepEqual(t.instance.position.value, { x: 0, y: 0 })
    t.props.visible = true; await flushTick(); await flushTick()
    assert.deepEqual(t.instance.position.value, { x: 592, y: 8 })
    t.close()
})

test('actual X template emits close, cancels dragging and isolates its pointer press from the title', () => {
    const t = componentHarness()
    const tree = wrapperRender()({$tp:value=>value}, [], t.props, Vue.proxyRefs(t.instance))
    const closeButton = tree.children[0].children[1]
    let stopped = 0
    closeButton.props.onPointerdown({stopPropagation:()=>{stopped++}})
    assert.equal(t.instance.dragging.value, false)
    t.instance.startDrag({pointerId:1,button:0,isPrimary:true,clientX:20,clientY:20,currentTarget:{},preventDefault(){}})
    closeButton.props.onClick({stopPropagation:()=>{stopped++}})
    assert.equal(stopped, 2)
    assert.equal(t.instance.dragging.value, false)
    assert.deepEqual(t.emitted, [['close']])
    t.close()
})

test('actual wrapper template forwards the exact source, visibility and read-only props without PTZ commands', async () => {
    const received = []
    const controls = { props: ['source', 'visible', 'viewOnly'], setup(props) { received.push(props); return () => h('div', 'existing PTZ') } }
    const options = component({ ...Vue, ref, watch, onBeforeUnmount, onMounted, VideoSquarePtzControls: controls,
        clampFloatingPanelPosition, createFloatingPanelDrag, window: {}, ResizeObserver: undefined })
    options.render = wrapperRender()
    const device = { protocol: 'ONVIF', rowId: 11 }
    const app = createSSRApp(options, { source: device, visible: true, viewOnly: true })
    app.config.globalProperties.$tp = value => value
    const html = await renderToString(app)
    assert.equal(received.length, 1)
    assert.equal(received[0].source.rowId, device.rowId)
    assert.equal(received[0].visible, true)
    assert.equal(received[0].viewOnly, true)
    assert.match(html, /existing PTZ/)
    assert.match(html, /拖动PTZ控制器/)
})
