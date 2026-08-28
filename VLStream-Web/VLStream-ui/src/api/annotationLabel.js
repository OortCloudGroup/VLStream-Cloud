/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

/**
 * annotation API
 */

/**
 * Get annotation item
 * @param {number} annotationId - annotation item ID
 * @param {string} keyword - ( )
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
 * annotation
 * @param {number} annotationId - annotation item ID
 * @param {Object} data - data
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
 * new annotation
 * @param {number} id - ID
 * @param {Object} data - new data
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
 * Delete annotation
 * @param {number} id - ID
 * @returns {Promise}
 */
export function deleteAnnotationLabel(id) {
  return request({
    url: `/vlsAnnotationLabel/${id}`,
    method: 'delete'
  })
}