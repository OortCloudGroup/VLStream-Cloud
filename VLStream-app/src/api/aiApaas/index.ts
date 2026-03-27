/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:45:18
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-04-12 17:06:18
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T, K>(interfaceName: string, data: T, method: string) {
  return request<K>({
    url: config.URL + config.gateWay + 'apaas-ai/' + interfaceName,
    method: method,
    data: data
  })
}

// ai 生成文本
export function textCompletion(data) {
  return commonFunc('api/v1/text_completion', data, 'post')
}

// ai 生成图片
export function imageGeneration(data) {
  return commonFunc('api/v1/text_img', data, 'post')
}

// 获取AI 文生图结果
export function getTextImage(data) {
  return commonFunc('api/v1/text_img_state', data, 'post')
}

// 获取base64的图片链接
export function textImageDownload(data) {
  return commonFunc('api/v1/text_img_download', data, 'post')
}
