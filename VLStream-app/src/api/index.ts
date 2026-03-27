/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:50:27
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:50:27
* @Copyright aPaaS-front-team. All rights reserved.
*/
import { request } from '@/utils/service'

export const fetchData = () => {
  return request({
    url: './table.json',
    method: 'get'
  })
}
