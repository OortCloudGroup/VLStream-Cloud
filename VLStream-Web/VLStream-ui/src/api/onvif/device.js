/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import request from '@/utils/wvpRequest';

export function WSDiscovery(){
    return request({
        url: '/onvif/device/WSDiscovery',
        method: 'get',
        timeout: 10000 * 6
    })
}

export function addOnvif(data) {
  return request({
    url: '/onvif/device/addOnvif',
    method: 'post',
    data: data
  })
}

// Query onvif device list
export function listDevice(query) {
  return request({
    url: '/onvif/device/list',
    method: 'get',
    params: query
  })
}

// Query onvif device list
export function deviceList(query) {
  return request({
    url: '/onvif/device/deviceList',
    method: 'get',
    params: query
  })
}

// Query onvif device
export function getDevice(id) {
  return request({
    url: '/onvif/device/' + id,
    method: 'get'
  })
}

// Add onvif device
export function addDevice(data) {
  return request({
    url: '/onvif/device',
    method: 'post',
    data: data
  })
}

// Update onvif device
export function updateDevice(data) {
  return request({
    url: '/onvif/device',
    method: 'put',
    data: data
  })
}

// Delete onvif device
export function delDevice(id) {
  return request({
    url: '/onvif/device/' + id,
    method: 'delete'
  })
}

// Get channeltoken
export function getChannelToken(query) {
  return request({
    url: '/onvif/service/getChannelToken',
    method: 'get',
    params: query
  })
}

//
export function absoluteMove(query) {
  return request({
    url: '/onvif/service/absoluteMove',
    method: 'get',
    params: query
  })
}

//
export function continuousMove(query) {
  return request({
    url: '/onvif/service/continuousMove',
    method: 'get',
    params: query
  })
}

//
export function continuousMoveStop(query) {
  return request({
    url: '/onvif/service/continuousMoveStop',
    method: 'get',
    params: query
  })
}


// Get
export function getPresetList(query) {
  return request({
    url: '/onvif/service/getPresets',
    method: 'get',
    params: query
  })
}

//
export function getGotoPreset(query) {
  return request({
    url: '/onvif/service/gotoPreset',
    method: 'get',
    params: query
  })
}

// Delete
export function removePreset(query) {
  return request({
    url: '/onvif/service/removePreset',
    method: 'get',
    params: query
  })
}

//
export function addPreset(query) {
  return request({
    url: '/onvif/service/addPreset',
    method: 'get',
    params: query
  })
}


// start
export function onvifPZTStart(query) {
  return request({
    url: '/onvif/service/onvifPZTStart',
    method: 'get',
    params: query
  })
}

// finish
export function onvifPZTEnd(query) {
  return request({
    url: '/onvif/service/onvifPZTEnd',
    method: 'get',
    params: query
  })
}

