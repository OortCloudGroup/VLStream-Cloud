export function squareCameraRtcConfig(cameraId, socketUrl) {
  if (typeof cameraId !== 'string' || !/^[A-Za-z0-9_-]{1,100}$/.test(cameraId)) {
    throw new Error('CameraRTC 设备标识无效')
  }
  return { cameraId, socketUrl }
}

// Parse the raw path before URL normalisation can hide dot segments or encoded separators.
export function cameraRtcIdFromUrl(streamUrl) {
  const input = String(streamUrl || '').trim()
  const raw = /^https?:\/\/([^/?#]+)([^?#]*)/i.exec(input)
  if (!raw || /[@\s]/.test(raw[1]) || /[\\\s]/.test(raw[2]) || /[\\\s#]/.test(input)) throw new Error('CameraRTC URL is invalid')
  const url = new URL(input)
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.hash) {
    throw new Error('CameraRTC requires an HTTP(S) URL')
  }
  const segments = raw[2].split('/').filter(Boolean)
  if (segments.some(segment => {
    const decoded = decodeURIComponent(segment)
    return decoded === '.' || decoded === '..' || /[\\/%\ufffd]/.test(decoded) ||
      (segment.toLowerCase() !== decoded.toLowerCase() && decoded.toLowerCase() === 'videocall')
  })) throw new Error('CameraRTC URL is invalid')
  const markers = segments.map((segment, index) => segment.toLowerCase() === 'videocall' ? index : -1).filter(index => index >= 0)
  const cameraId = markers.length === 1 ? segments[markers[0] + 1] : ''
  if (!/^[A-Za-z0-9_-]{1,100}$/.test(cameraId || '')) throw new Error('CameraRTC URL is missing a valid camera ID')
  return cameraId
}
