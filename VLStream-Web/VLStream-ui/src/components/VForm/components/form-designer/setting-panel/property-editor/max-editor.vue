<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <el-form-item :label="i18nt('designer.setting.maxValue')">
    <el-input-number v-model="maxValue" class="hide-spin-button" style="width: 100%" />
  </el-form-item>
</template>

<script>
import i18n from '~@/utils/i18n'
import { ElFormItem, ElInputNumber } from 'element-plus'
export default {
  name: 'MaxEditor',
  components: {
    ElFormItem,
    ElInputNumber
  },
  mixins: [i18n],
  props: {
    designer: Object,
    selectedWidget: Object,
    optionModel: Object
  },
  computed: {
    maxValue: {
      get() {
        return this.optionModel['max']
      },

      set(newValue) {
        if (!newValue || isNaN(newValue)) {
          this.optionModel.max = null
        } else {
          this.optionModel.max = Number(newValue)
        }
      }
    }

  }
}
</script>

<style scoped>

</style>
