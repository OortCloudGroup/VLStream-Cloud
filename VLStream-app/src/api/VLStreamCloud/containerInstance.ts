import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

/**
 * 容器实例管理API接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */

/**
 * 分页查询容器实例
 * @param {Object} params 查询参数
 * @param {number} params.current 当前页
 * @param {number} params.size 每页大小
 * @param {string} params.name 实例名称（模糊查询）
 * @param {string} params.status 实例状态
 * @param {number} params.algorithmId 算法ID
 * @param {string} params.healthStatus 健康状态
 * @param {string} params.startTime 创建时间开始
 * @param {string} params.endTime 创建时间结束
 */
export function getContainerInstancePage(params) {
  return commonFunc('/vlsContainerInstance/page', params, 'GET')
}

/**
 * 获取容器实例详情
 * @param {number} id 实例ID
 */
export function getContainerInstanceById(id) {
  return commonFunc(`/vlsContainerInstance/${id}`, {}, 'GET')
}

/**
 * 创建容器实例
 * @param {Object} data 容器实例信息
 * @param {string} data.name 实例名称
 * @param {string} data.image 镜像地址
 * @param {number} data.algorithmId 算法ID
 * @param {string} data.cpuLimit CPU限制
 * @param {string} data.memoryLimit 内存限制
 * @param {string} data.gpuLimit GPU限制
 * @param {string} data.description 描述
 * @param {string} data.envVariables 环境变量JSON
 * @param {string} data.portMappings 端口映射JSON
 */
export function createContainerInstance(data) {
  return commonFunc('/vlsContainerInstance', data)
}

/**
 * 更新容器实例
 * @param {Object} data 容器实例信息
 * @param {number} data.id 实例ID
 * @param {string} data.name 实例名称
 * @param {string} data.description 描述
 * @param {string} data.envVariables 环境变量JSON
 * @param {string} data.portMappings 端口映射JSON
 */
export function updateContainerInstance(data) {
  return commonFunc('/vlsContainerInstance', data, 'PUT')
}

/**
 * 删除容器实例
 * @param {number} id 实例ID
 */
export function deleteContainerInstance(id) {
  return commonFunc(`/vlsContainerInstance/${id}`, {}, 'DELETE')
}

/**
 * 批量删除容器实例
 * @param {Array<number>} ids 实例ID数组
 */
export function batchDeleteContainerInstances(ids) {
  return commonFunc('/vlsContainerInstance/batch', { ids }, 'DELETE')
}

/**
 * 启动容器实例
 * @param {number} id 实例ID
 */
export function startContainerInstance(id) {
  return commonFunc(`/vlsContainerInstance/${id}/start`, {}, 'POST')
}

/**
 * 停止容器实例
 * @param {number} id 实例ID
 */
export function stopContainerInstance(id) {
  return commonFunc(`/vlsContainerInstance/${id}/stop`, {}, 'POST')
}

/**
 * 重启容器实例
 * @param {number} id 实例ID
 */
export function restartContainerInstance(id) {
  return commonFunc(`/vlsContainerInstance/${id}/restart`, {}, 'POST')
}

/**
 * 获取容器实例统计信息
 */
export function getContainerInstanceStatistics() {
  return commonFunc('/vlsContainerInstance/statistics', {}, 'GET')
}

/**
 * 获取运行中的容器实例列表
 */
export function getRunningContainerInstances() {
  return commonFunc('/vlsContainerInstance/running', {}, 'GET')
}

/**
 * 获取错误状态的容器实例列表
 */
export function getErrorContainerInstances() {
  return commonFunc('/vlsContainerInstance/error', {}, 'GET')
}

/**
 * 获取不健康的容器实例列表
 */
export function getUnhealthyContainerInstances() {
  return commonFunc('/vlsContainerInstance/unhealthy', {}, 'GET')
}

/**
 * 检查实例名称是否存在
 * @param {string} name 实例名称
 * @param {number} excludeId 排除的实例ID（编辑时使用）
 */
export function checkContainerInstanceName(name, excludeId = null) {
  return commonFunc('/vlsContainerInstance/check-name', { name, excludeId }, 'GET')
}

/**
 * 根据算法ID获取容器实例列表
 * @param {number} algorithmId 算法ID
 */
export function getContainerInstancesByAlgorithm(algorithmId) {
  return commonFunc(`/vlsContainerInstance/algorithm/${algorithmId}`, {}, 'GET')
}

/**
 * 更新容器实例监控数据
 * @param {number} id 实例ID
 * @param {Object} data 监控数据
 * @param {number} data.cpuUsage CPU使用率
 * @param {number} data.memoryUsage 内存使用率
 * @param {number} data.gpuUsage GPU使用率
 */
export function updateContainerInstanceMonitoring(id, data) {
  return commonFunc(`/vlsContainerInstance/${id}/monitoring`, data, 'PUT')
}
