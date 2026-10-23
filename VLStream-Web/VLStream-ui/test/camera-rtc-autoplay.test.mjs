import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/utils/cameraRtcSession.js', import.meta.url), 'utf8')
const { CameraRtcSession } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)
const flush = async () => { for (let index = 0; index < 5; index++) await Promise.resolve() }

function harness(t, options = {}) {
  const sockets = [], peers = [], timers = new Map(), diagnostics = [], statuses = []
  let timerId = 0, rejectPlayback, allowPlayback = false, playbackCalls = 0
  const firstPlayback = new Promise((_, reject) => { rejectPlayback = reject })
  class Socket {
    static OPEN = 1
    constructor(url) { this.url = url; this.readyState = 0; this.sent = []; sockets.push(this) }
    open() { this.readyState = 1; this.onopen?.() }
    send(message) { this.sent.push(JSON.parse(message)) }
    close() { this.readyState = 3 }
  }
  class Peer {
    constructor() { this.signalingState = 'stable'; peers.push(this) }
    close() { this.signalingState = 'closed' }
  }
  const video = { controls: false, muted: true, srcObject: null, play: () => { playbackCalls++; return allowPlayback ? Promise.resolve() : firstPlayback } }
  const track = { stopped: false, stop() { this.stopped = true } }
  const stream = { getAudioTracks: () => [], getTracks: () => [track] }
  const runtime = { WebSocket: Socket, RTCPeerConnection: Peer, crypto: { randomUUID: () => 'controlled-session' },
    setTimeout: action => { timers.set(++timerId, action); return timerId }, clearTimeout: id => timers.delete(id),
    setInterval: action => { timers.set(++timerId, action); return timerId }, clearInterval: id => timers.delete(id),
  }
  const session = new CameraRtcSession({ videoElement: video, deviceId: 'controlled-camera', socketUrl: 'ws://controlled-signal',
    runtime, onDiagnostic: event => diagnostics.push(event), onStatus: status => statuses.push(status), ...options })
  session.start(); sockets[0].open()
  const peer = session.createPeerConnection(session.generation)
  peer.ontrack({ streams: [stream] })
  t.after(() => {
    session.stop()
    assert.equal(timers.size, 0)
    assert.equal(sockets[0].readyState, 3)
    assert.equal(peer.signalingState, 'closed')
    assert.equal(track.stopped, true)
    assert.equal(video.srcObject, null)
  })
  const reject = () => { const error = new Error('controlled browser autoplay denial'); error.name = 'NotAllowedError'; rejectPlayback(error) }
  return { session, video, peer, sockets, diagnostics, statuses, reject, playbackCalls: () => playbackCalls,
    allowAndPlay: () => { allowPlayback = true; return video.play() } }
}

test('default session still enables native controls after actual ontrack autoplay rejection', async t => {
  const current = harness(t)
  current.reject(); await flush()
  assert.equal(current.video.controls, true)
  assert.equal(current.session.nativeControlsOnAutoplayFailure, true)
  assert.deepEqual(current.diagnostics.find(event => event.type === 'autoplay-rejected'), {
    type: 'autoplay-rejected', name: 'NotAllowedError', message: 'controlled browser autoplay denial', nativeControlsEnabled: true,
  })
  assert.equal(current.session.retryCount, 0)
})

test('compact session keeps native controls off, reports rejection and allows subsequent explicit play', async t => {
  const current = harness(t, { nativeControlsOnAutoplayFailure: false })
  current.reject(); await flush()
  assert.equal(current.video.controls, false)
  assert.equal(current.diagnostics.find(event => event.type === 'autoplay-rejected')?.nativeControlsEnabled, false)
  assert.equal(current.session.peerConnection, current.peer)
  assert.equal(current.session.retryCount, 0)
  await current.allowAndPlay()
  assert.equal(current.playbackCalls(), 2)
  assert.equal(current.video.controls, false)
  assert.equal(current.sockets.length, 1, 'explicit playback does not reconnect signaling')
})

test('a late autoplay rejection after stop cannot enable controls or publish stale diagnostics', async t => {
  const current = harness(t)
  current.session.stop()
  current.reject(); await flush()
  assert.equal(current.video.controls, false)
  assert.equal(current.diagnostics.some(event => event.type === 'autoplay-rejected'), false)
  assert.equal(current.statuses.at(-1).state, 'idle')
})
