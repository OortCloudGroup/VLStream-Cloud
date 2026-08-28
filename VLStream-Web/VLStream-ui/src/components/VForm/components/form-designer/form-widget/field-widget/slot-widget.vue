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
    <div :class="[!!designState ? 'slot-wrapper-design' : 'slot-wrapper-render']">
      <!-- -->
      <slot :name="field.options.name" :form-model="formModel" />
      <!-- -->
      <!-- slot :name="field.options.name"></slot -->
      <div v-if="!!designState" class="slot-title">
        {{ field.options.label }}
      </div>
    </div>
  </static-content-wrapper>
</template>

<script>
import StaticContentWrapper from './static-content-wrapper'
import emitter from '~@/utils/emitter'
import i18n, { translate } from '~@/utils/i18n'
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'

export default {
  name: 'SlotWidget',
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
  .slot-wrapper-design {
    width: 100%;
    min-height: 26px;
    background: linear-gradient(45deg, #ccc 25%, #eee 0, #eee 50%, #ccc 0, #ccc 75%, #eee 0);
    background-size: 20px 20px;
    text-align: center;

    .slot-title {
      font-size: 13px;
    }
  }

  .slot-wrapper-render {

  }

</style>
