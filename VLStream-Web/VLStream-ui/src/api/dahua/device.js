/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import request from '@/utils/wvpRequest'

// Query device list ( )
export function listDevice(query) {
  return request({
    url: '/dahua/device/list',
    method: 'get',
    params: query
  })
}


// Query device list ( )
export function listDahuaDevice(query) {
  return request({
    url: '/dahua/device/listDahuaDevice',
    method: 'get',
    params: query
  })
}


// Query device
export function getDevice(id) {
  return request({
    url: '/dahua/device/' + id,
    method: 'get'
  })
}

// Add device
export function addDevice(data) {
  return request({
    url: '/dahua/device',
    method: 'post',
    data: data
  })
}

// Update device
export function updateDevice(data) {
  return request({
    url: '/dahua/device',
    method: 'put',
    data: data
  })
}

// Delete device
export function delDevice(id) {
  return request({
    url: '/dahua/device/' + id,
    method: 'delete'
  })
}

// device
export function startRealPlay(id) {
  return request({
    url: '/dahua/device/startRealPlay/' + id,
    method: 'get'
  })
}

export function stopRealPlay(id) {
  return request({
    url: '/dahua/device/stopRealPlay/' + id,
    method: 'get'
  })
}

// device
export function dahuaLogin(data) {
  return request({
    url: '/dahua/device/login',
    method: 'post',
    data: data
  })
}

// Get device
export function getRegisterDeviceList() {
  return request({
    url: '/dahua/device/getRegisterDeviceList',
    method: 'get'
  })
}

// device control (start)
export function ptzControlUpStart(id,direction,speed) {
  return request({
    url: '/dahua/device/ptzControlUpStart/'+id,
    method: 'get',
    params: {
      direction,
      speed,
    }
  })
}

// device control ( )
export function ptzControlUpEnd(id,direction) {
    return request({
        url: '/dahua/device/ptzControlUpEnd/'+id,
        method: 'get',
        params: {
            direction,
        }
    })
}

// device
export function snapPicture(id) {
  return request({
    url: '/dahua/device/snapPicture/'+id,
    method: 'get',
  })
}

// Query device list
export function listScreenshot(query) {
  return request({
    url: '/dahua/device/listScreenshot',
    method: 'get',
    params: query
  })
}

// Delete device
export function removeScreenshot(id) {
  return request({
    url: '/dahua/device/removeScreenshot/' + id,
    method: 'delete'
  })
}

// device
export function timerCapturePicture(id,interval) {
  return request({
    url: '/dahua/device/timerCapturePicture/' + id,
    method: 'get',
    params: {
        interval,
    }
  })
}

// device
export function stopCapturePicture(id) {
  return request({
    url: '/dahua/device/stopCapturePicture/' + id,
    method: 'get',
  })
}

// deviceGet
export function getTime(id) {
  return request({
    url: '/dahua/device/getTime/' + id,
    method: 'get',
  })
}

// deviceSet
export function setTime(id,date,type) {
  return request({
    url: '/dahua/device/setTime/' + id,
    method: 'get',
    params: {
      date,
      type
    }
  })
}

// device
export function reboot(id) {
  return request({
    url: '/dahua/device/reboot/' + id,
    method: 'get',
  })
}

// Delete device
export function delRegisterDevice(ips) {
  return request({
    url: '/dahua/device/delRegisterDevice/' + ips,
    method: 'delete',
  })
}

