<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <el-form-item :label="i18nt('designer.setting.multiple')">
    <el-switch v-model="optionModel.multiple" />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import propertyMixin from '~@/components/form-designer/setting-panel/property-editor/propertyMixin'
import { ElFormItem, ElSwitch } from 'element-plus'
export default {
  name: 'CascaderMultipleEditor',
  components: {
    ElFormItem,
    ElSwitch
  },
  mixins: [i18n, propertyMixin],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  }
}
</script>

<style scoped>

</style>
