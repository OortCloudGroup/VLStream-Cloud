/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-28 11:56:08
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-09-12 22:01:00
* @Copyright aPaaS-front-team. All rights reserved.
*/
import AddressSetingDialog from '@/components/personHome/addressSetingDialog.vue'
import ID2Name from '@/components/ID2Name.vue'
import ID2HeadPic from '@/components/ID2HeadPic.vue'
import DeptIdToName from '@/components/deptIdToName.vue'
import OortSvgIcon from '@/components/oortSvgIcon/index.vue'
import OortImg from '@/components/oort_img.vue'

// 实现按需引入
export {
  AddressSetingDialog,
  ID2Name,
  ID2HeadPic,
  DeptIdToName,
  OortSvgIcon,
  OortImg
}

const components = [
  { name: 'AddressSetingDialog', component: AddressSetingDialog },
  { name: 'ID2Name', component: ID2Name },
  { name: 'ID2HeadPic', component: ID2HeadPic },
  { name: 'DeptIdToName', component: DeptIdToName },
  { name: 'OortSvgIcon', component: OortSvgIcon },
  { name: 'OortImg', component: OortImg }
]

import { ElMessage } from 'element-plus'
const install = function(app) {
  components.forEach((item) => {
    app.component(item.name, item.component)
  })
  // 兼容this.$message的消息通知
  app.config.globalProperties.$message = ElMessage
}
// 实现批量引入
export default { install }

