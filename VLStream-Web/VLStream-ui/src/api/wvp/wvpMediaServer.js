import request from '@/utils/wvpRequest'

// 查询媒体服务器列表
export function listWvpMediaServer(query) {
  return request({
    url: '/api/server/media_server/list',
    method: 'get',
    params: query
  })
}

// 查询媒体服务器详细
export function getWvpMediaServer(id) {
  return request({
    url: '/api/server/media_server/one/' + id,
    method: 'get'
  })
}

// 新增媒体服务器
export function saveWvpMediaServer(data) {
  return request({
    url: '/api/server/media_server/save',
    method: 'post',
    data: data
  })
}

// 修改媒体服务器
export function updateWvpMediaServer(data) {
  return request({
    url: '/wvp/wvpMediaServer',
    method: 'put',
    data: data
  })
}

// 删除媒体服务器
export function delWvpMediaServer(id) {
  return request({
    url: '/api/server/media_server/delete/' + id,
    method: 'delete'
  })
}

// 检查流媒体服务是否可用
export function checkMediaServer(query) {
  return request({
    url: '/api/server/media_server/check',
    method: 'get',
    params: query
  })
}

// 获取平台配置信息
export function configInfo() {
  return request({
    url: '/api/server/system/configInfo',
    method: 'get',
  })
}

// 获取流媒体服务列表
export function getOnlineMediaServerList() {
  return request({
    url: '/api/server/media_server/online/list',
    method: 'get',
  })
}

