/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import AddressSetingDialog from '@/components/personHome/addressSetingDialog.vue'
import ID2Name from '@/components/ID2Name.vue'
import ID2HeadPic from '@/components/ID2HeadPic.vue'
import DeptIdToName from '@/components/deptIdToName.vue'
import OortSvgIcon from '@/components/oortSvgIcon/index.vue'
import OortImg from '@/components/oort_img.vue'

//
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
  // this.$message notification
  app.config.globalProperties.$message = ElMessage
}
//
export default { install }

