const systems = { 1: 'WGS84', 2: 'GCJ02', 3: 'BD09' }

function validNumber(value, limit) {
  if (value == null || value === '' || typeof value === 'boolean') return null
  const number = Number(value)
  return Number.isFinite(number) && Math.abs(number) <= limit ? number : null
}

export function eventLocationCoordinate(event) {
  const location = event?.display_location, point = event?.point || {}
  if ((typeof point.address === 'string' && point.address.trim()) || (typeof location?.address === 'string' && location.address.trim())) return null
  const lng = validNumber(location ? location.longitude : point.lng, 180)
  const lat = validNumber(location ? location.latitude : point.lat, 90)
  const coordinateSystem = systems[location?.coordinate_system ?? point.coord_system_type]
  return lng == null || lat == null || !coordinateSystem ? null : { lng, lat, coordinateSystem }
}

export function eventLocationView(event) {
  const location = event?.display_location
  const point = event?.point || {}
  const address = typeof point.address === 'string' && point.address.trim()
    ? point.address.trim() : (typeof location?.address === 'string' ? location.address.trim() : '')
  return {
    text: address || '—',
    tooltip: address || '—'
  }
}
