/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/wvpRequest'

// Query isupdevice list
export function listIsupDevice(query) {
  return request({
    url: '/isup/lsupDevice/list',
    method: 'get',
    params: query
  })
}

// Query isupdevice list
export function lsupDeviceList(query) {
  return request({
    url: '/isup/lsupDevice/lsupDeviceList',
    method: 'get',
    params: query
  })
}


//
export function start(id) {
  return request({
    url: '/isup/lsupDevice/start/' + id,
    method: 'get'
  })
}

export function stopRealPlay(id) {
  return request({
    url: '/isup/lsupDevice/stopRealPlay/' + id,
    method: 'get'
  })
}


export function getLsupDevice(id) {
  return request({
    url: '/isup/lsupDevice/' + id,
    method: 'get'
  })
}

// Add isupdevice
export function addLsupDevice(data) {
  return request({
    url: '/isup/lsupDevice',
    method: 'post',
    data: data
  })
}

// Update isupdevice
export function updateLsupDevice(data) {
  return request({
    url: '/isup/lsupDevice',
    method: 'put',
    data: data
  })
}

// Delete isupdevice
export function delLsupDevice(id) {
  return request({
    url: '/isup/lsupDevice/' + id,
    method: 'delete'
  })
}

// control (start)
export function ptzCtrlStart(id,direction,controSpeed) {
  return request({
    url: '/isup/lsupDevice/ptzCtrlStart/' + id,
    method: 'get',
    params: {
      direction,
      controSpeed,
    }
  })
}

// control (finish)
export function ptzCtrlEnd(id) {
  return request({
    url: '/isup/lsupDevice/ptzCtrlEnd/' + id,
    method: 'get',
  })
}

// control ( )
export function ptzCtrlFocus(id,controSpeed) {
  return request({
    url: '/isup/lsupDevice/ptzCtrlFocus/' + id,
    method: 'get',
    params: {
      controSpeed,
    }
  })
}
