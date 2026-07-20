import { request } from '@/utils/service'

/** 查询当前本地登录用户的常用语。 */
export function myOpinionList(data: Record<string, unknown>) {
  return request({
    url: '/api/v1/myOpinionList',
    method: 'post',
    data
  })
}

/** 保存当前本地登录用户的常用语。 */
export function myOpinionSave(data: Record<string, unknown>) {
  return request({
    url: '/api/v1/myOpinionSave',
    method: 'post',
    data
  })
}

/** 删除当前本地登录用户拥有的常用语。 */
export function myOpinionDel(data: Record<string, unknown>) {
  return request({
    url: '/api/v1/myOpinionDel',
    method: 'post',
    data
  })
}
