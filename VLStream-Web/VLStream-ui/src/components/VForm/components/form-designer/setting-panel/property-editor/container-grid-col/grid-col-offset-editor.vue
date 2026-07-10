<template>
  <el-form-item :label="i18nt('designer.setting.colOffsetTitle')">
    <el-input-number
      v-model.number="optionModel.offset"
      :min="0"
      :max="24"
      style="width: 100%"
    />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElInputNumber } from 'element-plus'

export default {
  name: 'GridColOffsetEditor',
  components: {
    ElFormItem,
    ElInputNumber
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
