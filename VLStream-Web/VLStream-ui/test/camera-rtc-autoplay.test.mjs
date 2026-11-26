import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/utils/cameraRtcSession.js', import.meta.url), 'utf8')
const { CameraRtcSession } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)
const flush = async () => { for (let index = 0; index < 5; index++) await Promise.resolve() }

function harness(t, { connect = true, ...options } = {}) {
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
  const video = Object.assign(new EventTarget(), {
    controls: false, muted: true, srcObject: null, readyState: 0, videoWidth: 0, paused: true,
    play: () => { playbackCalls++; return allowPlayback ? Promise.resolve() : firstPlayback },
  })
  const track = { stopped: false, stop() { this.stopped = true } }
  const stream = { getAudioTracks: () => [], getTracks: () => [track] }
  const runtime = { WebSocket: Socket, RTCPeerConnection: Peer, crypto: { randomUUID: () => 'controlled-session' },
    setTimeout: action => { timers.set(++timerId, action); return timerId }, clearTimeout: id => timers.delete(id),
    setInterval: action => { timers.set(++timerId, action); return timerId }, clearInterval: id => timers.delete(id),
  }
  const session = new CameraRtcSession({ videoElement: video, deviceId: 'controlled-camera', socketUrl: 'ws://controlled-signal',
    runtime, onDiagnostic: event => diagnostics.push(event), onStatus: status => statuses.push(status), ...options })
  session.start()
  let peer
  if (connect) {
    sockets[0].open()
    peer = session.createPeerConnection(session.generation)
    peer.ontrack({ streams: [stream] })
  }
  t.after(() => {
    session.stop()
    assert.equal(timers.size, 0)
    assert.equal(sockets[0].readyState, 3)
    if (peer) {
      assert.equal(peer.signalingState, 'closed')
      assert.equal(track.stopped, true)
    }
    assert.equal(video.srcObject, null)
  })
  const reject = () => { const error = new Error('controlled browser autoplay denial'); error.name = 'NotAllowedError'; rejectPlayback(error) }
  const fireCallTimeout = () => {
    const action = timers.get(session.callTimer)
    assert.equal(typeof action, 'function')
    timers.delete(session.callTimer)
    action()
  }
  return { session, video, peer, sockets, diagnostics, statuses, reject, fireCallTimeout, playbackCalls: () => playbackCalls,
    allowAndPlay: () => { allowPlayback = true; return video.play() } }
}

test('default session still enables native controls after actual ontrack autoplay rejection', async t => {
  const current = harness(t)
  current.reject(); await flush()
  assert.equal(current.video.controls, true)
  assert.equal(current.session.nativeControlsOnAutoplayFailure, true)
  assert.equal(current.statuses.at(-1).state, 'awaiting-playback')
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

test('a signaling socket that never opens times out and is closed before retry', t => {
  const current = harness(t, { connect: false })
  assert.equal(current.statuses.at(-1).state, 'connecting')
  current.fireCallTimeout()
  assert.equal(current.sockets[0].readyState, 3)
  assert.equal(current.statuses.at(-1).state, 'retrying')
  assert.equal(current.session.retryCount, 1)
})

test('ICE and an attached stream without decoded video do not complete startup', t => {
  const current = harness(t)
  current.peer.iceConnectionState = 'connected'
  current.peer.oniceconnectionstatechange()
  current.video.paused = false
  current.video.readyState = 3
  current.video.dispatchEvent(new Event('playing'))
  assert.equal(current.statuses.at(-1).state, 'negotiating')
  current.fireCallTimeout()
  assert.equal(current.statuses.at(-1).state, 'retrying')
  assert.equal(current.video.srcObject, null)
})

test('decoded video completes startup and autoplay stays muted', async t => {
  const current = harness(t)
  current.session.retryCount = 2
  await current.allowAndPlay()
  current.peer.ontrack({ streams: [current.video.srcObject] })
  await flush()
  assert.equal(current.video.muted, true)
  current.peer.iceConnectionState = 'connected'
  current.peer.oniceconnectionstatechange()
  assert.equal(current.session.retryCount, 2, 'ICE alone does not reset retries')
  Object.assign(current.video, { paused: false, readyState: 2, videoWidth: 1280 })
  current.video.dispatchEvent(new Event('loadeddata'))
  assert.equal(current.statuses.at(-1).state, 'playing')
  assert.equal(current.session.callTimer, null)
  assert.equal(current.session.retryCount, 0)
  assert.equal(current.video.muted, true)
})
