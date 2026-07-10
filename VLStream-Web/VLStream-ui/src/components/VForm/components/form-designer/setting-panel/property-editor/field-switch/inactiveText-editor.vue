<template>
  <el-form-item :label="i18nt('designer.setting.inactiveText')">
    <el-input v-model="optionModel.inactiveText" />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElInput } from 'element-plus'

export default {
  name: 'InactiveTextEditor',
  components: {
    ElFormItem,
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
