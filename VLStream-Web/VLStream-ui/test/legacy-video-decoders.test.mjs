import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/utils/legacyVideoDecoders.js', import.meta.url), 'utf8')
  .replaceAll('import.meta.env.BASE_URL', JSON.stringify('/bus/vls-ui/'))
  .replaceAll('export ', '')

function harness() {
  const scripts = []
  const window = {}
  const document = {
    head: { appendChild: script => scripts.push(script) },
    createElement: () => ({ remove() { this.removed = true } })
  }
  let tick
  const actions = new Function('window', 'document', 'setInterval', 'clearInterval', `${source}\nreturn { loadJessibuca, loadH265Web }`)(
    window, document, callback => { tick = callback }, () => { tick = undefined }
  )
  return { ...actions, window, scripts, tick: () => tick() }
}

test('concurrent legacy FLV players reuse one script and a failed script can be retried', async () => {
  const h = harness()
  const first = h.loadJessibuca()
  const second = h.loadJessibuca()
  assert.equal(first, second)
  assert.equal(h.scripts.length, 1)
  assert.equal(h.scripts[0].src, '/bus/vls-ui/jessibuca/jessibuca.js')
  h.scripts[0].onerror()
  await assert.rejects(first)
  assert.equal(h.scripts[0].removed, true)
  const retry = h.loadJessibuca()
  h.window.jessibuca = class Jessibuca {}
  h.scripts[1].onload()
  assert.equal(await retry, h.window.jessibuca)
  assert.equal(await h.loadJessibuca(), h.window.jessibuca)
  assert.equal(h.scripts.length, 2)
})

test('H265 player is available only after its bundled WASM runtime becomes ready', async () => {
  const h = harness()
  let delivered = false
  const player = h.loadH265Web().then(result => { delivered = true; return result })
  assert.equal(h.scripts.length, 1)
  assert.match(h.scripts[0].src, /h265webjs-v20221106\.js$/)
  h.window.new265webjs = function () {}
  h.scripts[0].onload()
  await new Promise(resolve => setImmediate(resolve))
  assert.match(h.scripts[1].src, /missile\.js$/)
  h.window.Module = { cwrap() {} }
  h.scripts[1].onload()
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(delivered, false)
  h.window.runtimeInitialized = true
  h.tick()
  assert.equal(await player, h.window.new265webjs)
  assert.equal(h.scripts.length, 2)
})
