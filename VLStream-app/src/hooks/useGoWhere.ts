/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:54:27
 * @Last Modified by: 兰舰
 * @Last Modified time: 2024-12-24 22:01:36
* @Copyright aPaaS-front-team. All rights reserved.
*/
import config from '@/config/index'

export default function() {
  const goWhere = function() {
    if (!!config.notforcedLogin) return
    window?.androidFunUtils.tokenOverdue()
  }
  return {
    goWhere
  }
}

