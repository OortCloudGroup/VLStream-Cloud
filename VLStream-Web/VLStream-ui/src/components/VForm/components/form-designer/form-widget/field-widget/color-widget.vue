<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<template>
  <form-item-wrapper
    :designer="designer"
    :field="field"
    :rules="rules"
    :design-state="designState"
    :parent-widget="parentWidget"
    :parent-list="parentList"
    :index-of-parent-list="indexOfParentList"
    :sub-form-row-index="subFormRowIndex"
    :sub-form-col-index="subFormColIndex"
    :sub-form-row-id="subFormRowId"
  >
    <el-color-picker
      ref="fieldEditor"
      v-model="fieldModel"
      :size="widgetSize"
      :disabled="field.options.disabled"
      @change="handleChangeEvent"
    />
  </form-item-wrapper>
</template>

<script>
import FormItemWrapper from './form-item-wrapper'
import emitter from '~@/utils/emitter'
import i18n from '~@/utils/i18n'
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import { ElColorPicker } from 'element-plus'

export default {
  name: 'ColorWidget',
  componentName: 'FieldWidget',
  components: {
    ElColorPicker,
    FormItemWrapper
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
  data() {
    return {
      oldFieldValue: null, // fieldcomponentchange before value
      fieldModel: null,
      rules: []
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
    this.initFieldModel()
    this.registerToRefList()
    this.initEventHandler()
    this.buildFieldRules()

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
  @import "../../../../styles/global.scss"; /* form-item-wrapper已引入，还需要重复引入吗？ */

  .full-width-input {
    width: 100% !important;
  }

</style>
