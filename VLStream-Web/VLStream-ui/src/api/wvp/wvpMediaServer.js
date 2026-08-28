/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/wvpRequest'

// Query service list
export function listWvpMediaServer(query) {
  return request({
    url: '/api/server/media_server/list',
    method: 'get',
    params: query
  })
}

// Query service
export function getWvpMediaServer(id) {
  return request({
    url: '/api/server/media_server/one/' + id,
    method: 'get'
  })
}

// Add service
export function saveWvpMediaServer(data) {
  return request({
    url: '/api/server/media_server/save',
    method: 'post',
    data: data
  })
}

// Update service
export function updateWvpMediaServer(data) {
  return request({
    url: '/wvp/wvpMediaServer',
    method: 'put',
    data: data
  })
}

// Delete service
export function delWvpMediaServer(id) {
  return request({
    url: '/api/server/media_server/delete/' + id,
    method: 'delete'
  })
}

// stream mediaservicewhether
export function checkMediaServer(query) {
  return request({
    url: '/api/server/media_server/check',
    method: 'get',
    params: query
  })
}

// Get configurationinfo
export function configInfo() {
  return request({
    url: '/api/server/system/configInfo',
    method: 'get',
  })
}

// Get stream mediaservice
export function getOnlineMediaServerList() {
  return request({
    url: '/api/server/media_server/online/list',
    method: 'get',
  })
}

