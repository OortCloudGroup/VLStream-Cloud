import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

// 算法模型API服务

/**
 * 分页查询算法模型
 * @param {Object} params 查询参数
 */
export function getModelPage(params) {
  return commonFunc('/vlsAlgorithmModel/page', params, 'GET')
}

/**
 * 根据ID查询算法模型详情
 * @param {Number} id 模型ID
 */
export function getModelById(id) {
  return commonFunc(`/vlsAlgorithmModel/${id}`, {}, 'GET')
}

/**
 * 创建算法模型
 * @param {Object} data 模型数据
 */
export function createModel(data) {
  return commonFunc('/vlsAlgorithmModel/create', data)
}

/**
 * 更新算法模型
 * @param {Object} data 模型数据
 */
export function updateModel(data) {
  return commonFunc('/vlsAlgorithmModel/update', data)
}

/**
 * 删除算法模型
 * @param {Number} id 模型ID
 */
export function deleteModel(id) {
  return commonFunc(`/vlsAlgorithmModel/${id}`, {}, 'DELETE')
}

/**
 * 批量删除算法模型
 * @param {Array} ids 模型ID列表
 */
export function batchDeleteModel(ids) {
  return commonFunc('/vlsAlgorithmModel/batch', ids, 'DELETE')
}

/**
 * 根据算法ID查询模型列表
 * @param {Number} algorithmId 算法ID
 */
export function getModelsByAlgorithmId(algorithmId) {
  return commonFunc(`/vlsAlgorithmModel/algorithm/${algorithmId}`, {}, 'GET')
}

/**
 * 根据训练任务ID查询模型列表
 * @param {Number} trainingId 训练任务ID
 */
export function getModelsByTrainingId(trainingId) {
  return commonFunc(`/vlsAlgorithmModel/training/${trainingId}`, {}, 'GET')
}

/**
 * 根据状态查询模型列表
 * @param {String} status 状态
 */
export function getModelsByStatus(status) {
  return commonFunc(`/vlsAlgorithmModel/status/${status}`, {}, 'GET')
}

/**
 * 发布模型
 * @param {Number} id 模型ID
 */
export function publishModel(id) {
  return commonFunc(`/vlsAlgorithmModel/publish/${id}`, {}, 'POST')
}

/**
 * 撤销发布模型
 * @param {Number} id 模型ID
 */
export function unpublishModel(id) {
  return commonFunc(`/vlsAlgorithmModel/unpublish/${id}`, {}, 'POST')
}

/**
 * 批量发布模型
 * @param {Array} ids 模型ID列表
 */
export function batchPublishModel(ids) {
  return commonFunc('/vlsAlgorithmModel/batch-publish', ids, 'POST')
}

/**
 * 下载模型
 * @param {Number} id 模型ID
 */
export function downloadModel(id) {
  return commonFunc(`/vlsAlgorithmModel/download/${id}`, {}, 'GET')
}

/**
 * 部署模型
 * @param {Number} id 模型ID
 */
export function deployModel(id) {
  return commonFunc(`/vlsAlgorithmModel/deploy/${id}`, {}, 'POST')
}

/**
 * 获取模型统计信息
 */
export function getModelStatistics() {
  return commonFunc('/vlsAlgorithmModel/statistics', {}, 'GET')
}

/**
 * 检查模型名称和版本是否存在
 * @param {String} modelName 模型名称
 * @param {String} version 模型版本
 * @param {Number} excludeId 排除的ID
 */
export function checkModelNameAndVersion(modelName, version, excludeId) {
  return commonFunc(
    '/vlsAlgorithmModel/check-name-version',
    { modelName, version, excludeId },
    'GET'
  )
}

/**
 * 根据算法ID和版本查询模型
 * @param {Number} algorithmId 算法ID
 * @param {String} version 版本
 */
export function getModelByAlgorithmIdAndVersion(algorithmId, version) {
  return commonFunc(
    `/vlsAlgorithmModel/algorithm/${algorithmId}/version/${version}`,
    {},
    'GET'
  )
}

/**
 * 获取算法下最新版本的模型
 * @param {Number} algorithmId 算法ID
 */
export function getLatestModelByAlgorithmId(algorithmId) {
  return commonFunc(`/vlsAlgorithmModel/algorithm/${algorithmId}/latest`, {}, 'GET')
}

/**
 * 查询热门模型
 * @param {Number} limit 限制数量
 */
export function getPopularModels(limit = 10) {
  return commonFunc('/vlsAlgorithmModel/popular', { limit }, 'GET')
}

/**
 * 根据创建人查询模型数量
 * @param {Number} createdBy 创建人ID
 */
export function countModelsByCreatedBy(createdBy) {
  return commonFunc(`/vlsAlgorithmModel/count/creator/${createdBy}`, {}, 'GET')
}

/**
 * 获取算法模型的总大小
 */
export function getTotalModelSize() {
  return commonFunc('/vlsAlgorithmModel/total-size', {}, 'GET')
}
