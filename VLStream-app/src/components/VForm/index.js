import { registerIcon } from '~@/utils/el-icons'
import 'virtual:svg-icons-register'

import ContainerWidgets from '~@/components/form-designer/form-widget/container-widget/index'
import ContainerItems from '~@/components/form-render/container-item/index'
import VmFormRender from '~@/components/form-render/index'

import { installI18n } from '~@/utils/i18n'
import { installMessage } from '~@/utils/message'

export default {
  install(app) {
    app.use(ContainerWidgets)
    app.use(ContainerItems)
    installMessage(app)
    installI18n(app)
    registerIcon(app)
    app.component('VmFormRender', VmFormRender)
  }
}
