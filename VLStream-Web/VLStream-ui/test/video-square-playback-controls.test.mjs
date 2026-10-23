import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { compile, createSSRApp, proxyRefs, reactive } from 'vue'
import { compileScript, parse } from 'vue/compiler-sfc'
import { renderToString } from '@vue/server-renderer'
import { canCaptureVideoFrame, captureVideoFrame, downloadVideoFrame } from '../src/utils/videoSquareCapture.mjs'

const descriptor = parse(readFileSync(new URL('../src/views/VideoAggregation/components/VideoSquarePlaybackControls.vue', import.meta.url), 'utf8')).descriptor
const script = compileScript(descriptor, { id: 'playback-toolbar-test' }).content.replace('export default', 'return')
const component = new Function(script)()
const render = compile(descriptor.template.content)

function toolbar(values = {}) {
  const props = reactive({ ptzOpen: false, muted: true, canMute: false, canScreenshot: false, canClose: false, fullscreen: false, busy: false, ...values })
  const events = []
  const setup = component.setup(props, { expose() {}, emit: (...args) => events.push(args) })
  const context = proxyRefs({ ...setup, $tp: value => value })
  Object.defineProperties(context, Object.fromEntries(Object.keys(props).map(name => [name, { get: () => props[name], configurable: true }])))
  const vnode = () => render(context, [])
  const buttons = () => vnode().children.filter(child => child.type === 'button')
  return { props, events, setup, buttons, html: () => renderToString(createSSRApp({ render: vnode })) }
}

test('actual toolbar renders all seven design controls in order and leaves unknown capabilities static', async () => {
  const h = toolbar()
  const buttons = h.buttons()
  assert.deepEqual(buttons.map(button => button.props['data-action']), ['microphone', 'speaker', 'screenshot', 'ptz', 'ai-hosting', 'fullscreen', 'close'])
  for (const index of [0, 4]) {
    assert.notEqual(buttons[index].props.disabled, undefined)
    assert.notEqual(buttons[index].props.disabled, false)
    assert.equal(buttons[index].props.onClick, undefined)
  }
  h.setup.dispatch('microphone'); h.setup.dispatch('ai-hosting')
  assert.deepEqual(h.events, [])
  const html = await h.html()
  assert.equal((html.match(/<button\b/g) || []).length, 7)
  assert.match(html, /aria-label="截屏"/)
  assert.match(html, /aria-label="AI托管"/)
  assert.match(html, /aria-label="关闭当前视频"/)
  assert.equal(html.includes('录制'), false, 'the AI-hosting icon is not misrepresented as recording')
})

test('real template handlers emit only local preview actions and obey availability/busy state', () => {
  const h = toolbar({ canMute: true, canScreenshot: true, canClose: true })
  const actionable = [1, 2, 3, 5, 6]
  for (const index of actionable) h.buttons()[index].props.onClick()
  assert.deepEqual(h.events, [['toggle-mute'], ['screenshot'], ['toggle-ptz'], ['toggle-fullscreen'], ['close-video']])
  h.events.length = 0
  h.props.busy = true
  for (const index of actionable) {
    const button = h.buttons()[index]
    assert.equal(button.props.disabled, true)
    button.props.onClick()
  }
  assert.deepEqual(h.events, [])
  h.props.busy = false; h.props.canMute = false; h.props.canScreenshot = false; h.props.canClose = false
  for (const index of [1, 2, 6]) h.buttons()[index].props.onClick()
  assert.deepEqual(h.events, [], 'programmatic invocations cannot bypass disabled capability guards')
})

test('toolbar reflects current PTZ, audio and fullscreen state without initiating device control', async () => {
  const h = toolbar({ ptzOpen: true, muted: false, fullscreen: true })
  const buttons = h.buttons()
  assert.equal(buttons[1].props['aria-pressed'], true)
  assert.equal(buttons[3].props['aria-pressed'], true)
  assert.ok(buttons[3].props.class.includes('is-active'))
  assert.equal(buttons[5].props['aria-label'], '退出全屏')
  assert.match(await h.html(), /aria-label="PTZ" aria-pressed="true"/)
  assert.deepEqual(h.events, [])
})

