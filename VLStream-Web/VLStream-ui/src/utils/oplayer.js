const OPLAYER_SCRIPT_URL = import.meta.env.DEV
  ? '/bus/vls-ui/OPlayer.min.js'
  : 'http://oort.oortcloudsmart.com:21410/bus/vls-ui/OPlayer.min.js'

export const CAMERA_RTC_SOCKET_URL = 'ws://146.56.220.167:8082'

let oplayerScriptLoader = null

const loadScriptTag = (src) => {
  return new Promise((resolve, reject) => {
    if (typeof document === 'undefined') {
      reject(new Error('Document not available'))
      return
    }

    const existing = document.querySelector(`script[src="${src}"]`)
    if (existing) {
      if (window?.OToolBox?.OPlayer) {
        resolve()
        return
      }

      existing.addEventListener('load', () => resolve(), { once: true })
      existing.addEventListener('error', () => reject(new Error(`Failed to load ${src}`)), { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`Failed to load ${src}`))
    document.head.appendChild(script)
  })
}

export const ensureOPlayer = async () => {
  if (typeof window === 'undefined') {
    throw new Error('OPlayer requires browser environment')
  }

  if (window.OToolBox?.OPlayer) {
    return
  }

  if (!oplayerScriptLoader) {
    oplayerScriptLoader = loadScriptTag(OPLAYER_SCRIPT_URL).catch((error) => {
      oplayerScriptLoader = null
      throw error
    })
  }

  await oplayerScriptLoader

  if (!window.OToolBox?.OPlayer) {
    throw new Error('OPlayer library not available')
  }
}

export const isCameraRtcStream = (streamUrl) => {
  if (!streamUrl) return false
  const normalized = String(streamUrl).trim().toLowerCase()
  if (!normalized) return false
  return normalized.startsWith(CAMERA_RTC_SOCKET_URL)
}
