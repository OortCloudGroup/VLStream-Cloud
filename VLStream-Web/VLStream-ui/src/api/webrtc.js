/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

// , before .
export let WEBRTC_SERVER_BASE_URL = '/bus/webrtc-streamer-server'
let webRTCBackendConfigLoader = null

/**
 * after , already Import ES module live binding .
 */
export function applyWebRTCServerBaseUrl(serverUrl) {
  const normalized = String(serverUrl || '').trim().replace(/\/+$/, '')
  if (normalized) {
    WEBRTC_SERVER_BASE_URL = normalized
  }
  return WEBRTC_SERVER_BASE_URL
}

/**
 * Get WebRTC configurationinfo
 */
export function getWebRTCConfig() {
  return request({
    url: '/api/webrtc/config',
    method: 'get'
  })
}

/**
 * Get WebRTC service
 */
export function getWebRTCServerStatus() {
  return request({
    url: '/api/webrtc/status',
    method: 'get'
  })
}

/**
 * Get WebRTC ( , old )
 */
export function getWebRTCStatus() {
  return getWebRTCServerStatus()
}

/**
 * new WebRTC
 */
export function refreshWebRTCConnection() {
  return request({
    url: '/api/webrtc/refresh',
    method: 'post'
  })
}

/**
 * WebRTC service
 */
export function startWebRTCServer() {
  return request({
    url: '/api/webrtc/start',
    method: 'post'
  })
}

/**
 * WebRTC service
 */
export function stopWebRTCServer() {
  return request({
    url: '/api/webrtc/stop',
    method: 'post'
  })
}

/**
 * RTSP whether
 * @param {string} rtspUrl RTSP
 */
export function checkRtspStream(rtspUrl) {
  return request({
    url: '/api/webrtc/validate',
    method: 'post',
    data: {
      rtspUrl
    }
  })
}

/**
 * Get WebRTC configuration
 * @param {string} streamId ID
 */
export function getStreamConfig(streamId) {
  return request({
    url: `/api/webrtc/check/${streamId}`,
    method: 'get'
  })
}

/**
 * WebRTC
 * @param {string} rtspUrl RTSP
 * @param {object} options configuration item
 */
export function createWebRTCStream(rtspUrl, options = {}) {
  return request({
    url: '/api/webrtc/start',
    method: 'post',
    data: {
      deviceId: options.deviceId || 'temp_' + Date.now(),
      rtspUrl,
      ...options
    }
  })
}

/**
 * WebRTC
 * @param {string} streamId ID
 */
export function destroyWebRTCStream(streamId) {
  return request({
    url: '/api/webrtc/stop',
    method: 'post',
    data: {
      deviceId: streamId
    }
  })
}

/**
 * Get all WebRTC
 */
export function getActiveStreams() {
  return request({
    url: '/api/webrtc/active',
    method: 'get'
  })
}

/**
 * WebRTC info
 * @param {string} streamId ID
 */
export function getStreamStats(streamId) {
  return request({
    url: `/api/webrtc/check/${streamId}`,
    method: 'get'
  })
}

/**
 * WebRTC-streamer API ( after )
 * @param {string} endpoint API
 * @param {object} options item
 */
export function callWebRTCStreamerAPI(endpoint, options = {}) {
  const {
    method = 'GET',
    data = null,
    headers = {}
  } = options

  const url = `${WEBRTC_SERVER_BASE_URL}${endpoint}`

  const config = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...headers
    }
  }

  if (data && (method === 'POST' || method === 'PUT')) {
    config.body = JSON.stringify(data)
  }

  return fetch(url, config)
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
      return response.json()
    })
    .catch(error => {
      console.error('WebRTC-streamer API call failed:', error)
      throw error
    })
}

/**
 * WebRTC Offer
 * @param {string} streamId ID
 * @param {RTCSessionDescription} offer WebRTC Offer
 */
export function createWebRTCOffer(streamId, offer) {
  return callWebRTCStreamerAPI('/api/call', {
    method: 'POST',
    data: {
      type: 'offer',
      sdp: offer.sdp,
      streamId: streamId
    }
  })
}

/**
 * Process WebRTC Answer
 * @param {string} streamId ID
 * @param {RTCSessionDescription} answer WebRTC Answer
 */
