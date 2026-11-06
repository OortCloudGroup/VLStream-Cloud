// Photon provides a public, moderate-use OSM search API; deployments can replace it.
const provider = import.meta.env.VITE_OSM_GEOCODING_PROVIDER || 'photon'
const endpoint = (import.meta.env.VITE_OSM_GEOCODING_URL ?? 'https://photon.komoot.io').replace(/\/$/, '')
export const hasGeocoding = Boolean(endpoint)
let queue = Promise.resolve(), lastRequest = 0
const cache = new Map()
function request(path, params) {
  if (!endpoint) return Promise.reject(new Error('地址搜索服务未配置'))
  const defaults = provider === 'nominatim' ? { format: 'jsonv2', 'accept-language': 'zh-CN' } : {}
  const url = `${endpoint}/${path}?${new URLSearchParams({ ...defaults, ...params })}`
  if (cache.has(url)) return Promise.resolve(cache.get(url))
  const job = queue.then(async () => {
    if (cache.has(url)) return cache.get(url)
    await new Promise(resolve => setTimeout(resolve, Math.max(0, 1100 - (Date.now() - lastRequest))))
    lastRequest = Date.now()
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), 10000)
    try {
      const response = await fetch(url, { signal: controller.signal, credentials: 'omit' })
      if (!response.ok) throw new Error('地址查询失败')
      const data = await response.json()
      if (data.error) throw new Error('未找到地址')
      if (cache.size >= 200) cache.delete(cache.keys().next().value)
      cache.set(url, data)
      return data
    } finally { clearTimeout(timer) }
  })
  queue = job.catch(() => {})
  return job
}
function photonResults(data) {
  return (data.features || []).filter(feature => feature.geometry?.type === 'Point').map(feature => {
    const p = feature.properties || {}
    return { lon: feature.geometry.coordinates[0], lat: feature.geometry.coordinates[1],
      display_name: [...new Set([p.name, [p.street, p.housenumber].filter(Boolean).join(' '), p.district, p.city, p.state, p.country].filter(Boolean))].join(', ') }
  })
}
export async function searchAddress(query) {
  const data = await request(provider === 'photon' ? 'api/' : 'search', { q: query, limit: '5' })
  return provider === 'photon' ? photonResults(data) : data
}
export async function reverseGeocode({ lng, lat, coordinateSystem = 'WGS84' }) {
  // Never send unconverted historical GCJ02/BD09 coordinates to a WGS84 service.
  if (coordinateSystem !== 'WGS84') throw new Error('地址解析需要 WGS84 坐标')
  if (!Number.isFinite(Number(lng)) || !Number.isFinite(Number(lat)) || lng == null || lat == null ||
      String(lng).trim() === '' || String(lat).trim() === '' || Math.abs(Number(lng)) > 180 || Math.abs(Number(lat)) > 90) throw new Error('坐标无效')
  const data = await request(provider === 'photon' ? 'reverse/' : 'reverse', { lon: String(lng), lat: String(lat) })
  const result = provider === 'photon' ? photonResults(data)[0] : data
  if (!result?.display_name) throw new Error('未找到地址')
  return result.display_name
}
