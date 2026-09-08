export function parseSnapshot(value) {
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return Array.isArray(parsed) ? parsed : null
  } catch { return null }
}

export function capabilityText(value) {
  const capabilities = parseSnapshot(value)
  if (!capabilities) return '未上报'
  const labels = { video: '视频', audio: '音频', ptz: '云台', aiInfer: 'AI 推理', face: '人脸识别', recording: '录像', ota: '固件升级' }
  return capabilities.length ? capabilities.map(item => labels[item] || item).join('、') : '无'
}

export function formatDeviceTime(value) {
  if (value == null || value === '') return '-'
  const date = new Date(typeof value === 'string' ? value.replace(' ', 'T') : value)
  if (Number.isNaN(date.getTime())) return '-'
  const pad = number => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function modelStatusText(status) {
  return ({ loaded: '已加载', running: '运行中', stopped: '已停止', failed: '异常' })[status] || status || '未上报'
}
