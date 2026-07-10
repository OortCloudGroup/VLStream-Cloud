<template>
  <el-form-item :label="i18nt('designer.setting.controlsPosition')">
    <el-select v-model="optionModel.controlsPosition">
      <el-option label="default" value="" />
      <el-option label="right" value="right" />
    </el-select>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElSelect, ElOption } from 'element-plus'
export default {
  name: 'ControlsPositionEditor',
  components: {
    ElFormItem,
    ElSelect,
    ElOption
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
