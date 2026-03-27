<template>
  <!-- vant 清空图标 -->
  <van-icon ref="clear" name="close" @click.stop="clearFieldModel" />
</template>
<script setup>
import { ref } from 'vue'
import { findParentByComponentName } from '~@/utils/util'
const clear = ref(null)

const clearFieldModel = () => {
  const parentWidget = findParentByComponentName(clear.value, 'FieldWidget')
  if (parentWidget) {
    parentWidget.fieldModel = null
    parentWidget.syncUpdateFormModel(parentWidget.fieldModel)
    parentWidget.handleChangeEvent(parentWidget.fieldModel)
  }
}
</script>
