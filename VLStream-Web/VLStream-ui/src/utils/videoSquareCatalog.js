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
  return VIDEO_PROTOCOLS.map(protocol => ({
    ...protocol,
    id: protocol.value,
    children: devices.filter(device => device.protocols?.[0] === protocol.value),
  })).filter(group => group.children.length)
}

export async function loadVideoDeviceCatalog(fetchPage) {
  const devices = new Map()
  let expectedTotal = null
  let protocols = []
  let stats = null
  for (let pageNum = 1; ; pageNum += 1) {
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
      devices.set(device.id, { ...device, catalogSource: 'VIDEO_SQUARE' })
    }
    if (devices.size === expectedTotal) break
    if (!page.rows.length || devices.size > expectedTotal) throw new Error('设备目录分页不完整，请刷新')
  }
  return { devices: [...devices.values()], protocols, stats }
}
