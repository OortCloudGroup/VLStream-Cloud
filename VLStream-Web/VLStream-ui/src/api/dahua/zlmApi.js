import request from '@/utils/wvpRequest';

// 代理播放
export function proxyPlay(data) {
    return request({
        url: '/dahua/zlmApi/proxyPlay',
        method: 'post',
        data: data
    })
}


export function stopProxy(id) {
    return request({
        url: '/dahua/zlmApi/stopProxy/' + id,
        method: 'get'
    })
}

