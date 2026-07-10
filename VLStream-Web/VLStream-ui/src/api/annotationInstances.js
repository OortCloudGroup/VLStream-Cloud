import request from '@/utils/request'

/**
 * 获取图片的标注实例列表
 * @param {number} annotationId - 标注项目ID
 * @param {string} imageName - 图片名称
 * @returns {Promise}
 */
export function getAnnotationInstances(annotationId, imageName) {
    return request({
        url: `/vlsAnnotationInstance/${annotationId}/instances`,
        method: 'get',
        params: { imageName }
    })
}

/**
 * 获取标注项目的所有标注实例列表
 * @param {number} annotationId - 标注项目ID
 * @returns {Promise}
 */
export function getAllAnnotationInstances(annotationId) {
    return request({
        url: `/vlsAnnotationInstance/${annotationId}/instances/all`,
        method: 'get'
    })
}

/**
 * 批量保存图片的标注实例
 * @param {number} annotationId - 标注项目ID
 * @param {string} imageId - 图片id
 * @param {Array<Object>} instances - 标注实例列表
 * @returns {Promise}
 */
export function batchSaveAnnotationInstances(annotationId, imageId, instances) {
    return request({
        url: `/vlsAnnotationInstance/${annotationId}/instances/batch`,
        method: 'post',
        data: { imageId, instances }
    })
}

/**
 * 删除标注实例
 * @param {number} instanceId - 实例ID
 * @returns {Promise}
 */
export function deleteAnnotationInstance(instanceId) {
    return request({
        url: `/vlsAnnotationInstance/instances/${instanceId}`,
        method: 'delete'
    })
}

/**
 * 批量删除标注实例
 * @param {Array} instanceIds - 实例ID数组
 * @returns {Promise}
 */
export function batchDeleteAnnotationInstances(instanceIds) {
    return request({
        url: '/vlsAnnotationInstance/instances/batch',
        method: 'delete',
        data: instanceIds
    })
}

/**
 * Delete annotation instances under an annotation by image name list
 * @param {number} annotationId - Annotation project ID
 * @param {string|string[]} imageIds - Image names to delete
 * @returns {Promise}
 */
export function deleteAnnotationInstancesByImage(annotationId, imageIds) {
    return request({
        url: '/vlsAnnotationInstance/instances/by-image',
        method: 'delete',
        data: { annotationId, imageIds: imageIds }
    })
}
