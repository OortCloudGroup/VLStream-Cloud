<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <el-form-item :label="i18nt('designer.setting.cellWidth')">
    <el-input v-model="optionModel.cellWidth" type="text" />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElInput } from 'element-plus'
export default {
  name: 'CellWidthEditor',
  components: {
    ElFormItem,
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
