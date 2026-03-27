<!--
/**
 * author: vformAdmin
 * email: vdpadmin@163.com
 * website: https://www.vform666.com
 * date: 2021.08.18
 * remark: 如果要分发VForm源码，需在本文件顶部保留此文件头信息！！
 */
-->

<template>
  <div class="container-wrapper" :class="[customClass, widget.type, widget.options.name]">
    <slot />
  </div>
</template>

<script>
import { computed } from 'vue'
export default {
  name: 'ContainerItemWrapper',
  props: {
    widget: {
      required: true,
      type: Object
    }
  },
  setup(props) {
    const customClass = computed(() => {
      return !!props.widget.options.customClass ? props.widget.options.customClass.join(' ') : ''
    })

    return {
      customClass
    }
  }
}
</script>

<style lang="scss" scoped></style>
