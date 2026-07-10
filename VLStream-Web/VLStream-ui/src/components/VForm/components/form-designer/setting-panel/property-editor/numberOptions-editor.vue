<template>
  <div>
    <el-divider class="custom-divider-margin-top">
      数据格式
    </el-divider>
    <el-form-item style="width: 100%;" label="数值类型">
      <el-select v-model="optionModel.numberOptions.type" placeholder="请选择规则类型" style="width: 100%;">
        <el-option label=" 常规" :value="1" />
        <el-option label=" 小数" :value="2" />
        <el-option label=" 货币" :value="3" />
        <el-option label=" 百分比" :value="4" />
      </el-select>
    </el-form-item>
    <template v-if="optionModel.numberOptions.type !== 1">
      <el-form-item style="width: 100%;" label="小数位数">
        <el-input
          v-model="optionModel.numberOptions.decimalLength"
          type="number"
          class="hide-spin-button"
          min="1"
          style="width: 100%"
          @input="inputNumberHandler"
        />
      </el-form-item>
    </template>
    <template v-if="optionModel.numberOptions.type === 3">
      <el-form-item label="货币类型">
        <el-select v-model="optionModel.numberOptions.moneyType">
          <el-option
            v-for="item in moneyTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
    </template>
  </div>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElSelect, ElOption, ElDivider, ElInput } from 'element-plus'
export default {
  name: 'NumberOptionsEditor',
  components: {
    ElFormItem,
    ElSelect,
    ElOption,
    ElDivider,
    ElInput
  },
  mixins: [i18n],
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
  },
  created() {
    console.log(this.optionModel)
  }
}
</script>

<style scoped>

</style>