export function handleWebRTCAnswer(streamId, answer) {
  return callWebRTCStreamerAPI('/api/call', {
    method: 'POST',
    data: {
      type: 'answer',
      sdp: answer.sdp,
      streamId: streamId
    }
  })
}

/**
 * ICE Candidate
 * @param {string} streamId ID
 * @param {RTCIceCandidate} candidate ICE
 */
export function sendICECandidate(streamId, candidate) {
  return callWebRTCStreamerAPI('/api/call', {
    method: 'POST',
    data: {
      type: 'candidate',
      candidate: candidate.candidate,
      sdpMid: candidate.sdpMid,
      sdpMLineIndex: candidate.sdpMLineIndex,
      streamId: streamId
    }
  })
}

/**
 * Get WebRTC-streamer service info
 */
export function getWebRTCStreamerInfo() {
  return callWebRTCStreamerAPI('/api/getServerOptions')
}

/**
 * Get device
 */
export function getVideoDevices() {
  return callWebRTCStreamerAPI('/api/getVideoDeviceList')
}

/**
 * Get device
 */
export function getAudioDevices() {
  return callWebRTCStreamerAPI('/api/getAudioDeviceList')
}

/**
 * RTSP WebRTC-streamer
 * @param {string} name
 * @param {string} url RTSP URL
 */
export function addRTSPStream(name, url) {
  return callWebRTCStreamerAPI('/api/addStream', {
    method: 'POST',
    data: {
      name: name,
      url: url
    }
  })
}

/**
 * RTSP
 * @param {string} name
 */
export function removeRTSPStream(name) {
  return callWebRTCStreamerAPI(`/api/removeStream?name=${encodeURIComponent(name)}`, {
    method: 'DELETE'
  })
}

/**
 * Get
 */
export function getStreamList() {
  return callWebRTCStreamerAPI('/api/getStreamList')
}

/**
 * Get WebRTCconfiguration
 */
export async function getWebRTCBackendConfig() {
  console.log('🔧 getWebRTCBackendConfig 调用 - 版本v4-20250711 - 修复后路径: /api/webrtc/config')
  const response = await request({
    url: '/api/webrtc/config',
    method: 'get'
  })
  if (response?.code === 200 && response?.data?.serverUrl) {
    applyWebRTCServerBaseUrl(response.data.serverUrl)
  }
  return response
}

/**
 * in page only Load configuration, all WebRTC .
 */
export function ensureWebRTCBackendConfig() {
  if (!webRTCBackendConfigLoader) {
    webRTCBackendConfigLoader = getWebRTCBackendConfig().catch(error => {
      webRTCBackendConfigLoader = null
      console.warn('读取 WebRTC 运行时配置失败，继续使用同源默认地址:', error)
      return null
    })
  }
  return webRTCBackendConfigLoader
}

/**
 * Get WebRTC
 */
export function getWebRTCBackendStatus() {
  return request({
    url: '/api/webrtc/status',
    method: 'get'
  })
}

/**
 * RTSP whether
 * @param {string} rtspUrl RTSP
 */
export function validateRtspStream(rtspUrl) {
  return request({
    url: '/api/webrtc/validate',
    method: 'post',
    data: {
      rtspUrl
    }
  })
}

/**
 * WebRTC
 * @param {string} deviceId deviceID
 * @param {string} rtspUrl RTSP
 */
export function startWebRTCPlay(deviceId, rtspUrl) {
  console.log('🚀 startWebRTCPlay调用 - 版本20250711-v4 - API路径已修复为: /api/webrtc/start')
  return request({
    url: '/api/webrtc/start',
    method: 'post',
    data: {
      deviceId,
      rtspUrl
    }
  })
}

/**
 * WebRTC
 * @param {string} deviceId deviceID
 */
export function stopWebRTCPlay(deviceId) {
  return request({
    url: '/api/webrtc/stop',
    method: 'post',
    data: {
      deviceId
    }
  })
}

/**
 * Get WebRTC
 */
export function getActiveWebRTCStreams() {
  return request({
    url: '/api/webrtc/active',
    method: 'get'
  })
}

/**
 * deviceWebRTC
 * @param {string} deviceId deviceID
 */
export function checkWebRTCStreamStatus(deviceId) {
  return request({
    url: `/api/webrtc/check/${deviceId}`,
    method: 'get'
  })
}

/**
 * WebRTC
 */
