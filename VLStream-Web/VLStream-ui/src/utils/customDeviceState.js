// Saved/manual status is configuration. Evidence belongs to this exact CUSTOM source.
const trustedSources = ['CAMERA_RTC_TARGET_HEARTBEAT', 'CUSTOM_STREAM_PROBE', 'CUSTOM_MEDIA_RECEIVING']
export function customStatusExpiresAt(device) {
  const evidence = device?.availability
  if (!evidence || !trustedSources.includes(evidence.source)) return 0
  const remaining = Number(evidence.expiresAt) - Number(evidence.observedAt ?? evidence.checkedAt)
  return Number.isFinite(remaining) && remaining > 0 && Number.isFinite(device._statusReceivedAt)
    ? device._statusReceivedAt + remaining : 0
}
export function effectiveCustomStatus(device, now = Date.now()) {
  const evidence = device?.availability
  if (customStatusExpiresAt(device) <= now) return 'UNKNOWN'
  const state = String(evidence.state || '').toUpperCase()
  return state === 'ONLINE' || state === 'OFFLINE' ? state : 'UNKNOWN'
}

export function customStatusDetail(device, now = Date.now()) {
  const coarse = effectiveCustomStatus(device, now)
  if (coarse !== 'UNKNOWN') return coarse
  const evidence = device?.availability
  return customStatusExpiresAt(device) > now &&
    ['AUTH_ERROR', 'STREAM_ERROR'].includes(evidence?.detailState) ? evidence.detailState : 'UNKNOWN'
}

export const stampCustomStatus = (device, now = Date.now()) => ({ ...device, _statusReceivedAt: now })

export function nextCustomStatusExpiry(devices, now = Date.now()) {
  return devices.reduce((next, device) => (device.sources || [device]).reduce((expiry, source) => {
    const value = customStatusExpiresAt(source)
    return value > now ? Math.min(expiry, value) : expiry
  }, next), Infinity)
}
