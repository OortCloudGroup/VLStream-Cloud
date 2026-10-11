const UNKNOWN_REASON = '无法确认云台能力，请检查设备网络、账号或协议兼容性后重新检查'

export function normalizePtzCapabilities(data, fallbackProfile = '') {
  const status = ['supported', 'unsupported', 'unknown'].includes(data?.status) ? data.status : 'unknown'
  return {
    status,
    reason: typeof data?.reason === 'string' && data.reason.trim()
      ? data.reason
      : status === 'unsupported' ? '该设备不支持云台控制' : status === 'unknown' ? UNKNOWN_REASON : '',
    continuousPanTilt: status === 'supported' && data?.continuousPanTilt === true,
    presets: status === 'supported' && data?.presets === true,
    profileToken: typeof data?.profileToken === 'string' && data.profileToken.trim()
      ? data.profileToken
      : typeof fallbackProfile === 'string' ? fallbackProfile : ''
  }
}

export function canUsePtzMovement(capabilities) {
  return capabilities?.status === 'supported' && capabilities.continuousPanTilt === true
}

export function canUsePtzPresets(capabilities) {
  return capabilities?.status === 'supported' && capabilities.presets === true
}

// A selection and its checks have separate revisions: reopening the same device
// or retrying a check cannot let an earlier response enable a newer dialog.
export function createPtzCapabilitiesController({ request, onChange = () => {} }) {
  let device = null
  let selectionRevision = 0
  let requestRevision = 0
  let state = normalizePtzCapabilities(null)
  const publish = next => {
    state = next
    onChange(next)
    return next
  }
  const session = () => selectionRevision
  const isCurrent = selection => device !== null && selection === selectionRevision
  const checking = () => publish({
    status: 'checking',
    reason: '正在检查云台能力，请稍候',
    continuousPanTilt: false,
    presets: false,
    profileToken: device?.channel || ''
  })
  const open = row => {
    selectionRevision += 1
    requestRevision += 1
    device = { id: row.id, channel: row.channel }
    checking()
    return session()
  }
  const check = async () => {
    if (!device) return undefined
    const selected = session()
    const revision = ++requestRevision
    const selectedDevice = device
    checking()
    let next
    try {
      const response = await request({ id: selectedDevice.id, refresh: true })
      next = normalizePtzCapabilities(response?.data, selectedDevice.channel)
    } catch (error) {
      next = normalizePtzCapabilities({ status: 'unknown', reason: error?.message || UNKNOWN_REASON }, selectedDevice.channel)
    }
    if (!isCurrent(selected) || revision !== requestRevision) return undefined
    return publish(next)
  }
  const markUnknown = reason => {
    if (!device) return
    requestRevision += 1
    publish(normalizePtzCapabilities({ status: 'unknown', reason }, device.channel))
  }
  const close = () => {
    selectionRevision += 1
    requestRevision += 1
    device = null
    publish(normalizePtzCapabilities(null))
  }
  return { open, check, close, markUnknown, session, isCurrent, snapshot: () => state }
}
