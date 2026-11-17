export const hasDeviceManifest = model => /^[a-f0-9]{40}$/.test(model?.published_commit || '') && model?.device_manifest_path === 'vls-device-manifest.json'
export function hubDeploymentPayload(model, algorithmId, devices, operationId) {
  if (!hasDeviceManifest(model)) throw new Error('模型缺少固定版本或设备部署契约')
  if (!algorithmId) throw new Error('请选择本租户目标检测算法')
  const deviceIds = [...new Set(devices.filter(item => item?.deviceId).map(item => String(item.deviceId)))]
  if (!deviceIds.length || deviceIds.length > 20) throw new Error('请选择 1 至 20 台设备')
  if (devices.some(item => !item.online)) throw new Error('请选择在线设备')
  if (!operationId) throw new Error('下发操作编号无效')
  return { uid: model.uid, commit: model.published_commit, algorithmId: String(algorithmId), deviceIds, operationId }
}
export function checkedHubDeploymentResults(data, deviceIds) {
  if (!Array.isArray(data) || data.length !== deviceIds.length || new Set(data.map(item => String(item.deviceId))).size !== deviceIds.length || data.some(item => !deviceIds.includes(String(item.deviceId)) || !item.requestId || !item.status)) {
    throw new Error('未收到完整下发结果，请核实原任务，不要新建重复下发')
  }
  return data
}

// Keep operation IDs across drawer/tab unmounts in this page, never across identities.
export function createHubAttemptStore() {
  const values = new Map()
  let owner = null
  function bind(identity) {
    try { owner?.assertCurrent() } catch { values.clear() }
    identity.assertCurrent()
    owner = identity
  }
  return {
    get(key, identity) { bind(identity); return values.get(key) },
    set(key, value, identity) { bind(identity); values.set(key, value) }
  }
}
export const hubAttemptStore = createHubAttemptStore()
