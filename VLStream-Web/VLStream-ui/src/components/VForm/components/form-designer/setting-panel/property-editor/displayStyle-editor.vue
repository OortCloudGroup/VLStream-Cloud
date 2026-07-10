<template>
  <el-form-item :label="i18nt('designer.setting.displayStyle')">
    <el-radio-group v-model="optionModel.displayStyle">
      <el-radio value="inline">
        {{ i18nt('designer.setting.inlineLayout') }}
      </el-radio>
      <el-radio value="block">
        {{ i18nt('designer.setting.blockLayout') }}
      </el-radio>
    </el-radio-group>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElRadio, ElRadioGroup } from 'element-plus'

export default {
  name: 'DisplayStyleEditor',
  components: {
    ElFormItem,
    ElRadio,
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

<style scoped>

</style>
