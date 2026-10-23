const pngName = value => {
  const name = String(value || 'video-frame').replace(/\.png$/i, '').replace(/[<>:"/\\|?*\u0000-\u001f\u007f]/g, '_').replace(/[ .]+$/g, '').slice(0, 120)
  return `${name || 'video-frame'}.png`
}

export function canCaptureVideoFrame(video) {
  return Boolean(video && video.readyState >= 2 && Number.isFinite(video.videoWidth) && video.videoWidth > 0 &&
    Number.isFinite(video.videoHeight) && video.videoHeight > 0)
}

export function downloadVideoFrame(blob, fileName, { document: dom = globalThis.document, urlApi = globalThis.URL } = {}) {
  if (!dom?.createElement || !urlApi?.createObjectURL || !urlApi?.revokeObjectURL) throw new Error('当前环境无法下载截屏')
  const url = urlApi.createObjectURL(blob)
  let link
  try {
    link = dom.createElement('a')
    link.href = url
    link.download = fileName
    link.rel = 'noopener'
    if (link.style) link.style.display = 'none'
    dom.body?.appendChild(link)
    link.click()
  } finally {
    link?.remove()
    urlApi.revokeObjectURL(url)
  }
}

// Captures only a decoded frame already present in the selected video element.
// No source fetch, device request, microphone access or fabricated image fallback.
export async function captureVideoFrame(video, options = {}) {
  if (!canCaptureVideoFrame(video)) throw new Error('当前视频没有可截取的画面')
  const dom = options.document || globalThis.document
  const canvas = options.createCanvas ? options.createCanvas() : dom?.createElement?.('canvas')
  const width = video.videoWidth, height = video.videoHeight
  if (!canvas) throw new Error('当前环境无法截屏')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext?.('2d')
  if (!context?.drawImage || typeof canvas.toBlob !== 'function') throw new Error('当前环境无法截屏')
  context.drawImage(video, 0, 0, width, height)
  const blob = await new Promise((resolve, reject) => {
    canvas.toBlob(value => value?.size > 0 && value.type === 'image/png' ? resolve(value) : reject(new Error('无法生成视频截屏')), 'image/png')
  })
  const fileName = pngName(options.fileName)
  const download = options.download || ((image, name) => downloadVideoFrame(image, name, { document: dom, urlApi: options.urlApi || globalThis.URL }))
  await download(blob, fileName)
  return { blob, width, height, fileName }
}
