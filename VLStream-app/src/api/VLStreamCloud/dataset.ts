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
 * 连接远程服务器（含自定义请求头，单独处理）
 * @param {Object} params - 连接参数
 * @param {string} params.host - 服务器地址
 * @param {string} params.username - 用户名
 * @param {string} params.password - 密码
 * @param {string} params.path - 数据集路径
 */
export function connectToServer(params) {
  return request({
    url: config.URL + config.gateWay + 'vls-server/' + '/api/dataset/connect',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * 获取远程服务器文件列表
 * @param {Object} params - 查询参数
 * @param {string} params.host - 服务器地址
 * @param {string} params.path - 文件路径
 */
export function getServerFiles(params) {
  return commonFunc('/api/dataset/files', params, 'GET')
}

/**
 * 获取远程文件内容
 * @param {Object} params - 查询参数
 * @param {string} params.host - 服务器地址
 * @param {string} params.path - 文件路径
 * @param {string} params.filename - 文件名
 */
export function getFileContent(params) {
  return commonFunc('/api/dataset/file-content', params, 'GET')
}

/**
 * 下载远程文件（含blob响应类型，单独处理）
 * @param {Object} params - 下载参数
 * @param {string} params.host - 服务器地址
 * @param {string} params.path - 文件路径
 * @param {string} params.filename - 文件名
 */
export function downloadFile(params) {
  return request({
    url: config.URL + config.gateWay + 'vls-server/' + '/api/dataset/download',
    method: 'get',
    data: params,
    responseType: 'blob'
  })
}

/**
 * 上传文件到远程服务器（含FormData/多表单头，单独处理）
 * @param {Object} params - 上传参数
 * @param {string} params.host - 服务器地址
 * @param {string} params.path - 目标路径
 * @param {File} params.file - 要上传的文件
 */
export function uploadFileToServer(params) {
  const formData = new FormData()
  formData.append('host', params.host)
  formData.append('path', params.path)
  formData.append('file', params.file)

  return request({
    url: config.URL + config.gateWay + 'vls-server/' + '/api/dataset/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 创建远程目录
 * @param {Object} params - 创建参数
 * @param {string} params.host - 服务器地址
 * @param {string} params.path - 目录路径
 */
export function createRemoteDirectory(params) {
  return commonFunc('/api/dataset/create-directory', params)
}

/**
 * 删除远程文件或目录
 * @param {Object} params - 删除参数
 * @param {string} params.host - 服务器地址
 * @param {string} params.path - 文件或目录路径
 */
export function deleteRemoteFile(params) {
  return commonFunc('/api/dataset/delete', params, 'DELETE')
}
