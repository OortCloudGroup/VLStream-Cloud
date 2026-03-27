<!-- eslint-disable vue/no-v-for-template-key -->
<template>
  <div
    v-show="!widget.options.hidden"
    v-bind="layoutProps"
    :key="widget.id"
    class="grid-cell"
    :class="[customClass]"
    :style="colHeightStyle"
  >
    <template v-if="!!widget.widgetList && widget.widgetList.length > 0">
      <template v-for="(subWidget, swIdx) in widget.widgetList" :key="swIdx">
        <component
          :is="'m-' + subWidget.type + '-widget'"
          :ref="`itemC${swIdx}`"
          :field="{...subWidget, ...{ required: field.options.required, disabled: field.options.disabled, readonly: field.options.readonly, clearable: field.options.clearable}}"
          :designer="null"
          :parent-list="widget.widgetList"
          :index-of-parent-list="indexOfParentList"
          :parent-widget="widget"
        />
      </template>
    </template>
  </div>
</template>

<script>
import { computed, toRefs, reactive, inject, getCurrentInstance } from 'vue'

import { useI18n } from '~@/utils/i18n'
import { useEmitter } from '~@/utils/emitter'
import { useRef } from '../../../components/form-render/refMixin'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget/index'

import { px2rem } from '~@/utils/util'
export default {
  componentName: 'ContainerItem',
  name: 'GridColItem',
  components: {
    ...FieldComponents
  },
  props: {
    colHeight: {
      default: null,
      type: String
    },
    indexOfParentList: Number,
    parentList: Array,
    parentWidget: Object,
    widget: Object,
    field: Object
  },
  setup(props) {
    console.log('props--------', props)
    // const refList = inject('refList')
    // const globalModel = inject('globalModel')
    const getFormConfig = inject('getFormConfig')

    const { proxy } = getCurrentInstance()
    const { i18nt } = useI18n()
    const refMixin = useRef()
    const data = reactive({
      layoutProps: {
      }
    })
    const emitterMixin = useEmitter(data)

    const formConfig = computed(() => {
      return getFormConfig()
    })

    const customClass = computed(() => {
      return props.widget.options.customClass || ''
    })

    const colHeightStyle = computed(() => {
      return !!props.colHeight ? { height: px2rem(props.colHeight) + 'px' } : {}
    })

    const initLayoutProps = () => {

    }

    const setDisabled = flag => {
      props.widget.widgetList.forEach((item, index) => {
        proxy.$refs[`itemC${index}`][0].setDisabled(flag)
      })
    }

    const setHidden = flag => {
      props.widget.widgetList.forEach((item, index) => {
        proxy.$refs[`itemC${index}`][0].setHidden(flag)
      })
    }

    initLayoutProps()
    refMixin.initRefList()

    return {
      i18nt,
      ...toRefs(data),
      ...emitterMixin,
      colHeightStyle,

      customClass,
      formConfig,
      scopedSlots: proxy.$slots,
      setDisabled,
      setHidden
    }
  }
}
</script>

<style lang="scss" scoped>
.blank-cell {
  font-style: italic;
  color: #cccccc;

  span.invisible-content {
    opacity: 0;
  }
}
</style>
