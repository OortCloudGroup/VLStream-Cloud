import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { effectScope, nextTick, reactive, ref, watch } from 'vue'
import { compileScript, parse } from 'vue/compiler-sfc'

function readSource(path) {
    return readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')
}

function loadScript(source, dependencies) {
    const script = source.replace(/^import\s+[\s\S]*?\s+from\s+['"][^'"]+['"];?\s*$/gm, '')
    return new Function('deps', `with (deps) { ${script} }`)(dependencies)
}

test('square reconnects share a watchdog and pause without racing while the tab is hidden', async () => {
    const timers = new Map()
    const intervals = new Map()
    const unmounts = []
    const page = new EventTarget()
    page.hidden = false
    let nextTimer = 0
    let starts = 0
    const source = readSource('composables/usePreviewReconnect.js')
        .replace('export function', 'function')
    const usePreviewReconnect = loadScript(`${source}\nreturn usePreviewReconnect`, {
        document: page,
        watch,
        onBeforeUnmount: callback => unmounts.push(callback),
        setTimeout: (fn, delay) => { timers.set(++nextTimer, { fn, delay }); return nextTimer },
        clearTimeout: id => timers.delete(id),
        setInterval: fn => { intervals.set(++nextTimer, fn); return nextTimer },
        clearInterval: id => intervals.delete(id),
    })
    const scope = effectScope()
    try {
        scope.run(() => {
            for (let player = 0; player < 2; player++) {
                usePreviewReconnect({ enabled: () => true, failed: () => true,
                    busy: () => false, reconnect: () => { starts++ } })
            }
        })
        assert.equal(intervals.size, 1)
        assert.equal(timers.size, 2)
        const canceled = timers.values().next().value.fn
        page.hidden = true
        page.dispatchEvent(new Event('visibilitychange'))
        assert.equal(intervals.size, 0)
        assert.equal(timers.size, 0)
        await canceled()
        assert.equal(starts, 0)

        page.hidden = false
        page.dispatchEvent(new Event('visibilitychange'))
        assert.equal(intervals.size, 1)
        assert.equal(timers.size, 2)
        const [id, { fn, delay }] = timers.entries().next().value
        assert.ok(delay >= 5000 && delay < 6000, 'cancelled work does not advance the backoff')
        timers.delete(id)
        await fn()
        assert.equal(starts, 1)
        assert.ok([...timers.values()].some(timer => timer.delay >= 10000))
    } finally {
        unmounts.forEach(unmount => unmount())
        scope.stop()
    }
    assert.equal(intervals.size, 0)
    assert.equal(timers.size, 0)
})

test('VLStream checks active preview status without reopening the same source on a catalog refresh', async () => {
    const filename = 'components/VlsDevicePlayer.vue'
    const compiled = compileScript(parse(readSource(filename)).descriptor, { id: 'preview-check' })
    const calls = []
    const unmounts = []
    const component = loadScript(compiled.content.replace('export default', 'return'), {
        watch, ref,
        onBeforeUnmount: callback => unmounts.push(callback),
        usePreviewReconnect() {},
        CameraRtcPlayer: {},
        RtcPlayer: {},
        getMqttDeviceDetail() {},
        getMqttDeviceStreams() {},
        createMqttDevicePreview() {},
        CAMERA_RTC_SOCKET_URL: '',
        parseCameraRtcConfig() {},
        squareCameraRtcConfig() {},
        getMqttSquareStatus: async id => {
            calls.push(['status', id])
            return { data: { device: { online: true } } }
        },
        getMqttSquareStreams: async id => {
            calls.push(['streams', id])
            return { data: id === 1 ? [{ id: 9 }] : [] }
        },
        createMqttSquarePreview: async id => {
            calls.push(['preview', id])
            return { data: { rtc: 'http://fixture.invalid/rtc' } }
        },
        monitorDevicePreview: () => {
            calls.push(['monitor'])
            return () => calls.push(['stop'])
        },
        location: { protocol: 'http:' },
    })
    const props = reactive({ device: { id: 1, online: true }, square: true, compactStatus: true })
    const scope = effectScope()
    const settle = async () => {
        for (let round = 0; round < 5; round++) await nextTick()
    }
    try {
        scope.run(() => component.setup(props, { expose() {} }))
        await settle()
        assert.deepEqual(calls, [['status', 1], ['streams', 1], ['preview', 1], ['monitor']])
        props.device = { id: 1, online: true }
        await settle()
        assert.equal(calls.length, 4)
        props.device = { id: 2, online: true }
        await settle()
        assert.deepEqual(calls.slice(4), [['stop'], ['status', 2], ['streams', 2]])
    } finally {
        unmounts.forEach(unmount => unmount())
        scope.stop()
    }
})

test('square ZLM failures use the parent retry path while legacy RTC keeps its retry', () => {
    const descriptor = parse(readSource('components/rtcPlayer/index.vue')).descriptor
    const handlers = new Map()
    const delays = []
    const events = []
    const debug = []
    const rtcEvent = 'offer-failed'
    const component = loadScript(descriptor.script.content.replace('export default', 'return'), {
        resolveZlmPlaybackUrl: url => url,
        console: { error() {}, log() {} },
        ZLMRTCClient: {
            Endpoint: class {
                constructor(options) { debug.push(options.debug) }
                on(name, handler) { handlers.set(name, handler) }
            },
            Events: { WEBRTC_OFFER_ANWSER_EXCHANGE_FAILED: rtcEvent },
        },
        setTimeout: (_, delay) => { delays.push(delay); return delays.length },
    })
    for (const compactStatus of [true, false]) {
        const context = { compactStatus, $refs: {}, $emit: (...event) => events.push(event) }
        context.eventcallbacK = component.methods.eventcallbacK.bind(context)
        component.methods.play.call(context, 'http://fixture.invalid/rtc')
        handlers.get(rtcEvent)({ code: -400, msg: '流不存在' })
        assert.equal(events.at(-1)[1].state, 'failed')
        assert.equal(delays.length, compactStatus ? 0 : 1)
    }
    assert.deepEqual(delays, [100])
    assert.deepEqual(debug, [false, true])
})
