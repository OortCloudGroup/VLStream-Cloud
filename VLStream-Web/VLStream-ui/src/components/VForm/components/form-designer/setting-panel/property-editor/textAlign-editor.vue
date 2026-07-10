<template>
  <el-form-item v-if="(selectedWidget.type === 'static-text')" :label="i18nt('designer.setting.textAlign')">
    <!-- eslint-disable-next-line vue/no-mutating-props -->
    <el-radio-group v-model="optionModel.textAlign" class="radio-group-custom">
      <el-radio-button label="left">
        {{ i18nt('designer.setting.leftAlign') }}
      </el-radio-button>
      <el-radio-button label="center">
        {{ i18nt('designer.setting.centerAlign') }}
      </el-radio-button>
      <el-radio-button label="right">
        {{ i18nt('designer.setting.rightAlign') }}
      </el-radio-button>
    </el-radio-group>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElRadioButton, ElRadioGroup } from 'element-plus'
export default {
  name: 'TextAlignEditor',
  components: {
    ElFormItem,
    ElRadioButton,
    ElRadioGroup
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  }
}
</script>

<style lang="scss" scoped>
  .radio-group-custom {
    :deep(.el-radio-button__inner ) {
      padding-left: 12px;
      padding-right: 12px;
    }
  }
</style>
