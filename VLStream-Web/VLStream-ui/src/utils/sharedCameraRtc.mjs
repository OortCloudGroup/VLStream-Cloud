const SIGNAL_PATH = '/vlstream/device/shared/camera-rtc'
export const SHARED_CAMERA_RTC_AUTH_HEADERS = Object.freeze([
  'accesstoken', 'X-VLStream-Token', 'X-WVP-Auth-Source', 'tenantid', 'requesttype',
  'appid', 'secretkey', 'Authorization', 'blade-auth',
])
const AUTH_HEADERS = new Map(SHARED_CAMERA_RTC_AUTH_HEADERS.map(name => [name.toLowerCase(), name]))
const IDENTITY_HEADERS = new Set(['accesstoken', 'x-vlstream-token', 'authorization', 'blade-auth'])
const CLIENT_FIELDS = {
  __connectto: [], __ping: [], __disconnected: [],
  __call: ['audio', 'video', 'datachannel', 'mode', 'source', 'iceservers'],
  __answer: ['type', 'sdp'], __ice_candidate: ['candidate'],
}

export function sharedCameraRtcProtocols(headers) {
  const invalid = () => new Error('共享信令认证信息无效，请重新登录')
  if (!headers || typeof headers !== 'object' || Array.isArray(headers) ||
    ![Object.prototype, null].includes(Object.getPrototypeOf(headers))) throw invalid()
  const safe = {}, seen = new Set()
  let hasIdentity = false
  for (const key of Reflect.ownKeys(headers)) {
    if (typeof key !== 'string') throw invalid()
    const lower = key.toLowerCase(), name = AUTH_HEADERS.get(lower), value = headers[key]
    if (!name || seen.has(lower) || typeof value !== 'string' || !value.trim() || /[\u0000-\u001f\u007f-\u009f]/.test(value)) throw invalid()
    seen.add(lower); safe[name] = value
    if (IDENTITY_HEADERS.has(lower)) hasIdentity = true
  }
  if (!hasIdentity) throw invalid()
  const bytes = new TextEncoder().encode(JSON.stringify(safe))
  if (bytes.length > 6144) throw invalid()
  const encoded = btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  if (encoded.length > 8192) throw invalid()
  return ['vlstream-shared-rtc.v1', `rtc-auth.${encoded}`]
}

export function sharedCameraRtcUrl(lease, wvpBase, origin) {
  const invalid = () => new Error('共享信令票据或同源代理配置无效')
  const page = new URL(origin), base = new URL(wvpBase, page)
  if (base.origin !== page.origin || base.username || base.password || base.search || base.hash) throw invalid()
  if (typeof lease?.webSocketPath !== 'string' || !lease.webSocketPath.startsWith('/') || lease.webSocketPath.startsWith('//')) throw invalid()
  const path = new URL(lease.webSocketPath, page)
  const ticket = path.searchParams.get('ticket')
  if (path.origin !== page.origin || path.pathname !== SIGNAL_PATH || path.hash || path.username || path.password ||
    [...path.searchParams.keys()].length !== 1 || !/^[A-Za-z0-9_-]{32,128}$/.test(ticket || '') || ticket !== lease.ticket ||
    !/^[a-f0-9]{64}$/i.test(lease.sessionId || '') || !(lease.ticketExpiresInSeconds > 0 && lease.ticketExpiresInSeconds <= 60)) throw invalid()
  const result = new URL(base.pathname.replace(/\/+$/, '') + path.pathname + path.search, page)
  result.protocol = page.protocol === 'https:' ? 'wss:' : 'ws:'
  if (!['http:', 'https:'].includes(page.protocol)) throw invalid()
  return result.href
}

