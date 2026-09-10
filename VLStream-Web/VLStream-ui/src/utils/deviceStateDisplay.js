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

// telemetry.bootTime: 设备实际启动时间，Unix 毫秒时间戳。
export function deviceBootTimeText(device) {
  try {
    const telemetry = typeof device?.telemetryJson === 'string' ? JSON.parse(device.telemetryJson) : device?.telemetryJson
    const bootTime = telemetry?.bootTime
    if (typeof bootTime === 'number' && Number.isFinite(bootTime) && bootTime > 0 && bootTime <= Date.now()) {
      const text = formatDeviceTime(bootTime)
      if (text !== '-') return text
    }
  } catch { /* 无有效启动时间时，尝试使用心跳快照估算。 */ }
  const rawIndex = device?.heartbeatIndex
  const index = typeof rawIndex === 'number' || typeof rawIndex === 'string' ? Number(rawIndex) : NaN
  const reportedAt = device?.lastReportedAt
  if (!Number.isSafeInteger(index) || index < 1 || reportedAt == null || reportedAt === '') return '未上报'
  const timestamp = new Date(typeof reportedAt === 'string' ? reportedAt.replace(' ', 'T') : reportedAt).getTime()
  if (!Number.isFinite(timestamp) || timestamp <= 0 || timestamp > Date.now()) return '未上报'
  // 当前设备实测心跳约 60 秒一次；此结果不是固件提供的真实启动时间。
  const estimated = timestamp - (index - 1) * 60000
  if (!Number.isFinite(estimated) || estimated <= 0) return '未上报'
  const text = formatDeviceTime(estimated)
  return text === '-' ? '未上报' : text.slice(0, 16)
}

export function deviceOnlineDurationText(device, now = Date.now()) {
  if (!device?.online) return '离线'
  const start = device.lastOnlineTime
  if (start == null || start === '') return '未上报'
  const timestamp = new Date(typeof start === 'string' ? start.replace(' ', 'T') : start).getTime()
  if (!Number.isFinite(timestamp) || timestamp > now) return '未上报'
  const seconds = Math.floor((now - timestamp) / 1000)
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor(seconds % 86400 / 3600)
  const minutes = Math.floor(seconds % 3600 / 60)
  return `${days ? `${days}天 ` : ''}${hours}小时 ${minutes}分 ${seconds % 60}秒`
}

export function deviceLocationText(device) {
  const longitude = device?.longitude
  const latitude = device?.latitude
  if (typeof longitude !== 'number' || typeof latitude !== 'number'
    || !Number.isFinite(longitude) || !Number.isFinite(latitude)
    || Math.abs(longitude) > 180 || Math.abs(latitude) > 90) return '未上报'
  return `经度 ${longitude}，纬度 ${latitude}（WGS84）`
}
