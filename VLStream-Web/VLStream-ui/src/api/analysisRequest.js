import request from '@/utils/request'

// 读取真实分析请求表分页数据。
export function getAnalysisRequestPage(params) {
  return request({ url: '/vlsAnalysisRequest/list', method: 'get', params })
}

// 提交真实分析申请，后端仅在成功入库后返回成功。
export function applyAnalysisRequest(data) {
  return request({ url: '/vlsAnalysisRequest/apply', method: 'post', data })
}

// 更新已持久化的分析请求。
export function updateAnalysisRequest(data) {
  return request({ url: '/vlsAnalysisRequest/update', method: 'post', data })
}
