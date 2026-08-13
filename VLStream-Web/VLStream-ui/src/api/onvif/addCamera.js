import request from '@/utils/wvpRequest'

// 探测摄像头
export function probe(query){
    return request({
        url: '/onvif/service/getInfo',
        method: 'get',
        params: query
    })
}

