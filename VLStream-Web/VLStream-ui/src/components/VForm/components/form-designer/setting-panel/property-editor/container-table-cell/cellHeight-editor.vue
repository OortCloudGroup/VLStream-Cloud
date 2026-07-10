<template>
  <el-form-item :label="i18nt('designer.setting.cellHeight')">
    <el-input v-model="optionModel.cellHeight" type="text" />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElSwitch, ElButton, ElInput } from 'element-plus'
export default {
  name: 'CellHeightEditor',
  components: {
    ElFormItem,
    ElSwitch,
    ElButton,
    ElInput
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
