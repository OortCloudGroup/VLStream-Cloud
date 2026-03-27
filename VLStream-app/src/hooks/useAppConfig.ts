/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:54:23
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 10:54:23
* @Copyright aPaaS-front-team. All rights reserved.
*/
import Config from '@/config/index'
import { getToken } from '@/utils/cache'

export default function() {
  const initHeader = function() {
    Config.headers.AccessToken = getToken()
  }

  return {
    initHeader
  }
}

