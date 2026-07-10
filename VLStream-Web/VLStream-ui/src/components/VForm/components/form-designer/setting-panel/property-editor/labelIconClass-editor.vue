<template>
  <div>
    <el-form-item label-width="0">
      <el-divider class="custom-divider">
        {{ i18nt('designer.setting.customLabelIcon') }}
      </el-divider>
    </el-form-item>
    <el-form-item :label="i18nt('designer.setting.labelIconClass')">
      <el-input v-model="optionModel.labelIconClass" type="text" />
    </el-form-item>
  </div>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElDivider, ElInput } from 'element-plus'
export default {
  name: 'LabelIconClassEditor',
  components: {
    ElFormItem,
    ElDivider,
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
