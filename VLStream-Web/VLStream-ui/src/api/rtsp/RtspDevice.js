import request from '@/utils/wvpRequest'

// 查询rtsp设备列表
export function listRtspDevice(query) {
    return request({
        url: '/rtsp/RtspDevice/list',
        method: 'get',
        params: query
    })
}

// 查询rtsp设备列表
export function rtspDeviceList(query) {
  return request({
    url: '/rtsp/RtspDevice/rtspDeviceList',
    method: 'get',
    params: query
  })
}


// 查询rtsp设备详细
export function getRtspDevice(id) {
    return request({
        url: '/rtsp/RtspDevice/' + id,
        method: 'get'
    })
}

// 新增rtsp设备
export function addRtspDevice(data) {
    return request({
        url: '/rtsp/RtspDevice',
        method: 'post',
        data: data
    })
}

// 修改rtsp设备
export function updateRtspDevice(data) {
    return request({
        url: '/rtsp/RtspDevice',
        method: 'put',
        data: data
    })
}

// 删除rtsp设备
export function delRtspDevice(id) {
    return request({
        url: '/rtsp/RtspDevice/' + id,
        method: 'delete'
    })
}

export function addDetection(data) {
    return request({
        url: '/rtsp/RtspDevice/addDetection',
        method: 'post',
        data: data
    })
}

export function stopDetection(data) {
    return request({
        url: '/rtsp/RtspDevice/stopDetection',
        method: 'post',
        data: data
    })
}

export function alarmClockRtspDevice(query) {
    return request({
        url: '/rtsp/RtspDevice/alarmClock',
        method: 'get',
        params: query
    })
}

