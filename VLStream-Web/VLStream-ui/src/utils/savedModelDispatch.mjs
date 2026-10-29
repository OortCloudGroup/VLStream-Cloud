export const pairKey = (modelId, deviceId) => JSON.stringify([String(modelId), String(deviceId)])

export function attemptResult(data) {
  if (!data?.requestId || !data?.state) throw new Error('未收到完整下发任务结果，请核实原请求')
  const labels = {
    CREATED: '任务已创建，等待发送', PUBLISHED: '任务已提交，等待设备部署', RECEIVED: '设备已接收',
    DOWNLOADING: '设备正在下载', DOWNLOADED: '下载完成，等待部署', VERIFYING: '设备正在校验',
    DEPLOYING: '设备正在部署', SUCCESS: '设备部署成功', FAILED: '下发失败', UNKNOWN: '结果未确认，请核实原任务'
  }
  const state = data.state === 'SUCCESS' ? 'success' : data.state === 'FAILED' ? 'failed' : data.state === 'UNKNOWN' ? 'uncertain' : 'pending'
  return { ...data, state, taskState: data.state, retryable: data.state === 'FAILED' && data.retryable === true,
    message: data.message || labels[data.state] || labels.UNKNOWN }
}
