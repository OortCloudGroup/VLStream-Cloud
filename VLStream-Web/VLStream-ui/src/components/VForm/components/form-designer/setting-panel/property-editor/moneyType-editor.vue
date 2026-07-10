<template>
  <el-form-item label="货币类型">
    <el-select v-model="optionModel.moneyType">
      <el-option
        v-for="item in moneyTypeOptions"
        :key="item.value"
        :label="item.label"
        :value="item.value"
      />
    </el-select>
  </el-form-item>
</template>

<script>
import { ElFormItem, ElSelect, ElOption } from 'element-plus'
export default {
  name: 'MoneyTypeEditor',
  components: {
    ElFormItem,
    ElSelect,
    ElOption
  },
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  },
  data() {
    return {
      moneyTypeOptions: [
        { label: '人民币', value: '￥' },
        { label: '港币', value: 'HK＄' },
        { label: '美元', value: '＄' },
        { label: '欧元', value: '€' }

      ]
    }
  }
}
</script>

<style scoped>

</style>
