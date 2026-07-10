<template>
  <div>
    <el-form-item style="width: 100%;" label="计算公式">
      <CalcOptionsSetting v-model="optionModel.calcOptions.rules" :designer="designer" />
    </el-form-item>
  </div>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem } from 'element-plus'
import CalcOptionsSetting from '~@/components/form-designer/setting-panel/calc-options-setting'
export default {
  name: 'CalcOptionsEditor',
  components: {
    ElFormItem,
    CalcOptionsSetting
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
  },
  created() {
  }
}
</script>

<style scoped>

</style>
