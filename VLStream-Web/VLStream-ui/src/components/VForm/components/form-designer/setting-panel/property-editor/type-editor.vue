<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <el-form-item v-if="selectedWidget.type === 'input'" :label="i18nt('designer.setting.displayType')">
    <el-select v-model="optionModel.type">
      <el-option label="text" value="text" />
      <!-- input typeSet to number , if non- , v-model value is empty , method Validate ! ! ! -->
      <!--
      <el-option label="number" value="number"></el-option>
      -->
      <el-option label="password" value="password" />
    </el-select>
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElSelect, ElOption } from 'element-plus'
export default {
  name: 'TypeEditor',
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
  computed: {

  }
}
</script>

<style lang="scss" scoped>

</style>
