import request from '@/utils/request'

// Each HTTP request stays short. Long CPU/storage work is performed by the backend worker.
export async function generateTrainingDataset(id, isCancelled = () => false) {
  const base = `/vlsAlgorithmAnnotation/${String(id)}`
  let response = await request.post(`${base}/save-dataset-async`, undefined, { silentError: true })
  return waitForResult(base, response, isCancelled)
}

export async function viewTrainingDatasetReport(id, isCancelled = () => false) {
  const base = `/vlsAlgorithmAnnotation/${String(id)}`
  const response = await request.get(`${base}/dataset-generation-report`, { silentError: true })
  return waitForResult(base, response, isCancelled)
}

async function waitForResult(base, response, isCancelled) {
  while (['QUEUED', 'RUNNING'].includes(response.data?.status)) {
    if (isCancelled()) throw new Error('页面已关闭，后台操作继续执行')
    await new Promise(resolve => setTimeout(resolve, 2000))
    if (isCancelled()) throw new Error('页面已关闭，后台操作继续执行')
    response = await request.get(`${base}/dataset-generation/${response.data.jobId}`, { silentError: true })
  }
  if (response.data?.status === 'FAILED') throw new Error(response.data.message || '训练数据包生成失败')
  return response
}
