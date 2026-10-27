export const MAX_BATCH_DEVICES = 20
export const modelDeploymentImpact = '部署会切换所选摄像机当前推理模型和配套类别。平台不发送其他模型的删除命令，不修改独立的阈值、区域或调度参数；其他模型能否保留或同时运行取决于固件。'
export const deploymentStatus = item => {
  const labels = {
    QUEUED: '尚未发送', CHECKING: '正在校验', CREATED: '已创建，等待发布', PENDING: '已受理，等待设备',
    PUBLISHED: '已发布，等待设备', RECEIVED: '设备已接收', DOWNLOADING: '下载中', DOWNLOADED: '传输完成，等待部署',
    VERIFYING: '校验中', DEPLOYING: '部署中', SUCCESS: '设备部署成功', FAILED: '失败', UNKNOWN: '结果未确认'
  }
  return labels[item?.state] || '结果未确认'
}
export const deploymentTag = item => item?.state === 'SUCCESS' ? 'success' : item?.state === 'FAILED' ? 'danger' : item?.state === 'UNKNOWN' ? 'warning' : 'info'
export const retryableDeviceIds = batch => (batch?.items || []).filter(item => item.retryable === true).map(item => item.deviceId)
export const batchSelectionError = devices => {
  if (!devices?.length || devices.length > MAX_BATCH_DEVICES) return '每批请选择1至20台摄像机'
  const ids = devices.map(device => String(device.deviceId || ''))
  if (ids.some(id => !id) || new Set(ids).size !== ids.length) return '设备业务编号缺失或重复，请重新加载设备目录'
  return ''
}
