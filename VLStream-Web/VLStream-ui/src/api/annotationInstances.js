/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

/**
 * Get annotationinstance
 * @param {number} annotationId - annotation item ID
 * @param {string} imageName -
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
 * Get annotation item all annotationinstance
 * @param {number} annotationId - annotation item ID
 * @returns {Promise}
 */
export function getAllAnnotationInstances(annotationId) {
    return request({
        url: `/vlsAnnotationInstance/${annotationId}/instances/all`,
        method: 'get'
    })
}

/**
 * annotationinstance
 * @param {number} annotationId - annotation item ID
 * @param {string} imageId - id
 * @param {Array<Object>} instances - annotationinstance
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
 * Delete annotationinstance
 * @param {number} instanceId - instanceID
 * @returns {Promise}
 */
export function deleteAnnotationInstance(instanceId) {
    return request({
        url: `/vlsAnnotationInstance/instances/${instanceId}`,
        method: 'delete'
    })
}

/**
 * Batch delete annotationinstance
 * @param {Array} instanceIds - instanceIDarray
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
