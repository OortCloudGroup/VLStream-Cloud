import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

// ========== 算法仓库管理 API ==========

/**
 * 分页查询算法仓库列表
 * @param {Object} params 查询参数
 * @param {number} params.current 当前页
 * @param {number} params.size 每页大小
 * @param {string} params.name 仓库名称（模糊查询）
 * @param {string} params.repositoryType 仓库类型
 * @param {string} params.status 状态
 */
export function getAlgorithmRepositoryPage(params) {
  return commonFunc('/vlsAlgorithmRepository/page', params, 'GET')
}

/**
 * 查询所有启用的算法仓库
 */
export function getEnabledAlgorithmRepositories() {
  return commonFunc('/vlsAlgorithmRepository/enabled', {}, 'GET')
}

/**
 * 根据类型查询算法仓库
 * @param {string} repositoryType 仓库类型
 */
export function getAlgorithmRepositoriesByType(repositoryType) {
  return commonFunc(`/vlsAlgorithmRepository/type/${repositoryType}`, {}, 'GET')
}

/**
 * 根据ID查询算法仓库详情
 * @param {number} id 仓库ID
 */
export function getAlgorithmRepositoryById(id) {
  return commonFunc(`/vlsAlgorithmRepository/${id}`, {}, 'GET')
}

/**
 * 创建算法仓库
 * @param {Object} data 仓库数据
 * @param {string} data.name 仓库名称
 * @param {string} data.remark 备注说明
 * @param {string} data.repositoryType 仓库类型
 * @param {string} data.status 状态
 */
export function createAlgorithmRepository(data) {
  return commonFunc('/vlsAlgorithmRepository', data)
}

/**
 * 更新算法仓库
 * @param {number} id 仓库ID
 * @param {Object} data 更新数据
 */
export function updateAlgorithmRepository(id, data) {
  return commonFunc(`/vlsAlgorithmRepository/${id}`, data, 'PUT')
}

/**
 * 删除算法仓库
 * @param {number} id 仓库ID
 */
export function deleteAlgorithmRepository(id) {
  return commonFunc(`/vlsAlgorithmRepository/${id}`, {}, 'DELETE')
}

/**
 * 批量删除算法仓库
 * @param {number[]} ids 仓库ID列表
 */
export function batchDeleteAlgorithmRepositories(ids) {
  return commonFunc('/vlsAlgorithmRepository/batch', ids, 'DELETE')
}

/**
 * 更新仓库状态
 * @param {number} id 仓库ID
 * @param {string} status 新状态
 */
export function updateAlgorithmRepositoryStatus(id, status) {
  return commonFunc(`/vlsAlgorithmRepository/${id}/status`, { status }, 'PUT')
}

/**
 * 批量更新仓库状态
 * @param {number[]} ids 仓库ID列表
 * @param {string} status 新状态
 */
export function batchUpdateAlgorithmRepositoryStatus(ids, status) {
  return commonFunc('/vlsAlgorithmRepository/batch/status', { ids, status }, 'PUT')
}

/**
 * 统计算法仓库数量
 */
export function countAlgorithmRepositories() {
  return commonFunc('/vlsAlgorithmRepository/count', {}, 'GET')
}

/**
 * 刷新仓库算法数量
 * @param {number} id 仓库ID
 */
export function refreshAlgorithmCount(id) {
  return commonFunc(`/vlsAlgorithmRepository/${id}/refresh-count`, {}, 'PUT')
}

// ========== 算法管理 API ==========

/**
 * 分页查询算法列表
 * @param {Object} params 查询参数
 * @param {number} params.current 当前页
 * @param {number} params.size 每页大小
 * @param {number} params.repositoryId 仓库ID
 * @param {string} params.name 算法名称（模糊查询）
 * @param {string} params.category 算法分类
 * @param {string} params.type 算法类型
 * @param {string} params.deployStatus 部署状态
 */
export function getAlgorithmPage(params) {
  return commonFunc('/vlsAlgorithm/page', params, 'GET')
}

/**
 * 根据仓库ID查询算法列表
 * @param {number} repositoryId 仓库ID
 */
