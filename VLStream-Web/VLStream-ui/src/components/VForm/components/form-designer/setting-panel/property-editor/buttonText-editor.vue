<template>
  <el-form-item label="按钮文字">
    <el-input v-model="optionModel.buttonText" type="text" />
  </el-form-item>
</template>

<script>
import { ElFormItem, ElInput } from 'element-plus'

export default {
  name: 'ButtonTextEditor',
  components: {
    ElFormItem,
    ElInput
  },
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  }
}
</script>

<style lang="scss" scoped>

</style>
