<template>
  <el-form-item label-width="0">
    <el-divider class="custom-divider-margin-top">
      地址本设置
    </el-divider>
    <address-setting v-model="optionModel.addressSetting" :multiple="optionModel.multiple" />
  </el-form-item>
</template>

<script>
import { ElFormItem, ElDivider } from 'element-plus'
import AddressSetting from '../address-setting.vue'

export default {
  name: 'AddressSettingEditor',
  components: {
    ElFormItem,
    ElDivider,
    AddressSetting
  },
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
