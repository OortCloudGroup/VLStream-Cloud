import request from '@/utils/request'

/**
 * 启动HLS流转换
 * @param {Object} data - 转换请求数据
 * @param {string} data.deviceId - 设备ID
 * @param {string} data.rtspUrl - RTSP流地址
 * @param {string} data.quality - 转换质量 (low/medium/high)
 */
export function startHLSStream(data) {
  return request({
    url: '/api/stream/start-hls',
    method: 'post',
    data
  })
}

/**
 * 停止HLS流转换
 * @param {Object} data - 停止请求数据
 * @param {string} data.deviceId - 设备ID
 */
export function stopHLSStream(data) {
  return request({
    url: '/api/stream/stop-hls',
    method: 'post',
    data
  })
}

/**
 * 获取活跃的流信息
 */
export function getActiveStreams() {
  return request({
    url: '/api/stream/active',
    method: 'get'
  })
}

/**
 * 检查设备流是否活跃
 * @param {string} deviceId - 设备ID
 */
export function checkStreamStatus(deviceId) {
  return request({
    url: `/api/stream/check/${deviceId}`,
    method: 'get'
  })
}

/**
 * 停止所有转换（管理员功能）
 */
export function stopAllStreams() {
  return request({
    url: '/api/stream/stop-all',
    method: 'post'
  })
} 