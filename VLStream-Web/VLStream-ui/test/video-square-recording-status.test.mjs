import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { computed, effectScope, ref, shallowRef, watch } from 'vue'

const script = readFileSync(new URL('../src/composables/useDeviceRecording.js', import.meta.url), 'utf8')
  .replace(/^import .*$/gm, '').replace('export function ', 'function ')
const load = new Function('deps', `with (deps) { ${script}\nreturn useDeviceRecording }`)

function setup(status) {
  let now = 0
  let timestamp = 0
  let dispose
  let revision = 1
  const timers = new Map()
  const listeners = new Set()
  const document = {
    hidden: false,
    addEventListener: (_, callback) => listeners.add(callback),
    removeEventListener: (_, callback) => listeners.delete(callback)
  }
  const flush = async () => { await Promise.resolve(); await Promise.resolve() }
  const useDeviceRecording = load({
    computed, shallowRef, watch, document, AbortController,
    authManager: {
      getIdentitySnapshot: () => ({ revision }),
      isIdentityCurrent: identity => identity.revision === revision
    },
    platformRecording: { status },
    onBeforeUnmount: callback => { dispose = callback },
    Date: { now: () => now },
    setTimeout: (callback, delay) => {
      const id = ++timestamp
      timers.set(id, { callback, at: now + delay })
      return id
    },
    clearTimeout: id => timers.delete(id),
  })
  return {
    async advance(ms) {
      const end = now + ms
      await flush()
      while (timers.size) {
        const [id, task] = [...timers].sort((left, right) => left[1].at - right[1].at)[0]
        if (task.at > end) break
        now = task.at
        timers.delete(id)
        task.callback()
        await flush()
      }
      now = end
    },
    player(sources) {
      const scope = effectScope()
      const recording = scope.run(() => useDeviceRecording(() => ({ sources })))
      const release = dispose
      return { recording, close() { release(); scope.stop() } }
    },
    changeIdentity() { revision += 1 },
    hide(hidden) {
      document.hidden = hidden
      for (const listener of listeners) listener()
    },
    listeners,
  }
}

test('recording windows share per-source polls and prioritize confirmed recordings', async () => {
  const calls = []
  const { advance, player, listeners } = setup(async (protocol, id) => {
    calls.push([protocol, id])
    return protocol === 'RTSP'
  })
  const rtsp = { protocol: 'RTSP', rowId: 4 }
  const onvif = { protocol: 'ONVIF', rowId: 4 }
  const first = player([rtsp, onvif, rtsp])
  const second = player([rtsp])
  await advance(0)
  assert.deepEqual(calls, [['RTSP', '4'], ['ONVIF', '4']])
  assert.equal(first.recording.value, true)
  assert.equal(second.recording.value, true)
  assert.equal(listeners.size, 1)
  await advance(15000)
  assert.equal(calls.filter(([protocol]) => protocol === 'RTSP').length, 4)
  assert.equal(calls.filter(([protocol]) => protocol === 'ONVIF').length, 2)
  first.close()
  await advance(15000)
  assert.equal(calls.filter(([protocol]) => protocol === 'RTSP').length, 7)
  assert.equal(calls.filter(([protocol]) => protocol === 'ONVIF').length, 2)
  second.close()
  assert.equal(listeners.size, 0)
  await advance(15000)
  assert.equal(calls.length, 9)
})

test('a failed check clears its badge and backs off, while a hidden tab cancels pending work', async () => {
  let calls = 0
  let pending
  const { advance, player, hide } = setup((_, __, signal) => {
    calls += 1
    if (calls === 1) return Promise.resolve(true)
    if (calls === 2) return Promise.reject(new Error('status failed'))
    return new Promise(resolve => { pending = { signal, resolve } })
  })
  const window = player([{ protocol: 'RTSP', rowId: 1 }])
  await advance(0)
  assert.equal(window.recording.value, true)
  await advance(5000)
  assert.equal(window.recording.value, false)
  await advance(29999)
  assert.equal(calls, 2)
  await advance(1)
  const previous = pending
  assert.equal(calls, 3)
  hide(true)
  assert.equal(previous.signal.aborted, true)
  previous.resolve(true)
  await advance(60000)
  assert.equal(calls, 3)
  assert.equal(window.recording.value, false)
  hide(false)
  await advance(0)
  assert.equal(calls, 4)
  pending.resolve(true)
  await advance(0)
  assert.equal(window.recording.value, true)
  window.close()
})

