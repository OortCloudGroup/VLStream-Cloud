import request from '@/utils/request'

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
  return request({
    url: `/vlsAnnotationLabel/${annotationId}/labels`,
    method: 'get',
    params: keyword ? { keyword } : {}
  })
}

/**
 * 创建标注标签
 * @param {number} annotationId - 标注项目ID
 * @param {Object} data - 标签数据
 * @returns {Promise}
 */
export function createAnnotationLabel(annotationId, data) {
  return request({
    url: `/vlsAnnotationLabel/${annotationId}/labels`,
    method: 'post',
    data
  })
}

/**
 * 更新标注标签
 * @param {number} id - 标签ID
 * @param {Object} data - 更新数据
 * @returns {Promise}
 */
export function updateAnnotationLabel(id, data) {
  return request({
    url: `/vlsAnnotationLabel/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除标注标签
 * @param {number} id - 标签ID
 * @returns {Promise}
 */
export function deleteAnnotationLabel(id) {
  return request({
    url: `/vlsAnnotationLabel/${id}`,
    method: 'delete'
  })
}