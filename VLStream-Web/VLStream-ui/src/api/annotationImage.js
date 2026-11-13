/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

export async function getAnnotationImagePage(annotationId, params) {
  const response = await request({
    url: `/vlsAnnotationImage/dataset/${annotationId}/page`,
    method: 'get',
    timeout: 20000,
    params
  })
  if (response.code !== 200 || response.success === false) {
    throw new Error(response.msg || '图片列表加载失败')
  }
  return response.data
}

/**
 * Get annotation item
 * @param {number} annotationId - annotation item ID
 * @returns {Promise}
 */
export function getAnnotationImages(annotationId) {
  return request({
    url: `/vlsAnnotationImage/dataset/${annotationId}`,
    timeout: 20000,
    method: 'get'
  })
}

/**
 * info annotation_image
 * @param {Array} files -
 * @param {number} annotationId - annotation item ID
 * @param {string} category -
 * @returns {Promise}
 */
export function uploadAnnotationImages(files, annotationId) {
  const formData = new FormData()

  // all ( after @RequestPart("files") MultipartFile[])
  const fileList = Array.isArray(files) ? files : [files]
  fileList.forEach(file => {
    formData.append('files', file)
  })

  // annotation item ID ( after @RequestParam("annotationId"))
  formData.append('annotationId', annotationId)

  return request({
    url: '/vlsAnnotationImage/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * info annotation_image
 * @param {Object} imageData - info
 * @returns {Promise}
 */
export function saveAnnotationImage(imageData) {
  return request({
    url: '/vlsAnnotationImage/images',
    method: 'post',
    data: imageData
  })
}

/**
 * info annotation_image
 * @param {Array} imagesData - info
 * @returns {Promise}
 */
export function batchSaveAnnotationImages(imagesData) {
  return request({
    url: '/vlsAnnotationImage/images/batch',
    method: 'post',
    data: imagesData
  })
}
