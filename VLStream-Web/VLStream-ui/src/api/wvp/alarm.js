import request from '@/utils/wvpRequest'

// 查询报警列表
export function listAlarm(query) {
  return request({
    url: '/api/alarm/all',
    method: 'get',
    params: query
  })
}

// 删除报警
export function delAlarm(alarmIds) {
  return request({
    url: '/api/alarm/delete/' + alarmIds,
    method: 'delete'
  })
}

