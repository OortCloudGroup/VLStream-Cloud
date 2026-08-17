import request from '@/utils/wvpRequest'

export function getClassificationTree(categoryType, protocolType) {
  return request({
    url: '/device/classification/tree',
    method: 'get',
    params: { categoryType, protocolType }
  })
}

export function addClassificationCategory(data) {
  return request({ url: '/device/classification/category', method: 'post', data })
}

export function updateClassificationCategory(data) {
  return request({ url: '/device/classification/category', method: 'put', data })
}

export function deleteClassificationCategory(id) {
  return request({ url: `/device/classification/category/${id}`, method: 'delete' })
}

export function getDeviceClassification(protocolType, deviceKey) {
  return request({
    url: '/device/classification/assignments',
    method: 'get',
    params: { protocolType, deviceKey }
  })
}

export function saveDeviceClassification(data) {
  return request({ url: '/device/classification/assignments', method: 'put', data })
}

