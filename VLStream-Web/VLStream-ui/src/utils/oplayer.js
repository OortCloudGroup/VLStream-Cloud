/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

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

/** Parse the CameraRTC camera ID and WebSocket signaling endpoint from its HTTP URL. */
export const parseCameraRtcConfig = (streamUrl) => {
  const url = new URL(String(streamUrl || '').trim())
  if (!['http:', 'https:'].includes(url.protocol)) {
    throw new Error('CameraRTC requires an HTTP(S) URL')
  }
  const segments = url.pathname.split('/').filter(Boolean)
  const markerIndex = segments.findIndex(segment => segment.toLowerCase() === 'videocall')
  const cameraId = markerIndex >= 0 ? segments[markerIndex + 1] : ''
  if (!cameraId) throw new Error('CameraRTC URL is missing camera ID')
  return {
    cameraId: decodeURIComponent(cameraId),
    socketUrl: url.origin.replace(/^http/, 'ws')
  }
}

export const isCameraRtcStream = (streamUrl) => {
  try {
    parseCameraRtcConfig(streamUrl)
    return true
  } catch {
    return false
  }
}