export function getAlgorithmsByRepositoryId(repositoryId) {
  return commonFunc(`/vlsAlgorithm/repository/${repositoryId}`, {}, 'GET')
}

/**
 * 根据分类查询算法列表
 * @param {string} category 算法分类
 */
export function getAlgorithmsByCategory(category) {
  return commonFunc(`/vlsAlgorithm/category/${category}`, {}, 'GET')
}

/**
 * 根据ID查询算法详情
 * @param {number} id 算法ID
 */
export function getAlgorithmById(id) {
  return commonFunc(`/vlsAlgorithm/${id}`, {}, 'GET')
}

/**
 * 创建算法
 * @param {Object} data 算法数据
 * @param {number} data.repositoryId 所属仓库ID
 * @param {string} data.name 算法名称
 * @param {string} data.category 算法分类
 * @param {string} data.type 算法类型
 * @param {string} data.description 算法描述
 * @param {string} data.version 算法版本
 * @param {string} data.inputFormat 输入格式
 * @param {string} data.outputFormat 输出格式
 * @param {number} data.gpuRequired 是否需要GPU
 */
export function createAlgorithm(data) {
  return commonFunc('/vlsAlgorithm', data)
}

/**
 * 更新算法
 * @param {number} id 算法ID
 * @param {Object} data 更新数据
 */
export function updateAlgorithm(id, data) {
  return commonFunc(`/vlsAlgorithm/${id}`, data, 'PUT')
}

/**
 * 删除算法
 * @param {number} id 算法ID
 */
export function deleteAlgorithm(id) {
  return commonFunc(`/vlsAlgorithm/${id}`, {}, 'DELETE')
}

/**
 * 批量删除算法
 * @param {number[]} ids 算法ID列表
 */
export function batchDeleteAlgorithms(ids) {
  return commonFunc('/vlsAlgorithm/batch', ids, 'DELETE')
}

/**
 * 更新部署状态
 * @param {number} id 算法ID
 * @param {string} deployStatus 部署状态
 */
export function updateAlgorithmDeployStatus(id, deployStatus) {
  return commonFunc(`/vlsAlgorithm/${id}/deploy-status`, { deployStatus }, 'PUT')
}

/**
 * 批量更新部署状态
 * @param {number[]} ids 算法ID列表
 * @param {string} deployStatus 部署状态
 */
export function batchUpdateAlgorithmDeployStatus(ids, deployStatus) {
  return commonFunc('/vlsAlgorithm/batch/deploy-status', { ids, deployStatus }, 'PUT')
}

/**
 * 部署算法到设备
 * @param {number} algorithmId 算法ID
 * @param {number[]} deviceIds 设备ID列表
 */
export function deployAlgorithmToDevices(algorithmId, deviceIds) {
  return commonFunc(`/vlsAlgorithm/${algorithmId}/deploy`, deviceIds, 'POST')
}

/**
 * 算法评估
 * @param {number} algorithmId 算法ID
 * @param {Object} testData 测试数据（可选）
 */
export function evaluateAlgorithm(algorithmId) {
  return commonFunc(`/vlsAlgorithm/${algorithmId}/evaluate`, {}, 'POST')
}

/**
 * 获取算法分类统计
 */
export function getAlgorithmCategoryStatistics() {
  return commonFunc('/vlsAlgorithm/statistics/category', {}, 'GET')
}

/**
 * 获取算法类型统计
 */
export function getAlgorithmTypeStatistics() {
  return commonFunc('/vlsAlgorithm/statistics/type', {}, 'GET')
}

/**
 * 获取部署状态统计
 */
export function getAlgorithmDeployStatusStatistics() {
  return commonFunc('/vlsAlgorithm/statistics/deploy-status', {}, 'GET')
}

/**
 * 统计某仓库下的算法数量
 * @param {number} repositoryId 仓库ID
 */
export function countAlgorithmsByRepositoryId(repositoryId) {
  return commonFunc(`/vlsAlgorithm/count/repository/${repositoryId}`, {}, 'GET')
}
