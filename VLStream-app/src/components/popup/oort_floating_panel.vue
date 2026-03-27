<!-- eslint-disable vue/no-deprecated-dollar-listeners-api -->
<!--
 * @Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-06-25 14:22:41
 * @Last Modified by:  兰舰
 * @Copyright 奥尔特云(深圳)智慧科技 aPaaS-front-team. All rights reserved.
!-->
<template>
  <van-floating-panel
    v-if="popupShow"
    v-bind="$attrs"
    v-model:height="height"
    class="oort-floating-panel"
    v-on="$listeners"
  >
    <slot v-if="destorySlotVis" />
  </van-floating-panel>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import bus from '@/utils/bus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  destroyOnClose: {
    type: Boolean,
    default: true
  },
  defaultHeight: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:modelValue', 'closed', 'moveHeightChange'])

const popupShow = ref(false)
const destorySlotVis = ref(true)

const height = ref(props.defaultHeight)

watch(() => height.value, (newVal) => {
  emit('moveHeightChange', newVal)
})

watch(
  () => props.modelValue,
  (newVal) => {
    popupShow.value = newVal
    // 恢复销毁的slot
    if (props.modelValue) {
      destorySlotVis.value = true
    }
    handlerPopupEvent()
  }
)

onMounted(() => {
  popupShow.value = props.modelValue
  handlerPopupEvent()
  // 这里做一个监听，用来使pop类的弹框出现在页面中时，点击物理返回键时会触发返回而不是
  bus.$on('popupHide', popShow)
  // 添加组件卸载前的清理
  onBeforeUnmount(() => {
    bus.$off('popupHide', popShow)
  })
})

const popShow = () => {
  popupShow.value = false
  // 同步更新modelValue
  emit('update:modelValue', false)
}

const handlerPopupEvent = () => {
  bus.$emit('popShow', popupShow.value)
}
</script>

