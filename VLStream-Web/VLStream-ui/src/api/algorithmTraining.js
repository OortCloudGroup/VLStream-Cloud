import request from '@/utils/request'

// 分页查询训练任务
export function getTrainingPage(params) {
  return request({
    url: '/vlsAlgorithmTraining/page',
    method: 'get',
    params: params
  })
}

// 查询训练任务详细
export function getTrainingTask(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id,
    method: 'get'
  })
}

// 新增训练任务
export function addTrainingTask(data) {
  return request({
    url: '/vlsAlgorithmTraining',
    method: 'post',
    data: data
  })
}

// 修改训练任务
export function updateTrainingTask(data) {
  return request({
    url: '/vlsAlgorithmTraining/' + data.id,
    method: 'put',
    data: data
  })
}

// 删除训练任务
export function delTrainingTask(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id,
    method: 'delete'
  })
}

// 开始训练任务
export function startTraining(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/start',
    method: 'post'
  })
}

// 开始训练任务（带参数）
export function startTrainingWithParams(id, params) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/start',
    method: 'post',
    params: params
  })
}

// 停止训练任务
export function stopTraining(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/stop',
    method: 'post'
  })
}

// 获取训练日志
export function getTrainingLogs(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/logs',
    method: 'get'
  })
}

// 获取训练状态
export function getTrainingStatus(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/status',
    method: 'get'
  })
}

// 批量删除训练任务
export function batchDeleteTraining(ids) {
  return request({
    url: '/vlsAlgorithmTraining/batch',
    method: 'delete',
    data: ids
  })
}

// 创建训练任务
export function createTraining(data) {
  return request({
    url: '/vlsAlgorithmTraining',
    method: 'post',
    data: data
  })
}

// 更新训练任务
export function updateTraining(data) {
  return request({
    url: '/vlsAlgorithmTraining/' + data.id,
    method: 'put',
    data: data
  })
}

// 删除训练任务
export function deleteTraining(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id,
    method: 'delete'
  })
}

// 转换模型
export function convertModel(id) {
  return request({
    url: '/vlsAlgorithmTraining/' + id + '/convert-model',
    method: 'post'
  })
}