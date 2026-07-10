<!-- eslint-disable vue/no-mutating-props -->
<template>
  <el-form-item :label="i18nt('designer.setting.displayType')">
    <el-select v-model="optionModel.datetype">
      <el-option label="日期时间" value="datetime" />
      <el-option label="日期" value="date" />
      <!-- <el-option label="dates" value="dates" /> -->
      <el-option label="年" value="year" />
      <el-option label="月" value="month" />
      <el-option label="周" value="week" />
    </el-select>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElSelect, ElOption } from 'element-plus'
export default {
  name: 'DateTypeEditor',
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
