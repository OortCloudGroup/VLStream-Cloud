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

// 查询onvif 设备列表
export function listDevice(query) {
  return request({
    url: '/onvif/device/list',
    method: 'get',
    params: query
  })
}

// 查询onvif 设备列表
export function deviceList(query) {
  return request({
    url: '/onvif/device/deviceList',
    method: 'get',
    params: query
  })
}

// 查询onvif 设备详细
export function getDevice(id) {
  return request({
    url: '/onvif/device/' + id,
    method: 'get'
  })
}

// 新增onvif 设备
export function addDevice(data) {
  return request({
    url: '/onvif/device',
    method: 'post',
    data: data
  })
}

// 修改onvif 设备
export function updateDevice(data) {
  return request({
    url: '/onvif/device',
    method: 'put',
    data: data
  })
}

// 删除onvif 设备
export function delDevice(id) {
  return request({
    url: '/onvif/device/' + id,
    method: 'delete'
  })
}

// 获取通道token
export function getChannelToken(query) {
  return request({
    url: '/onvif/service/getChannelToken',
    method: 'get',
    params: query
  })
}

// 绝对位置移动
export function absoluteMove(query) {
  return request({
    url: '/onvif/service/absoluteMove',
    method: 'get',
    params: query
  })
}

// 连续移动
export function continuousMove(query) {
  return request({
    url: '/onvif/service/continuousMove',
    method: 'get',
    params: query
  })
}

// 连续移动停止
export function continuousMoveStop(query) {
  return request({
    url: '/onvif/service/continuousMoveStop',
    method: 'get',
    params: query
  })
}


// 获取预置点列表
export function getPresetList(query) {
  return request({
    url: '/onvif/service/getPresets',
    method: 'get',
    params: query
  })
}

// 移动到预置点
export function getGotoPreset(query) {
  return request({
    url: '/onvif/service/gotoPreset',
    method: 'get',
    params: query
  })
}

// 删除预置点
export function removePreset(query) {
  return request({
    url: '/onvif/service/removePreset',
    method: 'get',
    params: query
  })
}

// 添加预置点
export function addPreset(query) {
  return request({
    url: '/onvif/service/addPreset',
    method: 'get',
    params: query
  })
}


// 云台开始
export function onvifPZTStart(query) {
  return request({
    url: '/onvif/service/onvifPZTStart',
    method: 'get',
    params: query
  })
}

// 云台结束
export function onvifPZTEnd(query) {
  return request({
    url: '/onvif/service/onvifPZTEnd',
    method: 'get',
    params: query
  })
}

