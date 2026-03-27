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
 * 标注标签API
 */

/**
 * 获取标注项目的标签列表
 * @param {number} annotationId - 标注项目ID
 * @param {string} keyword - 搜索关键词（可选）
 * @returns {Promise}
 */
export function getAnnotationLabels(annotationId, keyword = '') {
  return commonFunc(
    `/vlsAnnotationLabel/${annotationId}/labels`,
    keyword ? { keyword } : {},
    'GET'
  )
}

/**
 * 创建标注标签
 * @param {number} annotationId - 标注项目ID
 * @param {Object} data - 标签数据
 * @returns {Promise}
 */
export function createAnnotationLabel(annotationId, data) {
  return commonFunc(`/vlsAnnotationLabel/${annotationId}/labels`, data)
}

/**
 * 更新标注标签
 * @param {number} id - 标签ID
 * @param {Object} data - 更新数据
 * @returns {Promise}
 */
export function updateAnnotationLabel(id, data) {
  return commonFunc(`/vlsAnnotationLabel/${id}`, data, 'PUT')
}

/**
 * 删除标注标签
 * @param {number} id - 标签ID
 * @returns {Promise}
 */
export function deleteAnnotationLabel(id) {
  return commonFunc(`/vlsAnnotationLabel/${id}`, {}, 'DELETE')
}
