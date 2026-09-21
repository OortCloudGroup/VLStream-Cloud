import { currentLocale, translatePhrase } from '@/i18n'

export function parseSnapshot(value) {
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return Array.isArray(parsed) ? parsed : null
  } catch { return null }
}

export function capabilityText(value) {
  const capabilities = parseSnapshot(value)
  if (!capabilities) return translatePhrase('未上报')
  const labels = { video: '视频', audio: '音频', ptz: '云台', aiInfer: 'AI 推理', face: '人脸识别', recording: '录像', ota: '固件升级' }
  const translated = capabilities.map(item => translatePhrase(labels[item] || item))
  return translated.length ? new Intl.ListFormat(currentLocale.value, { style: 'short', type: 'conjunction' }).format(translated) : translatePhrase('无')
}

export function formatDeviceTime(value, { includeSeconds = true } = {}) {
  if (value == null || value === '') return '-'
  const date = new Date(typeof value === 'string' ? value.replace(' ', 'T') : value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat(currentLocale.value, {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: includeSeconds ? '2-digit' : undefined,
    hourCycle: 'h23'
  }).format(date)
}

export function modelStatusText(status) {
  return translatePhrase(({ loaded: '已加载', running: '运行中', stopped: '已停止', failed: '异常' })[status] || status || '未上报')
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
  if (!Number.isSafeInteger(index) || index < 1 || reportedAt == null || reportedAt === '') return translatePhrase('未上报')
  const timestamp = new Date(typeof reportedAt === 'string' ? reportedAt.replace(' ', 'T') : reportedAt).getTime()
  if (!Number.isFinite(timestamp) || timestamp <= 0 || timestamp > Date.now()) return translatePhrase('未上报')
  // 当前设备实测心跳约 60 秒一次；此结果不是固件提供的真实启动时间。
  const estimated = timestamp - (index - 1) * 60000
  if (!Number.isFinite(estimated) || estimated <= 0) return translatePhrase('未上报')
  const text = formatDeviceTime(estimated, { includeSeconds: false })
  return text === '-' ? translatePhrase('未上报') : text
}

export function deviceOnlineDurationText(device, now = Date.now()) {
  if (!device?.online) return translatePhrase('离线')
  const start = device.lastOnlineTime
  if (start == null || start === '') return translatePhrase('未上报')
  const timestamp = new Date(typeof start === 'string' ? start.replace(' ', 'T') : start).getTime()
  if (!Number.isFinite(timestamp) || timestamp > now) return translatePhrase('未上报')
  const seconds = Math.floor((now - timestamp) / 1000)
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor(seconds % 86400 / 3600)
  const minutes = Math.floor(seconds % 3600 / 60)
  return [
    days ? `${days} ${translatePhrase('天')}` : '',
    `${hours} ${translatePhrase('小时')}`,
    `${minutes} ${translatePhrase('分')}`,
    `${seconds % 60} ${translatePhrase('秒')}`
  ].filter(Boolean).join(' ')
}

export function deviceLocationText(device) {
  const longitude = device?.longitude
  const latitude = device?.latitude
  if (typeof longitude !== 'number' || typeof latitude !== 'number'
    || !Number.isFinite(longitude) || !Number.isFinite(latitude)
    || Math.abs(longitude) > 180 || Math.abs(latitude) > 90) return translatePhrase('未上报')
  const numberFormat = new Intl.NumberFormat(currentLocale.value, { maximumFractionDigits: 8 })
  return `${translatePhrase('经度')} ${numberFormat.format(longitude)}, ${translatePhrase('纬度')} ${numberFormat.format(latitude)} (WGS84)`
}
