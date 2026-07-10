import request from '@/utils/request'

// 设备管理API接口

/**
 * 获取设备列表（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.current - 当前页码
 * @param {number} params.size - 每页大小
 * @param {string} params.keyword - 搜索关键字（设备名称或设备ID�? * @param {string} params.status - 设备状�? * @param {string} params.tag - 标签
 */
export function getDeviceList(params) {
  console.log('API调用 getDeviceList，参数', params)
  return request({
    url: '/vlsDeviceInfo/page',
    method: 'get',
    params
  })
}

/**
 * 获取设备详情
 * @param {number} id - 设备ID
 */
export function getDeviceById(id) {
  return request({
    url: `/vlsDeviceInfo/${id}`,
    method: 'get'
  })
}

/**
 * 创建设备
 * @param {Object} data - 设备信息
 */
export function createDevice(data) {
  return request({
    url: '/vlsDeviceInfo',
    method: 'post',
    data
  })
}

/**
 * 更新设备
 * @param {number} id - 设备ID
 * @param {Object} data - 设备信息
 */
export function updateDevice(id, data) {
  return request({
    url: `/vlsDeviceInfo/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除设备
 * @param {number} id - 设备ID
 */
export function deleteDevice(id) {
  return request({
    url: `/vlsDeviceInfo/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除设备
 * @param {Array} ids - 设备ID数组
 */
export function batchDeleteDevices(ids) {
  return request({
    url: '/vlsDeviceInfo/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 获取设备统计信息
 */
export function getDeviceStatistics() {
  return request({
    url: '/vlsDeviceInfo/statistics',
    method: 'get'
  })
}

/**
 * 获取设备树结�? */
export function getDeviceTree() {
  return request({
    url: '/vlsDeviceInfo/tree',
    method: 'get'
  })
}

/**
 * 测试设备连接
 * @param {number} id - 设备ID
 */
export function testDeviceConnection(id) {
  return request({
    url: `/vlsDeviceInfo/${id}/test`,
    method: 'post'
  })
}

/**
 * 刷新设备状�? * @param {number} id - 设备ID
 */
export function refreshDeviceStatus(id) {
  return request({
    url: `/vlsDeviceInfo/${id}/refresh`,
    method: 'post'
  })
}

/**
 * 批量刷新设备状�? * @param {Array} ids - 设备ID数组
 */
export function batchRefreshDevices(ids) {
  return request({
    url: '/vlsDeviceInfo/batch/refresh',
    method: 'post',
    data: { ids }
  })
}

/**
 * 获取设备类型统计
 */
export function getDeviceTypeStatistics() {
  return request({
    url: '/vlsDeviceInfo/type-statistics',
    method: 'get'
  })
}

/**
 * 获取所有标签列�? */
export function getDeviceTags() {
  return request({
    url: '/vlsDeviceInfo/tags',
    method: 'get'
  })
}

/**
 * PTZ控制 - 上下左右移动
 * @param {number} id - 设备ID
 * @param {string} direction - 方向：up, down, left, right
 * @param {number} speed - 速度�?-8�? */
export function ptzMove(id, direction, speed = 4) {
  return request({
    url: `/vlsDeviceInfo/${id}/ptz/move`,
    method: 'post',
    data: { direction, speed }
  })
}

/**
 * PTZ控制 - 停止移动
 * @param {number} id - 设备ID
 */
export function ptzStop(id) {
  return request({
    url: `/vlsDeviceInfo/${id}/ptz/stop`,
    method: 'post'
  })
}

/**
 * PTZ控制 - 缩放
 * @param {number} id - 设备ID
 * @param {string} action - 动作：zoom_in, zoom_out
 * @param {number} speed - 速度�?-8�? */
export function ptzZoom(id, action, speed = 4) {
  return request({
    url: `/vlsDeviceInfo/${id}/ptz/zoom`,
    method: 'post',
    data: { action, speed }
  })
}

/**
 * 获取设备视频流信�? * @param {number} id - 设备ID
 */
export function getDeviceStreamInfo(id) {
  return request({
    url: `/vlsDeviceInfo/${id}/stream`,
    method: 'get'
  })
}

/**
 * 导出设备列表
 * @param {Object} params - 查询参数
 */
export function exportDevices(params) {
  return request({
    url: '/vlsDeviceInfo/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * 导入设备列表
 * @param {FormData} formData - 包含文件的表单数�? */
export function importDevices(formData) {
  return request({
    url: '/vlsDeviceInfo/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 摄像头算法下发（算法ID + 设备ID逗号串）
 * @param {number} algorithmId 算法ID
 * @param {string} deviceIdsStr 设备ID逗号分隔字符串
 */
export function dispatchAlgorithmToDevices(algorithmId, deviceIdsStr) {
  return request({
    url: `/vlsDeviceInfo/${algorithmId}/algorithms`,
    method: 'post',
    params: { deviceIds: deviceIdsStr }
  })
}
