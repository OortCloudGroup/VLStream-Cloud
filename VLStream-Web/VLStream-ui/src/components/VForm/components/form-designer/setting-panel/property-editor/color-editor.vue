<template>
  <el-form-item :label="i18nt('designer.setting.color')">
    <el-color-picker v-model="optionModel.color" />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElColorPicker } from 'element-plus'

export default {
  name: 'ColorEditor',
  components: {
    ElFormItem,
    ElColorPicker
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  },
  data() {
    return {
    }
  }
}
</script>

<style scoped>

</style>
