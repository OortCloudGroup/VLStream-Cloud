const OPLAYER_SCRIPT_URL = `${import.meta.env.BASE_URL}OPlayer.min.js`

let oplayerScriptLoader

function loadScript(src) {
  return new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[src="${src}"]`)
    if (existing) {
      if (window.OToolBox?.OPlayer) return resolve()
      existing.addEventListener('load', resolve, { once: true })
      existing.addEventListener('error', () => reject(new Error(`播放器脚本加载失败：${src}`)), { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = resolve
    script.onerror = () => reject(new Error(`播放器脚本加载失败：${src}`))
    document.head.appendChild(script)
  })
}

export async function ensureOPlayer() {
  if (window.OToolBox?.OPlayer) return
  if (!oplayerScriptLoader) {
    oplayerScriptLoader = loadScript(OPLAYER_SCRIPT_URL).catch(error => {
      oplayerScriptLoader = undefined
      throw error
    })
  }
  await oplayerScriptLoader
  if (!window.OToolBox?.OPlayer) throw new Error('播放器初始化失败')
}

export function getCustomStreamType(streamUrl) {
  if (!streamUrl) return 'unknown'
  const url = String(streamUrl).trim().toLowerCase()
  if (url.includes('/videocall/') || url.includes('/aety-')) return 'cameraRTC'
  if (url.startsWith('rtsp://')) return 'rtsp'
  if (url.startsWith('rtmp://')) return 'rtmp'
  if (/\.m3u8($|[?#])/i.test(url)) return 'hls'
  if (/\.flv($|[?#])/i.test(url)) return 'flv'
  if (/\.(mp4|avi|mov|wmv|mkv)($|[?#])/i.test(url)) return 'video'
  if (url.startsWith('http://') || url.startsWith('https://')) return 'http'
  return 'unknown'
}

export function createOPlayerOptions(streamUrl) {
  const streamType = getCustomStreamType(streamUrl)
  const playerConfig = { debuggerMode: false, autoSize: true, backgroundColor: '#000000', showHeader: true }

  if (streamType === 'cameraRTC') {
    const url = new URL(streamUrl)
    const cameraId = url.pathname.split('/').filter(Boolean).pop()
    if (!cameraId) throw new Error('CameraRTC 地址中缺少摄像头 ID')
    playerConfig.webRTCSocketURL = url.origin.replace(/^http/, 'ws')
    return { playerConfig, playConfig: { type: 'cameraRTC', src: cameraId } }
  }

  const playType = { flv: 'flv', hls: 'm3u8', video: 'mp4', http: 'mp4' }[streamType]
  if (!playType) throw new Error(`暂不支持该视频流类型：${streamType}`)
  return { playerConfig, playConfig: { type: playType, src: streamUrl } }
}
