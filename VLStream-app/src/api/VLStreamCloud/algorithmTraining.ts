import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

// 分页查询训练任务
export function getTrainingPage(params) {
  return commonFunc('/vlsAlgorithmTraining/page', params, 'GET')
}

// 查询训练任务详细
export function getTrainingTask(id) {
  return commonFunc(`/vlsAlgorithmTraining/${id}`, {}, 'GET')
}

// 新增训练任务
export function addTrainingTask(data) {
  return commonFunc('/vlsAlgorithmTraining', data)
}

// 修改训练任务
export function updateTrainingTask(data) {
  return commonFunc(`/vlsAlgorithmTraining/${data.id}`, data, 'PUT')
}

// 删除训练任务
export function delTrainingTask(id) {
  return commonFunc(`/vlsAlgorithmTraining/${id}`, {}, 'DELETE')
}

// 开始训练任务
export function startTraining(id) {
  return commonFunc(`/vlsAlgorithmTraining/${id}/start`, {}, 'POST')
}

// 开始训练任务（带参数）
export function startTrainingWithParams(id, params) {
  return commonFunc(`/vlsAlgorithmTraining/${id}/start`, params, 'POST')
}

// 停止训练任务
export function stopTraining(id) {
  return commonFunc(`/vlsAlgorithmTraining/${id}/stop`, {}, 'POST')
}

// 获取训练日志
export function getTrainingLogs(id) {
  return commonFunc(`/vlsAlgorithmTraining/${id}/logs`, {}, 'GET')
}

// 获取训练状态
export function getTrainingStatus(id) {
  return commonFunc(`/vlsAlgorithmTraining/${id}/status`, {}, 'GET')
}

// 批量删除训练任务
export function batchDeleteTraining(ids) {
  return commonFunc('/vlsAlgorithmTraining/batch', ids, 'DELETE')
}

// 创建训练任务
export function createTraining(data) {
  return commonFunc('/vlsAlgorithmTraining', data)
}

// 更新训练任务
export function updateTraining(data) {
  return commonFunc(`/vlsAlgorithmTraining/${data.id}`, data, 'PUT')
}

// 删除训练任务
export function deleteTraining(id) {
  return commonFunc(`/vlsAlgorithmTraining/${id}`, {}, 'DELETE')
}

// 转换模型
export function convertModel(id) {
  return commonFunc(`/vlsAlgorithmTraining/${id}/convert-model`, {}, 'POST')
}
