import request from '@/utils/request'

/**
 * 根据设备ID获取时间策略
 * @param {string} deviceId - 设备ID
 * @returns {Promise}
 */
export const getTimeStrategy = (deviceId) => {
  return request({
    url: `/vlsTimeStrategy/${deviceId}`,
    method: 'get'
  })
}

/**
 * 保存或更新时间策�?
 * @param {Object} timeStrategy - 时间策略对象
 * @returns {Promise}
 */
export const saveTimeStrategy = (timeStrategy) => {
  return request({
    url: '/vlsTimeStrategy',
    method: 'post',
    data: timeStrategy
  })
}

/**
 * 根据设备ID删除时间策略
 * @param {string} deviceId - 设备ID
 * @returns {Promise}
 */
export const deleteTimeStrategy = (deviceId) => {
  return request({
    url: `/vlsTimeStrategy/${deviceId}`,
    method: 'delete'
  })
} 
