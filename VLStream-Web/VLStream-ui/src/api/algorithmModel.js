import request from '@/utils/request'

// 算法模型API服务

/**
 * 分页查询算法模型
 * @param {Object} params 查询参数
 */
export function getModelPage(params) {
  return request({
    url: '/vlsAlgorithmModel/page',
    method: 'get',
    data: params
  })
}

/**
 * 根据ID查询算法模型详情
 * @param {Number} id 模型ID
 */
export function getModelById(id) {
  return request({
    url: `/vlsAlgorithmModel/${id}`,
    method: 'get'
  })
}

/**
 * 创建算法模型
 * @param {Object} data 模型数据
 */
export function createModel(data) {
  return request({
    url: '/vlsAlgorithmModel/create',
    method: 'post',
    data
  })
}

/**
 * 更新算法模型
 * @param {Object} data 模型数据
 */
export function updateModel(data) {
  return request({
    url: '/vlsAlgorithmModel/update',
    method: 'post',
    data
  })
}

/**
 * 删除算法模型
 * @param {Number} id 模型ID
 */
export function deleteModel(id) {
  return request({
    url: `/vlsAlgorithmModel/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除算法模型
 * @param {Array} ids 模型ID列表
 */
export function batchDeleteModel(ids) {
  return request({
    url: '/vlsAlgorithmModel/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 根据算法ID查询模型列表
 * @param {Number} algorithmId 算法ID
 */
export function getModelsByAlgorithmId(algorithmId) {
  return request({
    url: `/vlsAlgorithmModel/algorithm/${algorithmId}`,
    method: 'get'
  })
}

/**
 * 根据训练任务ID查询模型列表
 * @param {Number} trainingId 训练任务ID
 */
export function getModelsByTrainingId(trainingId) {
  return request({
    url: `/vlsAlgorithmModel/training/${trainingId}`,
    method: 'get'
  })
}

/**
 * 根据状态查询模型列表
 * @param {String} status 状态
 */
export function getModelsByStatus(status) {
  return request({
    url: `/vlsAlgorithmModel/status/${status}`,
    method: 'get'
  })
}

/**
 * 发布模型
 * @param {Number} id 模型ID
 */
export function publishModel(id) {
  return request({
    url: `/vlsAlgorithmModel/publish/${id}`,
    method: 'post'
  })
}

/**
 * 撤销发布模型
 * @param {Number} id 模型ID
 */
export function unpublishModel(id) {
  return request({
    url: `/vlsAlgorithmModel/unpublish/${id}`,
    method: 'post'
  })
}

/**
 * 批量发布模型
 * @param {Array} ids 模型ID列表
 */
export function batchPublishModel(ids) {
  return request({
    url: '/vlsAlgorithmModel/batch-publish',
    method: 'post',
    data: ids
  })
}

/**
 * 下载模型
 * @param {Number} id 模型ID
 */
export function downloadModel(id) {
  return request({
    url: `/vlsAlgorithmModel/download/${id}`,
    method: 'get'
  })
}

/**
 * 部署模型
 * @param {Number} id 模型ID
 */
export function deployModel(id) {
  return request({
    url: `/vlsAlgorithmModel/deploy/${id}`,
    method: 'post'
  })
}

/**
 * 获取模型统计信息
 */
export function getModelStatistics() {
  return request({
    url: '/vlsAlgorithmModel/statistics',
    method: 'get'
  })
}

/**
 * 检查模型名称和版本是否存在
 * @param {String} modelName 模型名称
 * @param {String} version 模型版本
 * @param {Number} excludeId 排除的ID
 */
export function checkModelNameAndVersion(modelName, version, excludeId) {
  return request({
    url: '/vlsAlgorithmModel/check-name-version',
    method: 'get',
    params: {
      modelName,
      version,
      excludeId
    }
  })
}

/**
 * 根据算法ID和版本查询模型
 * @param {Number} algorithmId 算法ID
 * @param {String} version 版本
 */
export function getModelByAlgorithmIdAndVersion(algorithmId, version) {
  return request({
    url: `/vlsAlgorithmModel/algorithm/${algorithmId}/version/${version}`,
    method: 'get'
  })
}

/**
 * 获取算法下最新版本的模型
 * @param {Number} algorithmId 算法ID
 */
export function getLatestModelByAlgorithmId(algorithmId) {
  return request({
    url: `/vlsAlgorithmModel/algorithm/${algorithmId}/latest`,
    method: 'get'
  })
}

/**
 * 查询热门模型
 * @param {Number} limit 限制数量
 */
export function getPopularModels(limit = 10) {
  return request({
    url: '/vlsAlgorithmModel/popular',
    method: 'get',
    params: { limit }
  })
}

/**
 * 根据创建人查询模型数量
 * @param {Number} createdBy 创建人ID
 */
export function countModelsByCreatedBy(createdBy) {
  return request({
    url: `/vlsAlgorithmModel/count/creator/${createdBy}`,
    method: 'get'
  })
}

/**
 * 获取算法模型的总大小
 */
export function getTotalModelSize() {
  return request({
    url: '/vlsAlgorithmModel/total-size',
    method: 'get'
  })
} 