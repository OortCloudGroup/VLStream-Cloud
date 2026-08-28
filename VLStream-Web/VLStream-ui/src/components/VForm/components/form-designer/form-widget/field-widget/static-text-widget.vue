<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <static-content-wrapper
    :designer="designer"
    :field="field"
    :design-state="designState"
    :parent-widget="parentWidget"
    :parent-list="parentList"
    :index-of-parent-list="indexOfParentList"
    :sub-form-row-index="subFormRowIndex"
    :sub-form-col-index="subFormColIndex"
    :sub-form-row-id="subFormRowId"
  >
    <div
      ref="fieldEditor"
      :style="(!!field.options.fontSize ? `font-size: ${field.options.fontSize};`: '')
        + ';height:100%;' + (!!field.options.height?'line-height:'+field.options.height+'px;':'')"
    >
      <pre
        :style="'color:' + field.options.color +';white-space' + (!!field.options.preWrap ? 'pre-wrap' : 'pre' ) +';width:100%'
          +';text-align:'+ (!!field.options.textAlign ? field.options.textAlign : 'left') +';font-weight:'+(!!field.options.fontWeight ? field.options.fontWeight : '100') "
      >{{ field.options.textContent }}</pre>
    </div>
  </static-content-wrapper>
</template>

<script>
import StaticContentWrapper from './static-content-wrapper'
import emitter from '~@/utils/emitter'
import i18n, { translate } from '~@/utils/i18n'
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'

export default {
  name: 'StaticTextWidget',
  componentName: 'FieldWidget',
  components: {
    StaticContentWrapper
  }, // to FieldWidget, component broadcastevent
  mixins: [emitter, fieldMixin, i18n],
  props: {
    field: Object,
    parentWidget: Object,
    parentList: Array,
    indexOfParentList: Number,
    designer: Object,

    designState: {
      type: Boolean,
      default: false
    },

    subFormRowIndex: { /* 子表单组件行索引，从0开始计数 */
      type: Number,
      default: -1
    },
    subFormColIndex: { /* 子表单组件列索引，从0开始计数 */
      type: Number,
      default: -1
    },
    subFormRowId: { /* 子表单组件行Id，唯一id且不可变 */
      type: String,
      default: ''
    }

  },
  computed: {

  },
  beforeCreate() {
    /* can method and property! ! */
  },

  created() {
    /* : sub componentmounted in componentcreated after、 componentmounted before , sub componentmounted need to prop
         需要在父组件created中初始化！！ */
    this.registerToRefList()
    this.initEventHandler()

    this.handleOnCreated()
  },

  mounted() {
    this.handleOnMounted()
  },

  beforeUnmount() {
    this.unregisterFromRefList()
  },

  methods: {

  }

}
</script>

<style lang="scss" scoped>
  @import "../../../../styles/global.scss"; // * static-content-wrapper already , need to ? *//

</style>
