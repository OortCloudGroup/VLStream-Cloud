import request from '@/utils/wvpRequest'

// 获取通道信息
export function getCommonChannel(id) {
    return request({
        url: `/api/common/channel/one/${id}`,
        method: 'get',
    })
}

// 获取通道信息列表
export function getIndustryCodeList() {
    return request({
        url: `/api/common/channel/industry/list`,
        method: 'get',
    })
}


// 获取通道类型列表
export function getDeviceTypeList() {
    return request({
        url: `/api/common/channel/type/list`,
        method: 'get',
    })
}

// 获取网络标识列表
export function getNetworkIdentificationTypeList() {
    return request({
        url: `/api/common/channel/network/identification/list`,
        method: 'get',
    })
}

// 重置通道
export function resetChannel(id) {
    return request({
        url: `/api/common/channel/reset/${id}`,
        method: 'post',
    })
}

// 修改通道信息
export function updateChannelData(data) {
    return request({
        url: `/api/common/channel/update`,
        method: 'post',
        data: data,
    })
}

// 通知设备上传媒体流
export function sendDevicePush(params) {
    return request({
        url: `/api/play/start/${params.deviceId}/${params.channelId}`,
        method: 'get',
        timeout: 10000 * 6
    })
}

// 新增通道信息
export function addChannelData(data) {
    return request({
        url: `/api/common/channel/add`,
        method: 'post',
        data: data,
    })
}

// 获取通道列表
export function queryListByCivilCode(query) {
    return request({
        url: `/api/common/channel/civilcode/list`,
        method: 'get',
        params: query
    })
}


// 根据ParentId获取通道列表
export function queryListByParentId(query) {
    return request({
        url: `/api/common/channel/parent/list`,
        method: 'get',
        params: query
    })
}

// 获取流信息
export function getServerMediaInfo(query) {
    return request({
        url: `/api/server/media_server/media_info`,
        method: 'get',
        params: query
    })
}

// 云台控制
export function getPtzCamera(url, query) {
    return request({
        url: '/api/front-end/ptz/' + url.deviceId + '/' + url.channelId,
        method: 'get',
        params: query
    })
}

// 云台控制 - 聚焦
export function getFocusCamera(url, query) {
    return request({
        url: '/api/front-end/fi/focus/' + url.deviceId + '/' + url.channelId,
        method: 'get',
        params: query
    })
}
// 云台控制 - 聚焦
export function getIrIsCamera(url, query) {
    return request({
        url: '/api/front-end/fi/iris/' + url.deviceId + '/' + url.channelId,
        method: 'get',
        params: query
    })
}
// 云台控制 - 预置位列表查询
export function gotoPresetList(url) {
    return request({
        url: '/api/front-end/preset/query/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get'
    })
}
// 云台控制 - 预置位新增
export function getAddPreset(url, query) {
    return request({
        url: '/api/front-end/preset/add/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}
// 云台控制 - 调用预置位
export function callPreset(url, query) {
    return request({
        url: '/api/front-end/preset/call/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}
// 云台控制 - 删除预置位
export function deletePreset(url, query) {
    return request({
        url: '/api/front-end/preset/delete/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// 云台控制 - 巡航组新增
export function GetAddCruisePoint(url, query) {
    return request({
        url: '/api/front-end/cruise/point/add/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// 云台控制 - 巡航组删除
export function GetDeleteCruisePoint(url, query) {
    return request({
        url: '/api/front-end/cruise/point/delete/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// 云台控制 - 设置扫描速度
export function GetSetSpeed(url, query) {
    return request({
        url: '/api/front-end/scan/set/speed/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// 云台控制 - 设置左边界
export function GetSetScanLeft(url, query) {
    return request({
        url: '/api/front-end/scan/set/left/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// 云台控制 - 开始自动扫描
export function GetStartScan(url, query) {
    return request({
        url: '/api/front-end/scan/start/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}


// 云台控制 - 停止自动扫描
export function GetStopScan(url, query) {
    return request({
        url: '/api/front-end/scan/stop/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// 云台控制 - 设置右边界
export function GetSetScanRight(url, query) {
    return request({
        url: '/api/front-end/scan/set/right/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// 云台控制 - 雨刷开关
export function GetPtzWiper(url, query) {
    return request({
        url: '/api/front-end/wiper/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// 云台控制 - 辅助功能
export function GetPtzSwitch(url, query) {
    return request({
        url: '/api/front-end/auxiliary/' + url.deviceId + '/' + url.channelDeviceId,
        method: 'get',
        params: query
    })
}

// 云台控制 - 语音对讲
export function GetBroadcast(url, query) {
    return request({
        url: '/api/play/broadcast/' + url.deviceId + '/' + url.channelId,
        method: 'get',
        params: query
    })
}

// 添加通道
export function addChannelToRegion(data) {
    return request({
        url: `/api/common/channel/region/add`,
        method: 'post',
        data: data
    })
}

// 删除通道
export function deleteChannelToRegion(data) {
    return request({
        url: `/api/common/channel/region/delete`,
        method: 'post',
        data: data
    })
}

// 删除通道
export function deleteChannelToGroup(data) {
    return request({
        url: `/api/common/channel/group/delete`,
        method: 'post',
        data: data
    })
}

// 添加通道信息
export function addChannelToGroup(data) {
    return request({
        url: `/api/common/channel/group/add`,
        method: 'post',
        data: data
    })
}


