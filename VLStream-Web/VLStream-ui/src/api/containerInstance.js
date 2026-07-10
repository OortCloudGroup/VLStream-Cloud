import request from '@/utils/request'

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
  return request({
    url: '/vlsContainerInstance/page',
    method: 'get',
    params
  })
}

/**
 * 获取容器实例详情
 * @param {number} id 实例ID
 */
export function getContainerInstanceById(id) {
  return request({
    url: `/vlsContainerInstance/${id}`,
    method: 'get'
  })
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
  return request({
    url: '/vlsContainerInstance',
    method: 'post',
    data
  })
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
  return request({
    url: '/vlsContainerInstance',
    method: 'put',
    data
  })
}

/**
 * 删除容器实例
 * @param {number} id 实例ID
 */
export function deleteContainerInstance(id) {
  return request({
    url: `/vlsContainerInstance/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除容器实例
 * @param {Array<number>} ids 实例ID数组
 */
export function batchDeleteContainerInstances(ids) {
  return request({
    url: '/vlsContainerInstance/batch',
    method: 'delete',
    data: { ids }
  })
}

/**
 * 启动容器实例
 * @param {number} id 实例ID
 */
export function startContainerInstance(id) {
  return request({
    url: `/vlsContainerInstance/${id}/start`,
    method: 'post'
  })
}

/**
 * 停止容器实例
 * @param {number} id 实例ID
 */
export function stopContainerInstance(id) {
  return request({
    url: `/vlsContainerInstance/${id}/stop`,
    method: 'post'
  })
}

/**
 * 重启容器实例
 * @param {number} id 实例ID
 */
export function restartContainerInstance(id) {
  return request({
    url: `/vlsContainerInstance/${id}/restart`,
    method: 'post'
  })
}

/**
 * 获取容器实例统计信息
 */
export function getContainerInstanceStatistics() {
  return request({
    url: '/vlsContainerInstance/statistics',
    method: 'get'
  })
}

/**
 * 获取运行中的容器实例列表
 */
export function getRunningContainerInstances() {
  return request({
    url: '/vlsContainerInstance/running',
    method: 'get'
  })
}

/**
 * 获取错误状态的容器实例列表
 */
export function getErrorContainerInstances() {
  return request({
    url: '/vlsContainerInstance/error',
    method: 'get'
  })
}

/**
 * 获取不健康的容器实例列表
 */
export function getUnhealthyContainerInstances() {
  return request({
    url: '/vlsContainerInstance/unhealthy',
    method: 'get'
  })
}

/**
 * 检查实例名称是否存在
 * @param {string} name 实例名称
 * @param {number} excludeId 排除的实例ID（编辑时使用）
 */
export function checkContainerInstanceName(name, excludeId = null) {
  return request({
    url: '/vlsContainerInstance/check-name',
    method: 'get',
    params: { name, excludeId }
  })
}

/**
 * 根据算法ID获取容器实例列表
 * @param {number} algorithmId 算法ID
 */
export function getContainerInstancesByAlgorithm(algorithmId) {
  return request({
    url: `/vlsContainerInstance/algorithm/${algorithmId}`,
    method: 'get'
  })
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
  return request({
    url: `/vlsContainerInstance/${id}/monitoring`,
    method: 'put',
    data
  })
} 