test('polling admits at most three requests and rejects results from an obsolete identity', async () => {
  const pending = new Map()
  const { advance, player, changeIdentity } = setup((_, id, signal) => new Promise(resolve => pending.set(id, { signal, resolve })))
  const windows = [1, 2, 3, 4].map(rowId => player([{ protocol: 'RTSP', rowId }]))
  await advance(0)
  assert.deepEqual([...pending.keys()], ['1', '2', '3'])
  changeIdentity()
  pending.get('1').resolve(true)
  await advance(0)
  assert.equal(windows[0].recording.value, false)
  assert.deepEqual([...pending.keys()], ['1', '2', '3', '4'])
  pending.get('4').resolve(true)
  await advance(0)
  assert.equal(windows[3].recording.value, true)
  windows.forEach(window => window.close())
  assert.equal(pending.get('2').signal.aborted, true)
  assert.equal(pending.get('3').signal.aborted, true)
})

const playbackSource = readFileSync(new URL('../src/views/VideoAggregation/VideoPlayback.vue', import.meta.url), 'utf8')
  .split('<script setup>')[1].split('</script>')[0].replace(/^import .*$/gm, '')
const loadPlayback = new Function('deps', `with (deps) {
  ${playbackSource}
  return { selectDevice, retryQuery, play, closePlayer, clipListStatus, playerStatus, segments, url }
}`)
const camera = { sources: [{ protocol: 'ONVIF', rowId: '1' }] }

function setupPlayback(api) {
  return loadPlayback({
    api, computed, ref, watch,
    useRoute: () => ({ query: {} }),
    onMounted() {},
    onBeforeUnmount() {},
    recordingError: error => error.message,
    t: value => value
  })
}

test('recording playback distinguishes a missing channel, a failed lookup and a channel without clips', async () => {
  let channels = []
  let lookups = 0
  const player = setupPlayback({
    sources: async () => channels,
    segments: async () => {
      lookups += 1
      if (lookups === 1) throw new Error('lookup failed')
      return []
    }
  })
  await player.selectDevice(camera)
  assert.equal(player.clipListStatus.value, '暂无录像通道')
  assert.equal(lookups, 0)
  channels = [{ id: 'channel' }]
  await player.selectDevice(camera)
  assert.equal(player.clipListStatus.value, '录像查询失败')
  await player.retryQuery()
  assert.equal(player.clipListStatus.value, '当天暂无录像')
  assert.equal(lookups, 2)
})

test('the recording list downloads media on selection and cancels stale playback on close', async () => {
  let pendingFile
  const clip = { id: 'clip', start_ms: 0 }
  const player = setupPlayback({
    sources: async () => [{ id: 'channel' }],
    segments: async () => [clip],
    file: (id, signal) => new Promise(resolve => { pendingFile = { id, signal, resolve } })
  })
  await player.selectDevice(camera)
  assert.deepEqual(player.segments.value, [clip])
  assert.equal(player.playerStatus.value, '选择录像片段播放')
  assert.equal(pendingFile, undefined)
  const playing = player.play(clip)
  assert.equal(pendingFile.id, 'clip')
  assert.equal(player.playerStatus.value, '正在加载录像')
  player.closePlayer()
  assert.equal(pendingFile.signal.aborted, true)
  pendingFile.resolve(new Blob(['media'], { type: 'video/mp4' }))
  await playing
  assert.equal(player.url.value, '')
})