export class WebRTCConnection {
  constructor(options = {}) {
    this.streamId = options.streamId || this.generateStreamId()
    this.rtspUrl = options.rtspUrl
    this.webrtcServerUrl = options.webrtcServerUrl || WEBRTC_SERVER_BASE_URL
    this.pc = null
    this.localStream = null
    this.remoteStream = null
    this.onTrack = options.onTrack || null
    this.onConnectionStateChange = options.onConnectionStateChange || null
    this.onError = options.onError || null
  }

  generateStreamId() {
    return 'stream_' + Math.random().toString(36).substr(2, 9)
  }

  async createPeerConnection() {
    const config = {
      iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' }
      ]
    }

    this.pc = new RTCPeerConnection(config)

    // Process
    this.pc.ontrack = (event) => {
      console.log('Received remote stream:', event.streams[0])
      this.remoteStream = event.streams[0]
      if (this.onTrack) {
        this.onTrack(event.streams[0])
      }
    }

    // Process
    this.pc.onconnectionstatechange = () => {
      console.log('WebRTC connection state:', this.pc.connectionState)
      if (this.onConnectionStateChange) {
        this.onConnectionStateChange(this.pc.connectionState)
      }
    }

    // Process ICE
    this.pc.onicecandidate = (event) => {
      if (event.candidate) {
        console.log('ICE candidate:', event.candidate)
        // ICE service
        this.sendICECandidate(event.candidate)
      }
    }
  }

  async connect() {
    try {
      await this.createPeerConnection()

      // offer
      const offer = await this.pc.createOffer()
      await this.pc.setLocalDescription(offer)

      // offer WebRTC-streamer
      const response = await createWebRTCOffer(this.streamId, offer)

      if (response.type === 'answer') {
        await this.pc.setRemoteDescription(new RTCSessionDescription(response))
      } else {
        throw new Error('Invalid response from WebRTC streamer')
      }

    } catch (error) {
      console.error('WebRTC connection failed:', error)
      if (this.onError) {
        this.onError(error)
      }
      throw error
    }
  }

  async sendICECandidate(candidate) {
    try {
      await sendICECandidate(this.streamId, candidate)
    } catch (error) {
      console.error('Failed to send ICE candidate:', error)
    }
  }

  disconnect() {
    if (this.pc) {
      this.pc.close()
      this.pc = null
    }
    this.remoteStream = null
  }

  getConnectionState() {
    return this.pc ? this.pc.connectionState : 'closed'
  }

  getStats() {
    return this.pc ? this.pc.getStats() : null
  }
}

/**
 * WebRTC workflow
 */
export class WebRTCPlayer {
  constructor(options = {}) {
    this.peerId = options.peerId || this.generatePeerId()
    this.rtspUrl = options.rtspUrl
    this.deviceId = options.deviceId
    this.username = options.username
    this.password = options.password
    this.options = options

    this.peerConnection = null
    this.localStream = null
    this.remoteStream = null
    this.iceServers = []
    this.channelToken = null
    this.systemConfig = null

    this.onTrack = options.onTrack || (() => {})
    this.onError = options.onError || (() => {})
    this.onConnectionStateChange = options.onConnectionStateChange || (() => {})

    this.isPlaying = false
    this.isConnecting = false
  }

  generatePeerId() {
    return Math.random().toString(36).substr(2, 15)
  }

  /**
   * Initialize workflow
   */
  async initialize() {
    try {
      this.isConnecting = true
      console.log('🚀 WebRTC初始化 - 版本v4-20250711-FINAL - API路径已修复')
      console.log('开始初始化WebRTC播放流程...')

      // configuration , Initialize
      console.log('WebRTC播放流程初始化完成（跳过配置检查）')
      return true
    } catch (error) {
      console.error('WebRTC播放流程初始化失败:', error)
      this.onError(error)
      return false
    } finally {
      this.isConnecting = false
    }
  }

  /**
   * Get WebRTCconfiguration
   */
  async getWebRTCConfig() {
    try {
      const response = await getWebRTCBackendConfig()

      if (response.code === 200) {
        this.webrtcConfig = response.data
        console.log('获取WebRTC配置成功:', this.webrtcConfig)
      } else {
        throw new Error(`获取WebRTC配置失败: ${response.message}`)
      }
    } catch (error) {
      console.error('获取WebRTC配置失败:', error)
      throw error
    }
  }

