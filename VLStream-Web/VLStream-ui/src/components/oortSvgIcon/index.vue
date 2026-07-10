<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <svg
    :class="{'icon_svg':!color}"
    :width="selfWidth"
    :height="selfHeight"
  >
    <use :xlink:href="`#icon-${name}`" :fill="color" />
  </svg>
</template>

<script setup lang="ts">

import { inject, computed } from 'vue'

import { clacPXToVW } from '@/utils/index'
const props = defineProps<{
      name: string,
      width?: string,
      height?: string,
      color?: string
    }>()

const tag = inject('tag')
const inXorkWeb = computed(() => tag === 'XorkWeb')

const selfWidth = clacPXToVW(parseInt(props.width || 24), inXorkWeb.value ? 1440 : 1920)
const selfHeight = clacPXToVW(parseInt(props.height || 24), inXorkWeb.value ? 1440 : 1920)

</script>

<style lang="scss" scoped>
    .icon_svg {
        fill: var(--el-color-primary)!important;
        outline: none!important;
    }

</style>
