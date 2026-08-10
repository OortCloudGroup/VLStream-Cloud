import request from '@/utils/wvpRequest'

// 查询大华设备列表（分页）
export function listDevice(query) {
  return request({
    url: '/dahua/device/list',
    method: 'get',
    params: query
  })
}


// 查询大华设备列表（不分页）
export function listDahuaDevice(query) {
  return request({
    url: '/dahua/device/listDahuaDevice',
    method: 'get',
    params: query
  })
}


// 查询大华设备详细
export function getDevice(id) {
  return request({
    url: '/dahua/device/' + id,
    method: 'get'
  })
}

// 新增大华设备
export function addDevice(data) {
  return request({
    url: '/dahua/device',
    method: 'post',
    data: data
  })
}

// 修改大华设备
export function updateDevice(data) {
  return request({
    url: '/dahua/device',
    method: 'put',
    data: data
  })
}

// 删除大华设备
export function delDevice(id) {
  return request({
    url: '/dahua/device/' + id,
    method: 'delete'
  })
}

// 大华设备实时预览
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

// 大华设备登录
export function dahuaLogin(data) {
  return request({
    url: '/dahua/device/login',
    method: 'post',
    data: data
  })
}

// 获取自动注册设备列表
export function getRegisterDeviceList() {
  return request({
    url: '/dahua/device/getRegisterDeviceList',
    method: 'get'
  })
}

// 大华设备云台控制（开始）
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

// 大华设备云台控制（停止）
export function ptzControlUpEnd(id,direction) {
    return request({
        url: '/dahua/device/ptzControlUpEnd/'+id,
        method: 'get',
        params: {
            direction,
        }
    })
}

// 大华设备抓图
export function snapPicture(id) {
  return request({
    url: '/dahua/device/snapPicture/'+id,
    method: 'get',
  })
}

// 查询大华设备抓图列表
export function listScreenshot(query) {
  return request({
    url: '/dahua/device/listScreenshot',
    method: 'get',
    params: query
  })
}

// 删除大华设备抓图
export function removeScreenshot(id) {
  return request({
    url: '/dahua/device/removeScreenshot/' + id,
    method: 'delete'
  })
}

// 大华设备定时抓图
export function timerCapturePicture(id,interval) {
  return request({
    url: '/dahua/device/timerCapturePicture/' + id,
    method: 'get',
    params: {
        interval,
    }
  })
}

// 大华设备停止定时抓图
export function stopCapturePicture(id) {
  return request({
    url: '/dahua/device/stopCapturePicture/' + id,
    method: 'get',
  })
}

// 大华设备获取时间
export function getTime(id) {
  return request({
    url: '/dahua/device/getTime/' + id,
    method: 'get',
  })
}

// 大华设备设置时间
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

// 大华设备重启
export function reboot(id) {
  return request({
    url: '/dahua/device/reboot/' + id,
    method: 'get',
  })
}

// 删除自动注册设备列表
export function delRegisterDevice(ips) {
  return request({
    url: '/dahua/device/delRegisterDevice/' + ips,
    method: 'delete',
  })
}

