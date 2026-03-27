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
 * 获取图片的标注实例列表
 * @param {number} annotationId - 标注项目ID
 * @param {string} imageName - 图片名称
 * @returns {Promise}
 */
export function getAnnotationInstances(annotationId, imageName) {
  return commonFunc(
    `/vlsAnnotationInstance/${annotationId}/instances`,
    { imageName },
    'GET'
  )
}

/**
 * 获取标注项目的所有标注实例列表
 * @param {number} annotationId - 标注项目ID
 * @returns {Promise}
 */
export function getAllAnnotationInstances(annotationId) {
  return commonFunc(
    `/vlsAnnotationInstance/${annotationId}/instances/all`,
    {},
    'GET'
  )
}

/**
 * 批量保存图片的标注实例
 * @param {number} annotationId - 标注项目ID
 * @param {string} imageId - 图片id
 * @param {Array<Object>} instances - 标注实例列表
 * @returns {Promise}
 */
export function batchSaveAnnotationInstances(annotationId, imageId, instances) {
  return commonFunc(
    `/vlsAnnotationInstance/${annotationId}/instances/batch`,
    { imageId, instances }
  )
}

/**
 * 删除标注实例
 * @param {number} instanceId - 实例ID
 * @returns {Promise}
 */
export function deleteAnnotationInstance(instanceId) {
  return commonFunc(
    `/vlsAnnotationInstance/instances/${instanceId}`,
    {},
    'DELETE'
  )
}

/**
 * 批量删除标注实例
 * @param {Array} instanceIds - 实例ID数组
 * @returns {Promise}
 */
export function batchDeleteAnnotationInstances(instanceIds) {
  return commonFunc(
    '/vlsAnnotationInstance/instances/batch',
    instanceIds,
    'DELETE'
  )
}

/**
 * Delete annotation instances under an annotation by image name list
 * @param {number} annotationId - Annotation project ID
 * @param {string|string[]} imageIds - Image names to delete
 * @returns {Promise}
 */
export function deleteAnnotationInstancesByImage(annotationId, imageIds) {
  return commonFunc(
    '/vlsAnnotationInstance/instances/by-image',
    { annotationId, imageIds },
    'DELETE'
  )
}
