<template>
  <div>
    <el-form-item style="width: 100%;" label="扫码类型">
      <el-select v-model="optionModel.scanType" class="item_width" placeholder="请选择扫码类型">
        <el-option
          v-for="item in sacnTypeOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </el-form-item>
  </div>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElSelect, ElOption } from 'element-plus'
export default {
  name: 'ScanTypeEditor',
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
      sacnTypeOptions: [
        { label: '二维码', value: 1 },
        { label: '条形码', value: 2 }
      ]
    }
  },
  created() {
  }
}
</script>

<style scoped>

</style>
