<!-- eslint-disable vue/no-v-for-template-key -->
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
  <container-wrapper :designer="designer" :widget="widget" :parent-widget="parentWidget" :parent-list="parentList" :index-of-parent-list="indexOfParentList" :class="[selected ? 'selected' : '']">
    <van-row :key="widget.id" :gutter="widget.options.gutter" class="grid-container" :class="[customClass]" @click.stop="selectWidget(widget)">
      <template v-for="(colWidget, colIdx) in widget.cols" :key="colWidget.id">
        <m-grid-col-widget
          :widget="colWidget"
          :designer="designer"
          :parent-list="widget.cols"
          :index-of-parent-list="colIdx"
          :parent-widget="widget"
          :col-height="widget.options.colHeight"
        />
      </template>
    </van-row>
  </container-wrapper>
</template>

<script>
import { computed } from 'vue'

import { px2rem } from '~@/utils/util'
import { useI18n } from '~@/utils/i18n'
import { useContainer } from '~@/components/form-designer/form-widget/container-widget/containerMixin'
import { useDesignRef } from '~@/components/form-designer/refMixinDesign'
import ContainerWrapper from '~@/components/form-designer/form-widget/container-widget/container-wrapper'

export default {
  componentName: 'ContainerWidget',
  name: 'MGridWidget',
  components: {
    ContainerWrapper
  },
  props: {
    designer: Object,
    indexOfParentList: Number,
    parentList: Array,
    parentWidget: Object,
    widget: Object
  },
  setup(props) {
    const { i18nt, i18n2t } = useI18n()
    const containerMixin = useContainer()
    const designRefMixin = useDesignRef()

    const selected = computed(() => {
      return props.widget.id === props.designer.selectedId
    })

    const customClass = computed(() => {
      return props.widget.options.customClass || ''
    })

    designRefMixin.initRefList()
    return {
      i18n2t,
      i18nt,
      ...containerMixin,

      customClass,
      px2rem,

      selected
    }
  }
}
</script>

<style lang="scss" scoped>
.van-row.grid-container {
  min-height: 38px;
  box-sizing: border-box;
  border: 1px dashed #336699;

  .form-widget-list {
    min-height: 28px;
  }
}
</style>
