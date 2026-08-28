/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/request'

/**
 * service
 * @param {Object} params - parameter
 * @param {string} params.host - service
 * @param {string} params.username - user
 * @param {string} params.password -
 * @param {string} params.path - dataset
 */
export function connectToServer(params) {
  return request({
    url: '/api/dataset/connect',
    method: 'post',
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    }
  })
}

/**
 * Get service
 * @param {Object} params - Query parameter
 * @param {string} params.host - service
 * @param {string} params.path -
 */
export function getServerFiles(params) {
  return request({
    url: '/api/dataset/files',
    method: 'get',
    params
  })
}

/**
 * Get
 * @param {Object} params - Query parameter
 * @param {string} params.host - service
 * @param {string} params.path -
 * @param {string} params.filename -
 */
export function getFileContent(params) {
  return request({
    url: '/api/dataset/file-content',
    method: 'get',
    params
  })
}

/**
 *
 * @param {Object} params - parameter
 * @param {string} params.host - service
 * @param {string} params.path -
 * @param {string} params.filename -
 */
export function downloadFile(params) {
  return request({
    url: '/api/dataset/download',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * service
 * @param {Object} params - parameter
 * @param {string} params.host - service
 * @param {string} params.path -
 * @param {File} params.file - need to
 */
export function uploadFileToServer(params) {
  const formData = new FormData()
  formData.append('host', params.host)
  formData.append('path', params.path)
  formData.append('file', params.file)

  return request({
    url: '/api/dataset/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 *
 * @param {Object} params - parameter
 * @param {string} params.host - service
 * @param {string} params.path -
 */
export function createRemoteDirectory(params) {
  return request({
    url: '/api/dataset/create-directory',
    method: 'post',
    data: params
  })
}

/**
 * Delete
 * @param {Object} params - Delete parameter
 * @param {string} params.host - service
 * @param {string} params.path -
 */
export function deleteRemoteFile(params) {
  return request({
    url: '/api/dataset/delete',
    method: 'delete',
    params
  })
}