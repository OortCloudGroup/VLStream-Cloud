import { effectiveCustomStatus, stampCustomStatus } from './customDeviceState'

export const VIDEO_PROTOCOLS = [
  ['VLSTREAM', 'VLStream', '/vlstream/device'],
  ['ISUP', 'ISUP', '/isup/isupDevice'],
  ['EHOME', 'EHome', '/ehome/device'],
  ['RTSP', 'RTSP', '/rtsp/rtspDevice'],
  ['ONVIF', 'ONVIF', '/onvif/cameraManage'],
  ['GB28181', '国标', '/gbmanger/device'],
  ['DAHUA', '大华', '/dahua/dahuaDevice'],
  ['CUSTOM', '自定义', '/custom/device'],
].map(([value, label, path]) => ({ value, label, path }))

export function videoDeviceCoordinates(device) {
  if (device.latitude == null || device.longitude == null ||
      String(device.latitude).trim() === '' || String(device.longitude).trim() === '') return null
  const lat = Number(device.latitude)
  const lng = Number(device.longitude)
  return Number.isFinite(lat) && Number.isFinite(lng) && Math.abs(lat) <= 90 && Math.abs(lng) <= 180
    ? { lat, lng } : null
}

export function matchesVideoDevice(device, keyword = '') {
  const search = keyword.trim().toLowerCase()
  return !search || [device.deviceName, device.deviceId, device.ipAddress,
    ...(device.sources || []).flatMap(source => [source.deviceId, source.serial])]
    .some(value => String(value || '').toLowerCase().includes(search))
}

export function videoDeviceStats(devices) {
  return {
    total: devices.length,
    online: devices.filter(device => device.status === 'ONLINE').length,
    offline: devices.filter(device => device.status === 'OFFLINE').length,
    unknown: devices.filter(device => device.status === 'UNKNOWN').length,
    located: devices.filter(videoDeviceCoordinates).length,
  }
}

export function videoDeviceGroups(devices) {
  // Multi-protocol devices appear once. Keep every source on the device itself.
  return ['ONLINE', 'UNKNOWN', 'OFFLINE'].flatMap(status => VIDEO_PROTOCOLS.map(protocol => ({
    ...protocol,
    id: status + ':' + protocol.value,
    status,
    children: sortVideoDevices(devices.filter(device => device.protocols?.[0] === protocol.value && device.status === status)),
  })).filter(group => group.children.length)
    .sort((a, b) => compareId(String(a.children[0].id), String(b.children[0].id))))
}

const compareId = (a, b) => a < b ? -1 : a > b ? 1 : 0
const statusRank = value => value === 'ONLINE' ? 0 : value === 'OFFLINE' ? 2 : 1

export function sortVideoDevices(devices) {
  return [...devices].sort((a, b) => statusRank(a.status) - statusRank(b.status) || compareId(String(a.id), String(b.id)))
}

export const videoSourceKey = source => source.protocol + ':' + source.rowId

export const videoSourceOnline = source =>
  source?.protocol === 'CUSTOM' ? effectiveCustomStatus(source) === 'ONLINE' :
    ['ONLINE', 'ON', '1', 'TRUE'].includes(String(source?.state).trim().toUpperCase())

export const canPreviewVideoSource = source =>
  (source?.protocol === 'VLSTREAM' && videoSourceOnline(source)) ||
  (['RTSP', 'ONVIF'].includes(source?.protocol) && (!source.playType || source.playType === '1')) ||
  (source?.protocol === 'ONVIF' && ['2', '3'].includes(source.playType)) ||
  (source?.protocol === 'CUSTOM' && effectiveCustomStatus(source) !== 'OFFLINE')

export function videoSourceState(source, now = Date.now()) {
  if (source.protocol === 'CUSTOM') return effectiveCustomStatus(source, now)
  if (videoSourceOnline(source)) return 'ONLINE'
  return ['OFFLINE', 'OFF', '0', 'FALSE'].includes(String(source.state).trim().toUpperCase()) ? 'OFFLINE' : 'UNKNOWN'
}

const aggregateStates = states => states.includes('ONLINE') ? 'ONLINE' :
  states.length && states.every(state => state === 'OFFLINE') ? 'OFFLINE' : 'UNKNOWN'

export function normalizeVideoDevice(device, now = Date.now()) {
  const sources = (device.sources || []).map(source => ({ ...source, state: videoSourceState(source, now) }))
  return { ...device, sources, status: sources.length ? aggregateStates(sources.map(source => source.state)) :
    device.protocols?.includes('CUSTOM') ? 'UNKNOWN' : device.status }
}

// A protocol filter describes the selected sources, not another source's presence.
export function videoDeviceForProtocols(device, protocols = [], now = Date.now()) {
  const normalized = normalizeVideoDevice(device, now)
  if (!protocols.length) return normalized
  const sources = normalized.sources.filter(source => protocols.includes(source.protocol))
  if (!sources.length) return null
  return { ...normalized, status: aggregateStates(sources.map(source => source.state)),
    protocols: [...new Set(sources.map(source => source.protocol))], preferredProtocols: protocols }
}

export function defaultVideoSource(sources = []) {
  const rank = source => canPreviewVideoSource(source) ? 0 : 1
  const state = source => videoSourceOnline(source) ? 0 : 1
  const protocolRank = source => source.protocol === 'VLSTREAM' ? 0 : 1
  return [...(sources || [])].sort((a, b) => rank(a) - rank(b) || state(a) - state(b) ||
    protocolRank(a) - protocolRank(b) || compareId(videoSourceKey(a), videoSourceKey(b)))[0]
}

export async function loadVideoDeviceCatalog(fetchPage) {
  const devices = new Map()
  let expectedTotal = null
  let protocols = []
  let stats = null
  for (let pageNum = 1; ; pageNum += 1) {
    const requestedAt = Date.now()
    const result = await fetchPage({ pageNum, pageSize: 100 })
    const page = result?.data
    if (!Array.isArray(page?.rows) || !Number.isSafeInteger(page.total) || page.total < 0) {
      throw new Error('设备目录响应无效')
    }
    if (expectedTotal !== null && page.total !== expectedTotal) throw new Error('设备目录已变化，请刷新')
    expectedTotal = page.total
    protocols = page.protocols || []
    stats = page.stats
    for (const device of page.rows) {
      if (!device.id || devices.has(device.id)) throw new Error('设备目录已变化，请刷新')
      devices.set(device.id, normalizeVideoDevice({ ...device,
        sources: (device.sources || []).map(source => stampCustomStatus(source, requestedAt)), catalogSource: 'VIDEO_SQUARE' }))
    }
    if (devices.size === expectedTotal) break
    if (!page.rows.length || devices.size > expectedTotal) throw new Error('设备目录分页不完整，请刷新')
  }
  return { devices: sortVideoDevices([...devices.values()]), protocols, stats }
}
