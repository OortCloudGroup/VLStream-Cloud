import wvpRequest from '@/utils/wvpRequest'

export function deptTreeSelect() {
  return wvpRequest({
    url: '/system/user/deptTree',
    method: 'get'
  })
}
