/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-28 11:56:08
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-09-12 22:01:26
* @Copyright aPaaS-front-team. All rights reserved.
*/
import FlowChart from './processui/flowChart.vue'
import ChoosePerson from './processui/flowProp/components/choosePerson.vue'

import VFormDesigner from '~@/components/form-designer/index.vue'
import VFormRender from '~@/components/form-render/index.vue'
import ContainerWidgets from '~@/components/form-designer/form-widget/container-widget/index'
import ContainerItems from '~@/components/form-render/container-item/index'

import { addDirective } from '~@/utils/directive'
import { installI18n } from '~@/utils/i18n'
import { loadExtension } from '~@/extension/extension-loader'
import { registerIcon } from '~@/utils/el-icons'

import './VForm/styles/index.scss'
import './VForm/iconfont/iconfont.css'

// 实现按需引入
export {
  FlowChart,
  ChoosePerson,
  VFormDesigner,
  VFormRender
}

const components = [
  { name: 'FlowChart', component: FlowChart },
  { name: 'VFormDesigner', component: VFormDesigner },
  { name: 'VFormRender', component: VFormRender },
  { name: 'ChoosePerson', component: ChoosePerson }
]

const install = function(app) {
  components.forEach((item) => {
    app.component(item.name, item.component)
  })
  // // 表单相关
  registerIcon(app)
  addDirective(app)
  installI18n(app)
  app.use(ContainerWidgets)
  app.use(ContainerItems)
  loadExtension(app)
}
// 实现批量引入
export default { install }

