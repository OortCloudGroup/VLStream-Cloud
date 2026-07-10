<template>
  <el-form-item label-width="0">
    <el-divider class="custom-divider-margin-top">
      链接设置
    </el-divider>
    <el-form-item label="链接地址">
      <el-input v-model="optionModel.hypeLinkOptions.linkUrl" style="width:120px" placeholder="链接地址" />
    </el-form-item>
    <el-form-item label="链接文字">
      <el-input v-model="optionModel.hypeLinkOptions.linkText" style="width:120px" placeholder="链接文字,不填为链接地址" />
    </el-form-item>
    <el-form-item label="显示类型">
      <el-select v-model="optionModel.hypeLinkOptions.linkType" style="width:120px">
        <el-option label="primary" value="primary" />
        <el-option label="success" value="success" />
        <el-option label="warning" value="warning" />
        <el-option label="danger" value="danger" />
        <el-option label="info" value="danger" />
        <el-option label="默认" value="default" />
      </el-select>
    </el-form-item>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElDivider, ElInput, ElSelect, ElOption } from 'element-plus'
export default {
  name: 'HypeLinkOptionsEditor',
  components: {
    ElFormItem,
    ElDivider,
    ElInput,
    ElSelect,
    ElOption
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  },
  created() {
  }
}
</script>

<style scoped>

</style>
