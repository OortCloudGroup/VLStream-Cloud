import request from '@/utils/wvpRequest'

// 录像信息查询
export function recordinfo(query) {
    return request({
        url: `/api/gb_record/query`,
        method: 'get',
        params: query,
        timeout: 1000 * 60
    })
}

// 录像下载
export function download(query) {
    return request({
        url: `/api/gb_record/download/start`,
        method: 'get',
        params: query,
        timeout: 1000 * 60
    })
}

export function progress(query) {
    return request({
        url: `/api/gb_record/download/progress/${query.deviceId}/${query.channelId}/${query.stream}`,
        method: 'get',
        timeout: 1000 * 60
    })
}

export function progressStop(query) {
    return request({
        url: `/api/gb_record/download/stop/${query.deviceId}/${query.channelId}/${query.stream}`,
        method: 'get',
        timeout: 1000 * 60
    })
}

