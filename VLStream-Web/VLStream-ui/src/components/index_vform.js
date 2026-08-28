/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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

//
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
  // // formrelated
  registerIcon(app)
  addDirective(app)
  installI18n(app)
  app.use(ContainerWidgets)
  app.use(ContainerItems)
  loadExtension(app)
}
//
export default { install }

