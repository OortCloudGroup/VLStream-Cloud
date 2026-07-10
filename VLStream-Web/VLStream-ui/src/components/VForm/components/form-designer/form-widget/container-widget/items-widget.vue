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
  <container-wrapper
    :designer="designer"
    :widget="widget"
    :parent-widget="parentWidget"
    :parent-list="parentList"
    :index-of-parent-list="indexOfParentList"
  >
    <div
      :key="widget.id"
      class="item-container"
      :class="[selected ? 'selected' : '', customClass]"
      @click.stop="selectWidget(widget)"
    >
      <template v-for="(colWidget, colIdx) in widget.items" :key="colIdx">
        <items-item-widget
          :widget="colWidget"
          :designer="designer"
          :parent-list="widget.items"
          :index-of-parent-list="colIdx"
          :parent-widget="widget"
          :col-height="widget.options.colHeight"
        />
      </template>
      <el-button type="text" style="margin: 0 auto" icon="el-icon-plus">
        <span>添加明细</span>
      </el-button>
    </div>
  </container-wrapper>
</template>

<script>
import i18n from '~@/utils/i18n'
import ItemsItemWidget from '~@/components/form-designer/form-widget/container-widget/items-item-widget'
import containerMixin from '~@/components/form-designer/form-widget/container-widget/containerMixin'
import ContainerWrapper from '~@/components/form-designer/form-widget/container-widget/container-wrapper'
import refMixinDesign from '~@/components/form-designer/refMixinDesign'

export default {
  name: 'ItemsWidget',
  componentName: 'ContainerWidget',
  components: {
    ContainerWrapper,
    ItemsItemWidget
  },
  mixins: [i18n, containerMixin, refMixinDesign],
  inject: ['refList'],
  props: {
    widget: Object,
    parentWidget: Object,
    parentList: Array,
    indexOfParentList: Number,
    designer: Object
  },
  computed: {
    selected() {
      return this.widget.id === this.designer.selectedId
    },

    customClass() {
      return this.widget.options.customClass || ''
    }

  },
  watch: {
    //
  },
  created() {
    this.initRefList()
  },
  mounted() {
    //
  },
  methods: {
    addDetail() {
      // this.designer.addItemsItemWidget(this.widget)
      // 获取内部的组件
      console.log('itemsItem', this.widget)
    }
  }
}
</script>

  <style lang="scss" scoped>
    .item-container {
      display: flex;
      flex-direction: column;
      min-height: 50px;
      outline: 1px dashed #336699;
      padding: 10px;
      .form-widget-list {
        min-height: 28px;
      }
    }

    .item-container.selected, .grid-cell.selected {
      outline: 2px solid var(--el-color-primary) !important;
    }

  </style>
