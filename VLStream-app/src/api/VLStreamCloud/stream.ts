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
 * 启动HLS流转换
 * @param {Object} data - 转换请求数据
 * @param {string} data.deviceId - 设备ID
 * @param {string} data.rtspUrl - RTSP流地址
 * @param {string} data.quality - 转换质量 (low/medium/high)
 */
export function startHLSStream(data) {
  return commonFunc('/api/stream/start-hls', data)
}

/**
 * 停止HLS流转换
 * @param {Object} data - 停止请求数据
 * @param {string} data.deviceId - 设备ID
 */
export function stopHLSStream(data) {
  return commonFunc('/api/stream/stop-hls', data)
}

/**
 * 获取活跃的流信息
 */
export function getActiveStreams() {
  return commonFunc('/api/stream/active', {}, 'GET')
}

/**
 * 检查设备流是否活跃
 * @param {string} deviceId - 设备ID
 */
export function checkStreamStatus(deviceId) {
  return commonFunc(`/api/stream/check/${deviceId}`, {}, 'GET')
}

/**
 * 停止所有转换（管理员功能）
 */
export function stopAllStreams() {
  return commonFunc('/api/stream/stop-all', {})
}