const video = { readyState: 2, videoWidth: 1280, videoHeight: 720 }
const imageBlob = () => new Blob([Uint8Array.from([137, 80, 78, 71, 13, 10, 26, 10])], { type: 'image/png' })
function frameHarness(options = {}) {
  const draws = [], downloads = []
  const blob = imageBlob()
  const canvas = { getContext: () => ({ drawImage: (...args) => draws.push(args) }), toBlob: (callback, type) => { assert.equal(type, 'image/png'); callback(blob) }, ...options.canvas }
  const settings = { createCanvas: () => canvas, download: async (...args) => downloads.push(args), ...options.settings }
  return { canvas, blob, settings, draws, downloads }
}

test('capture refuses unavailable frames before creating a canvas or triggering a download', async () => {
  let created = 0, downloaded = 0
  for (const source of [null, { ...video, readyState: 1 }, { ...video, videoWidth: 0 }, { ...video, videoHeight: -1 }, { ...video, videoWidth: Infinity }]) {
    assert.equal(canCaptureVideoFrame(source), false)
    await assert.rejects(captureVideoFrame(source, { createCanvas: () => { created++ }, download: () => { downloaded++ } }), /没有可截取的画面/)
  }
  assert.equal(created, 0); assert.equal(downloaded, 0)
})

test('capture draws the actual selected video dimensions and downloads its PNG before resolving', async () => {
  const h = frameHarness()
  const result = await captureVideoFrame(video, { ...h.settings, fileName: '现场/摄像机:1.png' })
  assert.equal(h.canvas.width, 1280); assert.equal(h.canvas.height, 720)
  assert.deepEqual(h.draws, [[video, 0, 0, 1280, 720]])
  assert.deepEqual(h.downloads, [[h.blob, '现场_摄像机_1.png']])
  assert.equal(result.blob, h.blob)
  assert.deepEqual({ width: result.width, height: result.height, fileName: result.fileName }, { width: 1280, height: 720, fileName: '现场_摄像机_1.png' })
})

test('cross-origin, encoding, empty PNG and download failures reject without fabricated success', async () => {
  const tainted = new DOMException('The canvas is tainted', 'SecurityError')
  const security = frameHarness({ canvas: { getContext: () => ({ drawImage() { throw tainted } }) } })
  await assert.rejects(captureVideoFrame(video, security.settings), error => error === tainted)
  assert.deepEqual(security.downloads, [])
  const encoding = new Error('encoding failed')
  const failed = frameHarness({ canvas: { toBlob() { throw encoding } } })
  await assert.rejects(captureVideoFrame(video, failed.settings), error => error === encoding)
  assert.deepEqual(failed.downloads, [])
  for (const blob of [null, new Blob([], { type: 'image/png' }), new Blob(['not png'], { type: 'text/plain' })]) {
    const h = frameHarness({ canvas: { toBlob: callback => callback(blob) } })
    await assert.rejects(captureVideoFrame(video, h.settings), /无法生成视频截屏/)
    assert.deepEqual(h.downloads, [])
  }
  const blocked = new Error('download blocked')
  const download = frameHarness({ settings: { download: async () => { throw blocked } } })
  await assert.rejects(captureVideoFrame(video, download.settings), error => error === blocked)
})

test('default download removes its temporary anchor and object URL on success and failure', () => {
  for (const shouldFail of [false, true]) {
    const calls = []
    const failure = new Error('browser refused download')
    const link = { style: {}, click() { calls.push('click'); if (shouldFail) throw failure }, remove() { calls.push('remove') } }
    const dom = { createElement: name => { assert.equal(name, 'a'); return link }, body: { appendChild: value => { assert.equal(value, link); calls.push('append') } } }
    const blob = imageBlob()
    const urlApi = { createObjectURL: value => { assert.equal(value, blob); calls.push('create'); return 'blob:controlled' }, revokeObjectURL: value => { assert.equal(value, 'blob:controlled'); calls.push('revoke') } }
    if (shouldFail) assert.throws(() => downloadVideoFrame(blob, 'frame.png', { document: dom, urlApi }), error => error === failure)
    else downloadVideoFrame(blob, 'frame.png', { document: dom, urlApi })
    assert.equal(link.download, 'frame.png')
    assert.deepEqual(calls, ['create', 'append', 'click', 'remove', 'revoke'])
  }
})
