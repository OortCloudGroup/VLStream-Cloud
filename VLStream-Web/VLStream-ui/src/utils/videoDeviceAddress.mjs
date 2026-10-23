export function savedVideoDeviceAddress(device = {}) {
  return [device.address, device.addressMap]
    .find(value => typeof value === 'string' && value.trim())?.trim() || ''
}

export function createVideoDeviceLocationPopup(markerData, resolveAddress, translate) {
  const element = document.createElement('div')
  element.className = 'video-device-location'
  const name = document.createElement('div')
  name.textContent = markerData.name
  const address = document.createElement('div')
  const saved = savedVideoDeviceAddress(markerData.deviceData)
  address.textContent = saved || translate('地址解析中...')
  address.title = saved
  const coordinates = document.createElement('div')
  coordinates.textContent = markerData.lng.toFixed(6) + ', ' + markerData.lat.toFixed(6)
  coordinates.hidden = true
  element.append(name, address, coordinates)
  return {
    element,
    async resolve(isCurrent) {
      const result = await resolveAddress(markerData.deviceData)
      if (!isCurrent()) return false
      address.textContent = result.address || translate(result.status === 'missing' ? '未上报位置' : '地址解析失败，显示经纬度')
      address.title = result.address
      coordinates.hidden = result.status !== 'failed'
      return true
    },
  }
}

// Per-page cache: no storage or writes to device data, shared pending requests,
// and a short failure cooldown to avoid repeated calls on popup reopen/refresh.
export function createVideoAddressResolver(reverseGeocode, timeoutMs = 15000) {
  const cache = new Map()
  return device => {
    const saved = savedVideoDeviceAddress(device)
    if (saved) return Promise.resolve({ status: 'saved', address: saved })
    const raw = [device.longitude, device.latitude]
    if (raw.some(value => value == null || !['string', 'number'].includes(typeof value) || String(value).trim() === '')) {
      return Promise.resolve({ status: 'missing', address: '' })
    }
    const [lng, lat] = raw.map(Number)
    if (!Number.isFinite(lng) || !Number.isFinite(lat) || Math.abs(lng) > 180 || Math.abs(lat) > 90) {
      return Promise.resolve({ status: 'missing', address: '' })
    }
    const key = `${lng},${lat}`
    const entry = cache.get(key)
    if (entry && entry.expires > Date.now()) return entry.promise
    let timer
    const next = { expires: Infinity }
    next.promise = Promise.race([
      Promise.resolve().then(() => reverseGeocode({ lng, lat })),
      new Promise((_, reject) => { timer = setTimeout(() => reject(new Error('timeout')), timeoutMs) }),
    ]).then(address => {
      if (typeof address !== 'string' || !address.trim()) throw new Error('empty address')
      return { status: 'resolved', address: address.trim() }
    }).catch(() => {
      next.expires = Date.now() + 60000
      return { status: 'failed', address: '' }
    }).finally(() => clearTimeout(timer))
    // Bound the cache for large catalogs. Pending work still stays isolated to its popup.
    if (cache.size >= 500) cache.delete(cache.keys().next().value)
    cache.set(key, next)
    return next.promise
  }
}