  /**
   * RTSP
   */
  async validateRtspUrl() {
    try {
      const response = await validateRtspStream(this.rtspUrl)

      if (response.code === 200) {
        console.log('RTSP流验证成功:', response.data)
        return true
      } else {
        throw new Error(`RTSP流验证失败: ${response.message}`)
      }
    } catch (error) {
      console.error('RTSP流验证失败:', error)
      throw error
    }
  }

  /**
   * PeerConnection ( )
   */
  async createPeerConnection() {
    try {
      const config = {
        iceServers: [
          { urls: 'stun:stun.l.google.com:19302' },
          { urls: 'stun:stun1.l.google.com:19302' }
        ]
      }

      this.peerConnection = new RTCPeerConnection(config)

      //
      this.peerConnection.ontrack = (event) => {
        console.log('收到远程流:', event.streams[0])
        this.remoteStream = event.streams[0]
        this.onTrack(event.streams[0])
      }

      //
      this.peerConnection.onconnectionstatechange = () => {
        const state = this.peerConnection.connectionState
        console.log('WebRTC连接状态变化:', state)
        this.onConnectionStateChange(state)

        if (state === 'connected') {
          this.isPlaying = true
        } else if (state === 'disconnected' || state === 'failed') {
          this.isPlaying = false
        }
      }

      console.log('PeerConnection创建成功')
    } catch (error) {
      console.error('创建PeerConnection失败:', error)
      throw error
    }
  }

  /**
   * start
   */
  async play() {
    console.log('🎬 WebRTCPlayer.play() 调用')
    try {
      // Initialize WebRTC workflow ( configuration )
      await this.initialize()

      console.log('开始WebRTC播放...')

      // after failed layer , MediaStream.
      const response = await startWebRTCPlay(this.deviceId, this.rtspUrl)

        if (response && response.code === 200) {
          const streamInfo = response.data
          console.log('WebRTC播放启动成功:', streamInfo)

          // only PeerConnection connected/ontrack can isPlaying to true.
          this.isPlaying = false
          return true
        } else {
          throw new Error(`WebRTC播放启动失败: ${response?.msg || response?.message || '未知错误'}`)
        }
    } catch (error) {
      console.error('WebRTC播放失败:', error)
      this.onError(error)
      return false
    }
  }

  /**
   *
   */
  async stop() {
    try {
      if (this.isPlaying) {
        await stopWebRTCPlay(this.deviceId)
      }

      if (this.peerConnection) {
        this.peerConnection.close()
        this.peerConnection = null
      }

      this.isPlaying = false
      this.remoteStream = null

      console.log('WebRTC播放已停止')
    } catch (error) {
      console.error('停止WebRTC播放失败:', error)
      this.onError(error)
    }
  }

  /**
   *
   */
  async checkStatus() {
    try {
      const response = await checkWebRTCStreamStatus(this.deviceId)
      if (response.code === 200) {
        return response.data
      } else {
        throw new Error(`检查播放状态失败: ${response.message}`)
      }
    } catch (error) {
      console.error('检查播放状态失败:', error)
      return null
    }
  }

  /**
   * Get
   */
  getPlayState() {
    return {
      isPlaying: this.isPlaying,
      isConnecting: this.isConnecting,
      connectionState: this.peerConnection?.connectionState,
      deviceId: this.deviceId
    }
  }
}

// Export
export default {
  getWebRTCConfig,
  getWebRTCServerStatus,
  getWebRTCStatus,
  refreshWebRTCConnection,
  startWebRTCServer,
  stopWebRTCServer,
  checkRtspStream,
  getStreamConfig,
  createWebRTCStream,
  destroyWebRTCStream,
  getActiveStreams,
  getStreamStats,
  callWebRTCStreamerAPI,
  createWebRTCOffer,
  handleWebRTCAnswer,
  sendICECandidate,
  getWebRTCStreamerInfo,
  getVideoDevices,
  getAudioDevices,
  addRTSPStream,
  removeRTSPStream,
  getStreamList,
  WebRTCConnection,
  getWebRTCBackendConfig,
  getWebRTCBackendStatus,
  validateRtspStream,
  startWebRTCPlay,
  stopWebRTCPlay,
  getActiveWebRTCStreams,
  checkWebRTCStreamStatus,
  WebRTCPlayer
}
