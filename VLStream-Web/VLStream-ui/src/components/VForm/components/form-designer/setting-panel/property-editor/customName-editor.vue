<!-- eslint-disable vue/no-mutating-props -->
<template>
  <el-form-item prop="customName" :rules="nameRequiredRule">
    <template #label>
      <span>{{ i18nt('designer.setting.customName') }}
        <el-tooltip effect="light" :content="i18nt('designer.setting.editNameHelp')">
          <svg-icon icon-class="el-info" /></el-tooltip>
      </span>
    </template>
    <el-input v-model="optionModel.customName" type="text" :readonly="widgetNameReadonly" @change="updateWidgetNameAndRef" />
  </el-form-item>
</template>

<!-- eslint-disable vue/no-mutating-props -->
<script>
import i18n from '~@/utils/i18n'
import { isEmptyStr } from '~@/utils/util'
import SvgIcon from '~@/components/svg-icon/index'
import { ElFormItem, ElTooltip } from 'element-plus'

export default {
  name: 'CustomNameEditor',
  components: {
    ElFormItem,
    ElTooltip,
    SvgIcon
  },
  mixins: [i18n],
  inject: ['serverFieldList', 'getDesignerConfig'],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  },
  data() {
    return {
      nameRequiredRule: [{ required: false, message: 'name required' }]
    }
  },
  computed: {
    noFieldList() {
      return !this.serverFieldList || (this.serverFieldList.length <= 0)
    },

    widgetNameReadonly() {
      return !!this.getDesignerConfig().widgetNameReadonly
    }

  },
  methods: {
    updateWidgetNameAndRef(newName) {
      let oldName = this.designer.selectedWidgetName
      if (isEmptyStr(newName)) {
        this.selectedWidget.options.name = oldName
        this.$message.info(this.i18nt('designer.hint.customNameRequired'))
        return
      }

      if (!!this.designer.formWidget) {
        let foundRef = this.designer.formWidget.getWidgetRef(newName) // 检查newName是否已存在！！
        if (!!foundRef) {
          this.selectedWidget.options.customName = oldName
          this.$message.info(this.i18nt('designer.hint.duplicateName') + newName)
          return
        }

        let widgetInDesign = this.designer.formWidget.getWidgetRef(oldName)
        if (!!widgetInDesign && !!widgetInDesign.registerToRefList) {
          widgetInDesign.registerToRefList(oldName) // 注册组件新的ref名称并删除老的ref！！
          let newLabel = this.getLabelByFieldName(newName)
          this.designer.updateSelectedWidgetNameAndLabel(this.selectedWidget, newName, newLabel)
        }
      }
    },

    getLabelByFieldName(fieldName) {
      for (let i = 0; i < this.serverFieldList.length; i++) {
        if (this.serverFieldList[i].customName === fieldName) {
          return this.serverFieldList[i].label
        }
      }

      return null
    }

  }
}
</script>

<style lang="scss" scoped>

</style>
