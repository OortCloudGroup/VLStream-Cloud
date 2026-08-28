/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/request'

// Query algorithmannotationdata
export function getAlgorithmAnnotationPage(params) {
  return request({
    url: '/vlsAlgorithmAnnotation/page',
    method: 'get',
    params
  })
}

// IDQuery algorithmannotation
export function getAlgorithmAnnotationById(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}`,
    method: 'get'
  })
}

// Add algorithmannotation
export function createAlgorithmAnnotation(data) {
  return request({
    url: '/vlsAlgorithmAnnotation',
    method: 'post',
    data
  })
}

// new algorithmannotation
export function updateAlgorithmAnnotation(id, data) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}`,
    method: 'put',
    data
  })
}

// Delete algorithmannotation
export function deleteAlgorithmAnnotation(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}`,
    method: 'delete'
  })
}

// Batch delete algorithmannotation
export function batchDeleteAlgorithmAnnotation(ids) {
  return request({
    url: '/vlsAlgorithmAnnotation/batch',
    method: 'delete',
    data: ids
  })
}

// startannotationtask
export function startAnnotationTask(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/start`,
    method: 'post'
  })
}

// annotationtask
export function completeAnnotationTask(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/complete`,
    method: 'post'
  })
}

// annotationtask
export function resetAnnotationTask(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/reset`,
    method: 'post'
  })
}

// new annotation
export function updateAnnotationProgress(id, annotatedCount) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/progress`,
    method: 'put',
    data: { annotatedCount }
  })
}

// Export annotationdata
export function exportAnnotationData(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/export`,
    method: 'post',
    responseType: 'blob'
  })
}

// Import annotationdata
export function importAnnotationData(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/import-zip`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// Get data
export function getStatistics() {
  return request({
    url: '/vlsAlgorithmAnnotation/statistics',
    method: 'get'
  })
}

// Get
export function getTypeStatistics() {
  return request({
    url: '/vlsAlgorithmAnnotation/statistics/type',
    method: 'get'
  })
}

// Get
export function getStatusStatistics() {
  return request({
    url: '/vlsAlgorithmAnnotation/statistics/status',
    method: 'get'
  })
}

// Get
export function getProgressStatistics() {
  return request({
    url: '/vlsAlgorithmAnnotation/statistics/progress',
    method: 'get'
  })
}

// Get
export function getWorkloadStatistics() {
  return request({
    url: '/vlsAlgorithmAnnotation/statistics/workload',
    method: 'get'
  })
}

// algorithmannotation
export function searchAlgorithmAnnotation(params) {
  return request({
    url: '/vlsAlgorithmAnnotation/search',
    method: 'get',
    params
  })
}

// operation
export function batchOperation(operation, ids) {
  return request({
    url: '/vlsAlgorithmAnnotation/batch-operation',
    method: 'post',
    data: { operation, ids }
  })
}

// annotationdata service
export function saveDataset(id, annotationData) {
  const formData = new FormData()
  formData.append('annotationData', annotationData)
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/save-dataset`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}


// Delete annotationinstance
export function deleteAnnotationInstance(instanceId) {
  return request({
    url: `/api/annotation-label/instances/${instanceId}`,
    method: 'delete'
  })
}

// Delete
export function deleteImage(fileName) {
  return request({
    url: '/image/delete',
    method: 'delete',
    params: { fileName }
  })
}



// annotation
export const ANNOTATION_TYPES = {
  OBJECT_DETECTION: 'object_detection',
  IMAGE_CLASSIFICATION: 'image_classification',
  INSTANCE_SEGMENTATION: 'instance_segmentation',
  SEMANTIC_SEGMENTATION: 'semantic_segmentation'
}

// annotation
export const ANNOTATION_STATUS = {
  NONE: 'none',
  PARTIAL: 'partial',
  COMPLETED: 'completed'
}

// annotation
export const ANNOTATION_TYPE_LABELS = {
  [ANNOTATION_TYPES.OBJECT_DETECTION]: '物体检测',
  [ANNOTATION_TYPES.IMAGE_CLASSIFICATION]: '图像分类',
  [ANNOTATION_TYPES.INSTANCE_SEGMENTATION]: '实例分割',
  [ANNOTATION_TYPES.SEMANTIC_SEGMENTATION]: '语义分割'
}

// annotation
export const ANNOTATION_STATUS_LABELS = {
  [ANNOTATION_STATUS.NONE]: '未标注',
  [ANNOTATION_STATUS.PARTIAL]: '标注中',
  [ANNOTATION_STATUS.COMPLETED]: '已完成'
}

// Get
export function getProgressPercentage(annotatedCount, totalCount) {
  if (totalCount === 0) return 0
  return Math.round((annotatedCount / totalCount) * 100)
}

// Get
export function getStatusTagType(status) {
  switch(status) {
    case ANNOTATION_STATUS.NONE:
      return 'info'
    case ANNOTATION_STATUS.PARTIAL:
      return 'warning'
    case ANNOTATION_STATUS.COMPLETED:
      return 'success'
    default:
      return 'info'
  }
}