const publicStun = value => {
  const match = /^stuns?:([^:/?#]+)(?::(\d{1,5}))?(?:\?transport=(?:udp|tcp))?$/i.exec(String(value || ''))
  if (!match) return false
  const host = match[1].toLowerCase().replace(/\.$/,'')
  if (!/^[a-z0-9][a-z0-9.-]*[a-z0-9]$/.test(host) || !host.includes('.') || /\.(?:local|localhost|internal|lan|home)$/.test(host) || (match[2] && (+match[2]<1 || +match[2]>65535))) return false
  if (/^[\d.]+$/.test(host)) {
    const parts=host.split('.').map(Number)
    if(parts.length!==4 || parts.some(value=>!Number.isInteger(value)||value<0||value>255) || host.split('.').some(value=>value.length>1&&value.startsWith('0')))return false
    if([0,10,127].includes(parts[0]) || parts[0]>=224 || (parts[0]===192&&parts[1]===168) || (parts[0]===172&&parts[1]>=16&&parts[1]<=31) || (parts[0]===169&&parts[1]===254) || (parts[0]===100&&parts[1]>=64&&parts[1]<=127))return false
  }
  return true
}
export function sharedIceConfiguration(raw) {
  let value
  try { value=typeof raw==='string'?JSON.parse(raw):raw } catch { value=null }
  const iceServers = (Array.isArray(value?.iceServers)?value.iceServers:[]).flatMap(server=>{
    if(!server || typeof server!=='object')return []
    const urls=(Array.isArray(server.urls)?server.urls:[server.urls]).filter(publicStun)
    return urls.length?[{urls}]:[]
  })
  return {iceServers,iceTransportPolicy:'all',iceCandidatePoolSize:0}
}

// Reuse media negotiation, but never the owner's /wswebclient URL construction,
// credential fields, TURN environment overrides, or reconnecting a consumed ticket.
export function createSharedCameraRtcSession({ CameraRtcSession, lease, wvpBase, origin, videoElement, getAuthHeaders, runtime=globalThis, onStatus=()=>{} }) {
  const relayUrl=sharedCameraRtcUrl(lease,wvpBase,origin)
  let opened=false
  class SharedSession extends CameraRtcSession {
    openSocket() {
      if(this.stopped)return
      if(opened){this.scheduleReconnect('共享信令票据已使用');return}
      opened=true;this.cleanupSocket();this.clientId='shared-viewer'
      let protocols
      try {
        if(typeof getAuthHeaders!=='function')throw new Error('missing auth provider')
        protocols=sharedCameraRtcProtocols(getAuthHeaders())
      } catch { this.scheduleReconnect('共享信令认证失败，请重新登录');return }
      this.setStatus('connecting','正在连接共享信令服务…')
      let socket
      try { socket=new this.runtime.WebSocket(relayUrl,protocols) } catch { this.scheduleReconnect('共享信令连接失败');return }
      this.socket=socket
      socket.onopen=()=>{if(this.stopped||socket!==this.socket)return;this.startHeartbeat();this.connectToDevice()}
      socket.onmessage=event=>{if(this.stopped||socket!==this.socket)return;return this.handleSocketMessage(event)}
      socket.onerror=()=>{if(socket===this.socket)this.scheduleReconnect('共享信令连接失败')}
      socket.onclose=()=>{if(socket!==this.socket)return;this.socket=null;this.scheduleReconnect('共享信令连接已关闭')}
    }
    send(eventName,data={}) {
      if(!Object.hasOwn(CLIENT_FIELDS,eventName))return false
      const safe={to:this.deviceId}
      for(const field of CLIENT_FIELDS[eventName])if(data[field]!==undefined)safe[field]=data[field]
      return super.send(eventName,safe)
    }
    handleCreate(data) {
      return super.handleCreate({state:data.state,...Object.fromEntries(['iceServers','domainnameiceServers','domainnameIceServers'].map(key=>[key,sharedIceConfiguration(data[key])]))})
    }
    scheduleReconnect(reason) {
      if(this.stopped)return
      this.stop();this.setStatus('failed',`${reason}，请重新打开共享预览`)
    }
    retryNow() { this.scheduleReconnect('共享信令需重新申请票据') }
  }
  return new SharedSession({videoElement,deviceId:lease.deviceRef,socketUrl:relayUrl,runtime,onStatus,
    extraTurnUrls:[],maxRetries:0,disconnectGraceMs:0,onDiagnostic:()=>{}})
}
