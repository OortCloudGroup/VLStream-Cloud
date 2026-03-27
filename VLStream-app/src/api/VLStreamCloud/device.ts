/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:38:14
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-01-23 14:35:59
* @Copyright aPaaS-front-team. All rights reserved.
*/
// import { request } from '@/utils/service'
// import config from '@/config'
// function commonFunc<T, K>(interfaceName: string, data: T, method = 'post') {
//   return request<K>({
//     url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
//     method: method,
//     data: data
//   })
// }

// export function getDeviceList(data) {
//   return commonFunc('/vlsDeviceInfo/page', data, 'GET')
// }

import { request } from '@/utils/service'
import config from '@/config'

// 公共请求函数：统一URL拼接、请求方式、入参处理，泛型支持类型约束
function commonFunc<T, K>(interfaceName: string, data: T, method = 'post', isParams = false) {
  let params = method === 'get' || method === 'delete' || method === 'GET' || method === 'DELETE' ? { params: data } : { data: data }
  if (isParams) {
    params = { params: data }
  }
  return request < K >({
    url: config.URL + config.gateWay + 'vls-server' + interfaceName,
    method: method,
    ...params
  })
}

// 设备管理API接口

/**
 * 获取设备列表（分页）
 * @param {Object} params - 查询参数
 * @param {number} params.current - 当前页码
 * @param {number} params.size - 每页大小
 * @param {string} params.keyword - 搜索关键字（设备名称或设备ID）
 * @param {string} params.status - 设备状态
 * @param {string} params.tag - 标签
 */
export function getDeviceList(params) {
  return commonFunc('/vlsDeviceInfo/list', params, 'GET')
}

/**
 * 获取设备详情
 * @param {number} id - 设备ID
 */
export function getDeviceById(id) {
  return commonFunc(`/vlsDeviceInfo/${id}`, {}, 'GET')
}

/**
 * 获取设备详情
 */
export function getDeviceDetail(data) {
  return commonFunc('/vlsDeviceInfo/detail', data, 'GET')
}

/**
 * 获取摄像机OSD设置
 */
export function getVlsCameraOsdSetting(data) {
  return commonFunc('/vlsCameraOsdSetting/detail', data, 'GET')
}

/**
 * 新增或修改摄像机OSD设置
 */
export function updateVlsCameraOsdSetting(data) {
  return commonFunc('/vlsCameraOsdSetting/submit', data, 'POST')
}

/**
 * 恢复默认值摄像机OSD设置
 */
export function restoreDefaultVlsCameraOsdSetting(data) {
  return commonFunc('/vlsCameraOsdSetting/restoreDefault', data, 'GET')
}

/**
 * 获取摄像机显示设置
 */
export function getVlsCameraDisplaySetting(data) {
  return commonFunc('/vlsCameraDisplaySetting/detail', data, 'GET')
}

/**
 * 新增或修改摄像机显示设置
 */
export function updateVlsCameraDisplaySetting(data) {
  return commonFunc('/vlsCameraDisplaySetting/submit', data, 'POST')
}

/**
 * 恢复默认值摄像机显示设置
 */
export function restoreDefaultVlsCameraDisplaySetting(data) {
  return commonFunc('/vlsCameraDisplaySetting/restoreDefault', data, 'GET')
}

/**
 * 获取摄像头事件策略
 */
export function getVlsRecordEventStrategy(deviceId, data) {
  return commonFunc(`/vlsRecordEventStrategy/${deviceId}`, data, 'GET')
}

/**
 * 保存摄像头事件策略
 */
export function saveVlsRecordEventStrategy(data) {
  return commonFunc('/vlsRecordEventStrategy', data, 'POST')
}

/**
 * 获取音频异常侦测设置
 */
export function getVlsAudioAnomalyDetectionSetting(data) {
  return commonFunc('/vlsAudioAnomalyDetectionSetting/detail', data, 'GET')
}

/**
 * 保存音频异常侦测设置
 */
export function saveVlsAudioAnomalyDetectionSetting(data) {
  return commonFunc('/vlsAudioAnomalyDetectionSetting/submit', data, 'POST')
}

/**
 * 获取音频联动方式设置
 */
export function getVlsAudioLinkageModeSetting(data) {
  return commonFunc('/vlsAudioLinkageModeSetting/detail', data, 'GET')
}

/**
 * 保存音频联动方式设置
 */
export function saveVlsAudioLinkageModeSetting(data) {
  return commonFunc('/vlsAudioLinkageModeSetting/submit', data, 'POST')
}

/**
 * 获取音频布防时间设置
 */
export function getVlsAudioDefenseTimeSetting(data) {
  return commonFunc('/vlsAudioDefenseTimeSetting/detail', data, 'GET')
}

/**
 * 保存音频布防时间设置
 */
