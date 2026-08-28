<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
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
    <el-input
      ref="fieldEditor"
      v-model="fieldModel"
      :disabled="field.options.disabled"
      :readonly="field.options.readonly"
      :size="widgetSize"
      class="hide-spin-button"
      :type="inputType"
      :show-password="field.options.showPassword"
      :placeholder="field.options.placeholder"
      :clearable="field.options.clearable"
      :minlength="field.options.minLength"
      :maxlength="field.options.maxLength"
      :show-word-limit="field.options.showWordLimit"
      :prefix-icon="field.options.prefixIcon"
      :suffix-icon="field.options.suffixIcon"
      @focus="handleFocusCustomEvent"
      @blur="handleBlurCustomEvent"
      @input="handleInputCustomEvent"
      @change="handleChangeEvent"
    />
  </form-item-wrapper>
</template>

<script>
import FormItemWrapper from './form-item-wrapper'
import emitter from '~@/utils/emitter'
import i18n from '~@/utils/i18n'
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import { calcRules } from '~@/utils/funcItems.js'
import eventBus from '~@/utils/event-bus'

export default {
  name: 'CalcMethodWidget',
  componentName: 'FieldWidget',
  components: {
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
      fieldModel: 0,
      rules: []
    }
  },
  computed: {
    inputType() {
      if (this.field.options.type === 'number') {
        return 'text' // input typeSet to number , if non- , v-model value is empty , method Validate ! ! !
      }
      return this.field.options.type
    }
  },
  watch: {
    'field.options.calcOptions.rules'() {
      this.calcResult()
    }
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
    // value
    this.calcResult()
    eventBus.$on('field-value-changed', (otherField) => {
      // event
      if (otherField.id !== this.field.id && !otherField.id.startsWith('calcmethod')) {
        this.calcResult()
      }
    })
  },

  beforeUnmount() {
    this.unregisterFromRefList()
  },

  methods: {
    calcResult() {
      let wgList = this.designer ? this.designer.widgetList : this.parentList
      // value
      wgList.forEach(element => {
        if (this.globalModel.formModel[element.id]) {
          element.options.defaultValue = this.globalModel.formModel[element.id]
        }
      })
      this.fieldModel = calcRules(this.field.options.calcOptions.rules, wgList)
      this.handleChangeEvent(this.fieldModel)
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "../../../../styles/global.scss"; /* form-item-wrapper已引入，还需要重复引入吗？ */

</style>
