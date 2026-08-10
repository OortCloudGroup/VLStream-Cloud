import request from '@/utils/wvpRequest'

// 停止回放
export function playStop(query) {
    return request({
        url: `/api/playback/stop/${query.deviceId}/${query.channelId}/${query.streamId}`,
        method: 'get',
    })
}

// 开始回放
export function start(query) {
    return request({
        url: `/api/playback/start`,
        method: 'get',
        params: query,
    })
}

