<template>
  <el-form-item :label="i18nt('designer.setting.plain')">
    <el-switch v-model="optionModel.plain" />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElSwitch } from 'element-plus'
export default {
  name: 'PlainEditor',
  components: {
    ElFormItem,
    ElSwitch
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
