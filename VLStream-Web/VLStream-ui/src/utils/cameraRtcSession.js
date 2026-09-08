/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

const DEFAULT_DISCONNECT_GRACE_MS = 8000
const DEFAULT_CALL_TIMEOUT_MS = 25000
const DEFAULT_RETRY_DELAY_MS = 1000
const DEFAULT_MAX_RETRY_DELAY_MS = 8000
const DEFAULT_MAX_RETRIES = -1
const DEFAULT_HEARTBEAT_INTERVAL_MS = 20000

const toUrlList = (urls) => {
  if (Array.isArray(urls)) return urls
  return urls ? [urls] : []
}

const parseRtcConfiguration = (value) => {
  if (!value) return null
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

const isHostname = (host) => Boolean(host && !/^\d{1,3}(?:\.\d{1,3}){3}$/.test(host) && !host.includes(':'))

const expandTurnUrl = (value) => {
  const url = String(value || '').trim()
  if (!url) return []

  const result = [url]
  if (!/^turn:/i.test(url) || !/[?&]transport=udp(?:&|$)/i.test(url)) return result

  result.push(url.replace(/([?&]transport=)udp(?=&|$)/i, '$1tcp'))

  const match = url.match(/^turn:([^:?/]+)(?::\d+)?(?:\?.*)?$/i)
  if (match && isHostname(match[1])) {
    result.push(`turns:${match[1]}:5349?transport=tcp`)
  }
  return result
}

export const parseCameraRtcTurnUrls = (value) =>
  String(value || '')
    .split(/[;,\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)

/**
 * Keep the device-provided ICE configurations intact for the first attempts.
 * Later attempts rotate through TCP and TURN/TLS alternatives one at a time;
 * some camera firmware rejects or delays a single configuration containing a
 * mixture of transports even though browsers accept it.
 */
export const buildCameraRtcIceStrategies = (payload = {}, extraTurnUrls = []) => {
  const originalConfigurations = [payload.iceServers, payload.domainnameiceServers, payload.domainnameIceServers]
    .map(parseRtcConfiguration)
    .filter((config) => Array.isArray(config?.iceServers) && config.iceServers.length)
  const strategies = []
  const seen = new Set()
  const addStrategy = (configuration) => {
    const strategy = JSON.parse(JSON.stringify(configuration))
    const key = JSON.stringify(strategy)
    if (seen.has(key)) return
    seen.add(key)
    strategies.push(strategy)
  }

  originalConfigurations.forEach(addStrategy)

  for (const configuration of originalConfigurations) {
    for (const server of configuration.iceServers) {
      for (const originalUrl of toUrlList(server.urls)) {
        for (const fallbackUrl of expandTurnUrl(originalUrl).slice(1)) {
          addStrategy({
            iceServers: [{ ...server, urls: fallbackUrl }],
            iceTransportPolicy: configuration.iceTransportPolicy || 'all',
            iceCandidatePoolSize: Number(configuration.iceCandidatePoolSize || 0),
          })
        }
      }
    }
  }

  const credentialSource =
    originalConfigurations
      .flatMap((configuration) => configuration.iceServers)
      .find((server) => server?.username || server?.credential) || {}
  for (const url of extraTurnUrls) {
    addStrategy({
      iceServers: [
        {
          urls: url,
          username: credentialSource.username,
          credential: credentialSource.credential,
        },
      ],
      iceTransportPolicy: 'all',
      iceCandidatePoolSize: 0,
    })
  }

  return strategies.length ? strategies : [{ iceServers: [] }]
}

const newGuid = (runtime) => {
  if (runtime.crypto?.randomUUID) return runtime.crypto.randomUUID().toUpperCase()
  const segment = () => ((65536 * (1 + Math.random())) | 0).toString(16).substring(1)
  return `${segment()}${segment()}-${segment()}-4${segment().substring(0, 3)}-${segment()}-${segment()}${segment()}${segment()}`.toUpperCase()
}

export class CameraRtcSession {
  constructor(options) {
    this.videoElement = options.videoElement
    this.deviceId = options.deviceId
    this.socketUrl = String(options.socketUrl || '').replace(/\/+$/, '')
    this.runtime = options.runtime || globalThis
    this.extraTurnUrls = options.extraTurnUrls || []
    this.disconnectGraceMs = options.disconnectGraceMs ?? DEFAULT_DISCONNECT_GRACE_MS
    this.callTimeoutMs = options.callTimeoutMs ?? DEFAULT_CALL_TIMEOUT_MS
    this.retryDelayMs = options.retryDelayMs ?? DEFAULT_RETRY_DELAY_MS
    this.maxRetryDelayMs = options.maxRetryDelayMs ?? DEFAULT_MAX_RETRY_DELAY_MS
    this.maxRetries = options.maxRetries ?? DEFAULT_MAX_RETRIES
    this.heartbeatIntervalMs = options.heartbeatIntervalMs ?? DEFAULT_HEARTBEAT_INTERVAL_MS
    this.onStatus = options.onStatus || (() => {})
    this.onDiagnostic = options.onDiagnostic || (() => {})

    this.socket = null
    this.peerConnection = null
    this.sessionId = ''
    this.clientId = ''
    this.iceConfiguration = { iceServers: [] }
    this.iceStrategies = []
    this.dataChannel = null
    this.pendingRemoteCandidates = []
    this.remoteDescriptionReady = false
    this.retryCount = 0
    this.disconnectTimer = null
    this.retryTimer = null
    this.callTimer = null
    this.heartbeatTimer = null
    this.stopped = true
    this.generation = 0
  }

  start() {
    if (!this.videoElement || !this.deviceId || !this.socketUrl) {
      this.setStatus('failed', 'CameraRTC 播放参数不完整')
      return
    }
    this.stopped = false
    this.retryCount = 0
    this.openSocket()
  }

  stop() {
    if (this.stopped) return
    this.sendDisconnect()
    this.stopped = true
    this.generation += 1
    this.clearTimers()
    this.cleanupPeerConnection()
    this.cleanupSocket()
    this.setStatus('idle', '')
  }

  retryNow() {
    if (this.stopped) return
    this.retryCount = 0
    this.clearTimers()
    this.cleanupPeerConnection()
    if (this.isSocketOpen()) this.connectToDevice()
    else this.openSocket()
  }

  openSocket() {
    if (this.stopped) return
    this.cleanupSocket()
    this.setStatus('connecting', '正在连接 CameraRTC 信令服务…')
    this.clientId = newGuid(this.runtime)
    const socket = new this.runtime.WebSocket(`${this.socketUrl}/wswebclient/${this.clientId}`)
    this.socket = socket
    socket.onopen = () => {
      if (this.stopped || socket !== this.socket) return
      this.emitDiagnostic('signaling-open')
      this.startHeartbeat()
      this.connectToDevice()
    }
    socket.onmessage = (event) => this.handleSocketMessage(event)
    socket.onerror = () => this.emitDiagnostic('signaling-error')
    socket.onclose = (event) => {
      if (socket !== this.socket) return
      this.socket = null
      this.emitDiagnostic('signaling-close', { code: event.code })
      if (!this.stopped) this.scheduleReconnect('信令连接中断')
    }
  }

  connectToDevice() {
    if (this.stopped || !this.isSocketOpen()) return
    this.generation += 1
    this.cleanupPeerConnection()
    this.clearAttemptTimers()
    this.sessionId = `PCWEB-${newGuid(this.runtime)}`
    this.pendingRemoteCandidates = []
    this.remoteDescriptionReady = false
    this.setStatus('connecting', '正在连接设备…')
    this.send('__connectto', { to: this.deviceId })
    this.armCallTimeout()
  }

  async handleSocketMessage(event) {
    let message
    try {
      message = JSON.parse(event.data)
    } catch {
      this.emitDiagnostic('invalid-signaling-message')
      return
    }
    const data = message?.data || {}
    if (data.sessionId !== this.sessionId) return

    switch (message.eventName) {
      case '_create':
        this.handleCreate(data)
        break
      case '_offer':
        await this.handleOffer(data)
        break
      case '_ice_candidate':
        await this.handleRemoteCandidate(data)
        break
      case '_session_failed':
        this.scheduleReconnect(data.message === 'call time out' ? '设备呼叫超时' : '设备会话失败')
        break
      case '_session_disconnected':
        this.scheduleReconnect('设备结束了播放会话')
        break
      case '_connectinfo':
        this.emitDiagnostic('connection-info', {
          relay: this.isRelayConnection(data.message),
        })
        break
      default:
        break
    }
  }

  handleCreate(data) {
    if (!['online', 'sleep'].includes(data.state)) {
      this.scheduleReconnect('设备当前离线')
      return
    }
    this.iceStrategies = buildCameraRtcIceStrategies(data, this.extraTurnUrls)
    const strategyIndex = this.retryCount <= 1 ? 0 : (this.retryCount - 1) % this.iceStrategies.length
    this.iceConfiguration = this.iceStrategies[strategyIndex]
    this.emitDiagnostic('ice-strategy', {
      strategyIndex,
      urls: this.iceConfiguration.iceServers.flatMap((server) => toUrlList(server.urls)),
    })
    this.setStatus('negotiating', '正在协商媒体通道…')
    this.send('__call', {
      to: this.deviceId,
      datachannel: 'true',
      audio: 'recvonly',
      video: 'recvonly',
      user: 'admin',
      pwd: '123456',
      mode: 'live',
      source: 'MainStream',
      iceservers: JSON.stringify(this.iceConfiguration),
    })
    this.armCallTimeout()
  }

  async handleOffer(data) {
    const generation = this.generation
    try {
      const peerConnection = this.createPeerConnection(generation)
      await peerConnection.setRemoteDescription({
        type: 'offer',
        sdp: data.sdp,
      })
      if (!this.isCurrentPeer(peerConnection, generation)) return
      this.remoteDescriptionReady = true
      await this.flushRemoteCandidates(peerConnection, generation)
      const answer = await peerConnection.createAnswer()
      if (!this.isCurrentPeer(peerConnection, generation)) return
      await peerConnection.setLocalDescription(answer)
      if (!this.isCurrentPeer(peerConnection, generation)) return
      this.send('__answer', {
        to: this.deviceId,
        type: answer.type,
        sdp: answer.sdp,
      })
      this.setStatus('negotiating', '正在建立视频通道…')
    } catch (error) {
      this.emitDiagnostic('offer-error', {
        message: error?.message || String(error),
      })
      this.scheduleReconnect('媒体协商失败')
    }
  }

  createPeerConnection(generation) {
    const pendingRemoteCandidates = this.pendingRemoteCandidates
    this.cleanupPeerConnection()
    this.pendingRemoteCandidates = pendingRemoteCandidates
    const peerConnection = new this.runtime.RTCPeerConnection(this.iceConfiguration)
    this.peerConnection = peerConnection
    if (typeof peerConnection.createDataChannel === 'function') {
      try {
        this.dataChannel = peerConnection.createDataChannel('mydatachannel')
        this.dataChannel.onopen = () => this.emitDiagnostic('data-channel-open')
        this.dataChannel.onclose = () => this.emitDiagnostic('data-channel-close')
        this.dataChannel.onerror = () => this.emitDiagnostic('data-channel-error')
      } catch (error) {
        this.emitDiagnostic('data-channel-create-error', { message: error?.message || String(error) })
      }
    }

    peerConnection.ontrack = (event) => {
      if (!this.isCurrentPeer(peerConnection, generation)) return
      const stream = event.streams?.[0] || new this.runtime.MediaStream([event.track])
      this.videoElement.srcObject = stream
      stream.getAudioTracks().forEach((track) => {
        track.enabled = true
      })
      const playResult = this.videoElement.play()
      if (playResult?.then) {
        playResult
          .then(() => {
            this.videoElement.muted = false
          })
          .catch(() => {
            this.videoElement.controls = true
          })
      }
      this.setStatus('playing', '播放中')
    }

    peerConnection.onicecandidate = (event) => {
      if (!event.candidate || !this.isCurrentPeer(peerConnection, generation)) return
      this.send('__ice_candidate', {
        to: this.deviceId,
        candidate: JSON.stringify({
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid,
          sdpMLineIndex: event.candidate.sdpMLineIndex,
        }),
      })
    }

    peerConnection.oniceconnectionstatechange = () => {
      if (!this.isCurrentPeer(peerConnection, generation)) return
      this.handleIceConnectionState(peerConnection.iceConnectionState)
    }
    peerConnection.onconnectionstatechange = () => {
      if (!this.isCurrentPeer(peerConnection, generation)) return
      this.emitDiagnostic('peer-state', {
        connectionState: peerConnection.connectionState,
        iceConnectionState: peerConnection.iceConnectionState,
      })
    }
    return peerConnection
  }

  async handleRemoteCandidate(data) {
    let candidate
    try {
      candidate = typeof data.candidate === 'string' ? JSON.parse(data.candidate) : data.candidate
    } catch {
      this.emitDiagnostic('invalid-remote-candidate')
      return
    }
    if (!candidate) return

    if (!this.peerConnection || !this.remoteDescriptionReady) {
      this.pendingRemoteCandidates.push(candidate)
      return
    }
    try {
      await this.peerConnection.addIceCandidate(candidate)
    } catch (error) {
      this.emitDiagnostic('remote-candidate-error', {
        message: error?.message || String(error),
      })
    }
  }

  async flushRemoteCandidates(peerConnection, generation) {
    const candidates = this.pendingRemoteCandidates.splice(0)
    for (const candidate of candidates) {
      if (!this.isCurrentPeer(peerConnection, generation)) return
      try {
        await peerConnection.addIceCandidate(candidate)
      } catch (error) {
        this.emitDiagnostic('remote-candidate-error', {
          message: error?.message || String(error),
        })
      }
    }
  }

  handleIceConnectionState(state) {
    this.emitDiagnostic('ice-state', { state })
    if (['connected', 'completed'].includes(state)) {
      this.clearDisconnectTimer()
      this.clearCallTimer()
      this.retryCount = 0
      if (this.videoElement.srcObject) this.setStatus('playing', '播放中')
      else this.setStatus('negotiating', '媒体通道已连接，正在等待画面…')
      return
    }

    if (state === 'disconnected') {
      if (this.disconnectTimer) return
      this.setStatus('recovering', `网络波动，等待 ${Math.ceil(this.disconnectGraceMs / 1000)} 秒自动恢复…`)
      const peerConnection = this.peerConnection
      this.disconnectTimer = this.runtime.setTimeout(() => {
        this.disconnectTimer = null
        if (peerConnection === this.peerConnection && peerConnection?.iceConnectionState === 'disconnected') {
          this.scheduleReconnect('媒体连接中断')
        }
      }, this.disconnectGraceMs)
      return
    }

    if (state === 'failed') {
      this.clearDisconnectTimer()
      this.scheduleReconnect('媒体连接失败')
    }
  }

  scheduleReconnect(reason) {
    if (this.stopped || this.retryTimer) return
    this.clearAttemptTimers()
    this.sendDisconnect()
    this.cleanupPeerConnection()

    if (this.maxRetries >= 0 && this.retryCount >= this.maxRetries) {
      this.setStatus('failed', `${reason}，自动重连已达上限`)
      return
    }

    const attempt = ++this.retryCount
    const maxExponent = Math.max(0, Math.ceil(Math.log2(this.maxRetryDelayMs / this.retryDelayMs)))
    const exponent = Math.min(attempt - 1, maxExponent)
    const delay = Math.min(this.retryDelayMs * 2 ** exponent, this.maxRetryDelayMs)
    const attemptLabel = this.maxRetries >= 0 ? `${attempt}/${this.maxRetries}` : `第 ${attempt} 次`
    this.setStatus(
      'retrying',
      `${reason}，${Math.max(1, Math.ceil(delay / 1000))} 秒后重连（${attemptLabel}）`,
    )
    this.retryTimer = this.runtime.setTimeout(() => {
      this.retryTimer = null
      if (this.stopped) return
      if (this.isSocketOpen()) this.connectToDevice()
      else this.openSocket()
    }, delay)
  }

  armCallTimeout() {
    this.clearCallTimer()
    this.callTimer = this.runtime.setTimeout(() => {
      this.callTimer = null
      this.scheduleReconnect('设备呼叫超时')
    }, this.callTimeoutMs)
  }

  startHeartbeat() {
    this.clearHeartbeat()
    this.heartbeatTimer = this.runtime.setInterval(() => {
      if (!this.stopped && this.sessionId && this.isSocketOpen()) {
        this.send('__ping', { to: this.deviceId })
      }
    }, this.heartbeatIntervalMs)
  }

  send(eventName, data = {}) {
    if (!this.isSocketOpen()) return false
    this.socket.send(
      JSON.stringify({
        eventName,
        data: {
          sessionId: this.sessionId,
          sessionType: 'IE',
          messageId: newGuid(this.runtime),
          from: this.clientId,
          ...data,
        },
      }),
    )
    return true
  }

  sendDisconnect() {
    if (!this.sessionId || !this.isSocketOpen()) return
    this.send('__disconnected', { to: this.deviceId })
  }

  isRelayConnection(message) {
    try {
      return JSON.parse(message)?.type === 'relay'
    } catch {
      return false
    }
  }

  isSocketOpen() {
    const openState = this.runtime.WebSocket?.OPEN ?? 1
    return this.socket?.readyState === openState
  }

  isCurrentPeer(peerConnection, generation) {
    return !this.stopped && peerConnection === this.peerConnection && generation === this.generation
  }

  cleanupPeerConnection() {
    const peerConnection = this.peerConnection
    this.peerConnection = null
    this.remoteDescriptionReady = false
    this.pendingRemoteCandidates = []
    const dataChannel = this.dataChannel
    this.dataChannel = null
    if (dataChannel) {
      dataChannel.onopen = null
      dataChannel.onclose = null
      dataChannel.onerror = null
      if (dataChannel.readyState !== 'closed') dataChannel.close()
    }
    if (peerConnection) {
      peerConnection.ontrack = null
      peerConnection.onicecandidate = null
      peerConnection.oniceconnectionstatechange = null
      peerConnection.onconnectionstatechange = null
      if (peerConnection.signalingState !== 'closed') peerConnection.close()
    }
    const stream = this.videoElement?.srcObject
    if (stream?.getTracks) stream.getTracks().forEach((track) => track.stop())
    if (this.videoElement) this.videoElement.srcObject = null
  }

  cleanupSocket() {
    this.clearHeartbeat()
    const socket = this.socket
    this.socket = null
    if (!socket) return
    socket.onopen = null
    socket.onmessage = null
    socket.onerror = null
    socket.onclose = null
    if (socket.readyState === 0 || socket.readyState === 1) socket.close(1000, 'player closed')
  }

  clearAttemptTimers() {
    this.clearDisconnectTimer()
    this.clearCallTimer()
  }

  clearDisconnectTimer() {
    if (!this.disconnectTimer) return
    this.runtime.clearTimeout(this.disconnectTimer)
    this.disconnectTimer = null
  }

  clearCallTimer() {
    if (!this.callTimer) return
    this.runtime.clearTimeout(this.callTimer)
    this.callTimer = null
  }

  clearTimers() {
    this.clearAttemptTimers()
    if (!this.retryTimer) return
    this.runtime.clearTimeout(this.retryTimer)
    this.retryTimer = null
  }

  clearHeartbeat() {
    if (!this.heartbeatTimer) return
    this.runtime.clearInterval(this.heartbeatTimer)
    this.heartbeatTimer = null
  }

  setStatus(state, message) {
    this.onStatus({ state, message, retryCount: this.retryCount })
  }

  emitDiagnostic(type, details = {}) {
    this.onDiagnostic({ type, ...details })
  }
}