export function saveVlsAudioDefenseTimeSetting(data) {
  return commonFunc('/vlsAudioDefenseTimeSetting/submit', data, 'POST')
}

/**
 * 获取时间策略表
 */
export function getVlsTimeStrategy(data) {
  return commonFunc('/vlsTimeStrategy/detail', data, 'GET')
}

/**
 * 修改时间策略表
 */
export function updateVlsTimeStrategy(data) {
  return commonFunc('/vlsTimeStrategy/submit', data, 'POST')
}

/**
 * 创建设备
 * @param {Object} data - 设备信息
 */
export function createDevice(data) {
  return commonFunc('/vlsDeviceInfo', data)
}

/**
 * 更新设备
 * @param {number} id - 设备ID
 * @param {Object} data - 设备信息
 */
export function updateDevice(id, data) {
  return commonFunc(`/vlsDeviceInfo/${id}`, data, 'PUT')
}

/**
 * 摄像头使用申请
 */
export function vlsCameraApply(id, data) {
  return commonFunc('/vlsCameraApply/camera-apply/submit', data, 'POST')
}

/**
 * 删除设备
 * @param {number} id - 设备ID
 */
export function deleteDevice(id) {
  return commonFunc(`/vlsDeviceInfo/${id}`, {}, 'DELETE')
}

/**
 * 删除设备
 */
export function removeDevice(data) {
  return commonFunc('/vlsDeviceInfo/remove', data, 'GET')
}

/**
 * 批量删除设备
 * @param {Array} ids - 设备ID数组
 */
export function batchDeleteDevices(ids) {
  return commonFunc('/vlsDeviceInfo/batch', ids, 'DELETE')
}

/**
 * 获取设备统计信息
 */
export function getDeviceStatistics() {
  return commonFunc('/vlsDeviceInfo/statistics', {}, 'GET')
}

/**
 * 获取设备树结构
 */
export function getDeviceTree() {
  return commonFunc('/vlsDeviceInfo/tree', {}, 'GET')
}

/**
 * 测试设备连接
 * @param {number} id - 设备ID
 */
export function testDeviceConnection(id) {
  return commonFunc(`/vlsDeviceInfo/${id}/test`, {}, 'POST')
}

/**
 * 刷新设备状态
 * @param {number} id - 设备ID
 */
export function refreshDeviceStatus(id) {
  return commonFunc(`/vlsDeviceInfo/${id}/refresh`, {}, 'POST')
}

/**
 * 批量刷新设备状态
 * @param {Array} ids - 设备ID数组
 */
export function batchRefreshDevices(ids) {
  return commonFunc('/vlsDeviceInfo/batch/refresh', { ids }, 'POST')
}

/**
 * 获取设备类型统计
 */
export function getDeviceTypeStatistics() {
  return commonFunc('/vlsDeviceInfo/type-statistics', {}, 'GET')
}

/**
 * 获取所有标签列表
 */
export function getDeviceTags() {
  return commonFunc('/vlsDeviceInfo/tags', {}, 'GET')
}

/**
 * PTZ控制 - 上下左右移动
 * @param {number} id - 设备ID
 * @param {string} direction - 方向：up, down, left, right
 * @param {number} speed - 速度1-8
 */
export function ptzMove(id, direction, speed = 4) {
  return commonFunc(`/vlsDeviceInfo/${id}/ptz/move`, { direction, speed }, 'POST')
}

/**
 * PTZ控制 - 停止移动
 * @param {number} id - 设备ID
 */
export function ptzStop(id) {
  return commonFunc(`/vlsDeviceInfo/${id}/ptz/stop`, {}, 'POST')
}

/**
 * PTZ控制 - 缩放
 * @param {number} id - 设备ID
 * @param {string} action - 动作：zoom_in, zoom_out
 * @param {number} speed - 速度1-8
 */
export function ptzZoom(id, action, speed = 4) {
  return commonFunc(`/vlsDeviceInfo/${id}/ptz/zoom`, { action, speed }, 'POST')
}

/**
 * 获取设备视频流信息
 * @param {number} id - 设备ID
 */
export function getDeviceStreamInfo(id) {
  return commonFunc(`/vlsDeviceInfo/${id}/stream`, {}, 'GET')
}

/**
 * 导出设备列表
 * @param {Object} params - 查询参数
 */
export function exportDevices(params) {
  return request({
    url: config.URL + config.gateWay + 'vls-server/' + '/vlsDeviceInfo/export',
    method: 'get',
    data: params,
    responseType: 'blob'
  })
}

/**
 * 导入设备列表
 * @param {FormData} formData - 包含文件的表单数据
 */
export function importDevices(formData) {
  return request({
    url: config.URL + config.gateWay + 'vls-server/' + '/vlsDeviceInfo/import',
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
  return commonFunc(`/vlsDeviceInfo/${algorithmId}/algorithms`, { deviceIds: deviceIdsStr }, 'POST')
}

