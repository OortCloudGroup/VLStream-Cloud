import request from '@/utils/wvpRequest'

// 查询isup设备列表
export function listIsupDevice(query) {
  return request({
    url: '/isup/lsupDevice/list',
    method: 'get',
    params: query
  })
}

// 查询isup设备列表
export function lsupDeviceList(query) {
  return request({
    url: '/isup/lsupDevice/lsupDeviceList',
    method: 'get',
    params: query
  })
}


// 播放
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

// 新增isup设备
export function addLsupDevice(data) {
  return request({
    url: '/isup/lsupDevice',
    method: 'post',
    data: data
  })
}

// 修改isup设备
export function updateLsupDevice(data) {
  return request({
    url: '/isup/lsupDevice',
    method: 'put',
    data: data
  })
}

// 删除isup设备
export function delLsupDevice(id) {
  return request({
    url: '/isup/lsupDevice/' + id,
    method: 'delete'
  })
}

// 云台控制（开始）
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

// 云台控制（结束）
export function ptzCtrlEnd(id) {
  return request({
    url: '/isup/lsupDevice/ptzCtrlEnd/' + id,
    method: 'get',
  })
}

// 云台控制（聚焦）
export function ptzCtrlFocus(id,controSpeed) {
  return request({
    url: '/isup/lsupDevice/ptzCtrlFocus/' + id,
    method: 'get',
    params: {
      controSpeed,
    }
  })
}

// 获取所有数字通道状态
export function getAllDigitalChannelStatus(id) {
  return request({
    url: '/isup/lsupDevice/getAllDigitalChannelStatus/' + id,
    method: 'get',
  })
}

