<template>
  <el-form-item :label="i18nt('designer.setting.widgetSize')">
    <el-select v-model="optionModel.size">
      <el-option
        v-for="item in widgetSizes"
        :key="item.value"
        :label="item.label"
        :value="item.value"
      />
    </el-select>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElSelect, ElOption } from 'element-plus'
export default {
  name: 'SizeEditor',
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
  },
  data() {
    return {
      widgetSizes: [
        { label: '默认', value: '' },
        { label: '大', value: 'large' },
        // {label: 'medium', value: 'medium'},
        { label: '小', value: 'small' }
        // {label: 'mini', value: 'mini'},
      ]
    }
  }
}
</script>

<style scoped>

</style>
