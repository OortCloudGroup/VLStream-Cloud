// Protocol contract matches public/hikvision/webrtcstreamer.js.
export class WebrtcPreviewSession {
  constructor({ video, serverUrl, onState, onReady, onError, fetcher = globalThis.fetch.bind(globalThis), PeerConnection = RTCPeerConnection }) {
    Object.assign(this, { video, onState, onReady, onError, fetcher, PeerConnection })
    this.serverUrl = serverUrl.replace(/\/+$/, '')
    this.closed = false
    this.pc = null
    this.peerId = ''
    this.stream = null
    this.earlyCandidates = []
    this.remoteReady = false
    this.candidateTimer = null
    this.connectionTimer = null
  }

  async request(path, body) {
    const response = await this.fetcher(this.serverUrl + path, body === undefined ? {} : {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    return response.json()
  }

  fail(error) {
    if (this.closed) return
    this.onError(error?.message || String(error))
    this.disconnect()
  }

  async start(url) {
    this.connectionTimer = setTimeout(() => this.fail(new Error('WebRTC 连接超时')), 20000)
    try {
      const config = await this.request('/api/getIceServers')
      if (this.closed) return
      this.peerId = Math.random().toString()
      const pc = this.pc = new this.PeerConnection(config)
      pc.addTransceiver('video', { direction: 'recvonly' })
      pc.addTransceiver('audio', { direction: 'recvonly' })
      pc.createDataChannel('ClientDataChannel')
      pc.ontrack = event => {
        if (this.closed) return
        this.stream = event.streams[0] || new MediaStream([event.track])
        this.video.srcObject = this.stream
        this.video.play().catch(() => {})
        this.onReady()
      }
      pc.onconnectionstatechange = () => {
        if (this.closed) return
        this.onState(pc.connectionState)
        if (pc.connectionState === 'connected') clearTimeout(this.connectionTimer)
        if (pc.connectionState === 'failed') this.fail(new Error('WebRTC 连接失败'))
      }
      pc.onicecandidate = event => {
        if (!event.candidate || this.closed) return
        if (this.remoteReady) this.sendCandidate(event.candidate).catch(error => this.fail(error))
        else this.earlyCandidates.push(event.candidate)
      }
      const offer = await pc.createOffer()
      if (this.closed) return
      await pc.setLocalDescription(offer)
      if (this.closed) return
      const params = new URLSearchParams({ peerid: this.peerId, url, options: 'rtptransport=tcp&timeout=60' })
      const answer = await this.request(`/api/call?${params}`, { type: offer.type, sdp: offer.sdp })
      // A close during the call must release a session that only became visible after the reply.
      if (this.closed) { await this.release(); return }
      if (answer?.type !== 'answer' || !answer.sdp) throw new Error('播放服务未返回有效 WebRTC 应答')
      await pc.setRemoteDescription(answer)
      if (this.closed) { await this.release(); return }
      this.remoteReady = true
      for (const candidate of this.earlyCandidates.splice(0)) await this.sendCandidate(candidate)
      await this.receiveCandidates(0)
    } catch (error) {
      if (this.closed) await this.release()
      else this.fail(error)
    }
  }

  sendCandidate(candidate) {
    if (this.closed) return Promise.resolve()
    return this.request(`/api/addIceCandidate?peerid=${this.peerId}`, candidate)
  }

  async receiveCandidates(attempt) {
    if (this.closed) return
    const candidates = await this.request(`/api/getIceCandidate?peerid=${this.peerId}`)
    if (this.closed) return
    if (!Array.isArray(candidates)) throw new Error('播放服务返回无效 ICE 地址')
    for (const candidate of candidates) await this.pc.addIceCandidate(candidate)
    if (!candidates.length && attempt < 9 && this.pc.connectionState !== 'connected') {
      this.candidateTimer = setTimeout(() => {
        this.receiveCandidates(attempt + 1).catch(error => this.fail(error))
      }, 500)
    }
  }

  release() {
    if (!this.peerId) return Promise.resolve()
    return this.request(`/api/hangup?peerid=${this.peerId}`).catch(() => {})
  }

  disconnect() {
    this.closed = true
    clearTimeout(this.connectionTimer)
    clearTimeout(this.candidateTimer)
    this.pc?.close()
    if (this.stream && this.video.srcObject === this.stream) this.video.srcObject = null
    this.earlyCandidates = []
    return this.release()
  }
}
