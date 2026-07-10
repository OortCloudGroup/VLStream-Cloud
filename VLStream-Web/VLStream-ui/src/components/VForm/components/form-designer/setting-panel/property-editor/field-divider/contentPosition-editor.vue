<template>
  <el-form-item :label="i18nt('designer.setting.contentPosition')">
    <el-select v-model="optionModel.contentPosition">
      <el-option label="center" value="center" />
      <el-option label="left" value="left" />
      <el-option label="right" value="right" />
    </el-select>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElSelect, ElOption } from 'element-plus'
export default {
  name: 'ContentPositionEditor',
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
