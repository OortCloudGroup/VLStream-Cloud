
<!-- eslint-disable vue/no-v-for-template-key -->
<template>
  <div
    v-show="!widget.options.hidden"
    v-bind="layoutProps"
    :key="widget.id"
    class="grid-cell"
    :style="colHeightStyle"
  >
    <template v-if="!!widget.widgetList && (widget.widgetList.length > 0)">
      <template v-for="(subWidget, swIdx) in widget.widgetList" :key="swIdx">
        <div class="form-widget-list">
          <component
            :is="subWidget.type + '-widget'"
            :ref="`itemC${swIdx}`"
            :key="subWidget.id"
            :field="{...subWidget, ...{ required: field.options.required, disabled: field.options.disabled, readonly: field.options.readonly, clearable: field.options.clearable}}"
            :designer="designer"
            :parent-list="widget.widgetList"
            :index-of-parent-list="indexOfParentList"
            :parent-widget="widget"
            :design-state="true"
          />
        </div>
      </template>
    </template>
  </div>
</template>

<script>
import emitter from '~@/utils/emitter'
import i18n from '../../../utils/i18n'
import refMixin from '../../../components/form-render/refMixin'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget/index'

export default {
  name: 'ItemC',
  componentName: 'ContainerItem',
  components: {
    ...FieldComponents
  },
  mixins: [emitter, i18n, refMixin],
  inject: ['refList', 'sfRefList', 'globalModel'],
  props: {
    widget: Object,
    field: Object,
    parentWidget: Object,
    parentList: Array,
    indexOfParentList: Number,

    colHeight: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      layoutProps: {
        span: this.widget.options.span || 24,
        offset: this.widget.options.offset || 0,
        push: this.widget.options.push || 0,
        pull: this.widget.options.pull || 0
      }
    }
  },
  computed: {
    formConfig() {
      return this.getFormConfig()
    },

    customClass() {
      return this.widget.options.customClass || ''
    },

    colHeightStyle() {
      return !!this.colHeight ? { height: this.colHeight + 'px' } : {}
    }
  },

  created() {
    this.initRefList()
    this.initLayoutProps()
  },
  methods: {
    setDisabled(flag = true) {
      this.widget.widgetList.forEach((item, index) => {
        this.$refs[`itemC${index}`][0].setDisabled(flag)
      })
    },
    initLayoutProps() {
    }
  }
}
</script>

<style lang="scss" scoped>
  .form-widget-list {
    display: flex;
    flex-direction: column;
  }
</style>
