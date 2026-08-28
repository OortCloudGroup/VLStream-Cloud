<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<!--
/**
 * author: vformAdmin
 * email: vdpadmin@163.com
 * website: https://www.vform666.com
 * date: 2021.08.18
 * remark: if need to VForm , in info! !
 */
-->

<template>
  <div class="container-wrapper" :class="[customClass]">
    <slot />
  </div>
</template>

<script>
export default {
  name: 'ContainerItemWrapper',
  props: {
    widget: Object
  },
  computed: {
    customClass() {
      return !!this.widget.options.customClass ? this.widget.options.customClass.join(' ') : ''
    }

  }
}
</script>

<style lang="scss" scoped>

</style>
