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
 * 根据设备ID获取时间策略
 * @param {string} deviceId - 设备ID
 * @returns {Promise}
 */
export const getTimeStrategy = (deviceId) => {
  return commonFunc(`/vlsTimeStrategy/${deviceId}`, {}, 'GET')
}

/**
 * 保存或更新时间策�?
 * @param {Object} timeStrategy - 时间策略对象
 * @returns {Promise}
 */
export const saveTimeStrategy = (timeStrategy) => {
  return commonFunc('/vlsTimeStrategy', timeStrategy)
}

/**
 * 根据设备ID删除时间策略
 * @param {string} deviceId - 设备ID
 * @returns {Promise}
 */
export const deleteTimeStrategy = (deviceId) => {
  return commonFunc(`/vlsTimeStrategy/${deviceId}`, {}, 'DELETE')
}
