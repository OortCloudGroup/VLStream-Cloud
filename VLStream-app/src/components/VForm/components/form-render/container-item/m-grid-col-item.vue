<template>
  <van-col v-show="!widget.options.hidden" v-bind="layoutProps" :key="widget.id" class="grid-cell" :class="[customClass]" :style="colHeightStyle">
    <template v-if="!!widget.widgetList && widget.widgetList.length > 0">
      <template v-for="(subWidget, swIdx) in widget.widgetList">
        <template v-if="'container' === subWidget.category">
          <component
            :is="subWidget.type + '-item'"
            :key="swIdx"
            :widget="subWidget"
            :parent-list="widget.widgetList"
            :index-of-parent-list="swIdx"
            :parent-widget="widget"
            :sub-form-row-id="subFormRowId"
            :sub-form-row-index="subFormRowIndex"
            :sub-form-col-index="subFormColIndex"
          >
            <!-- 递归传递插槽！！！ -->
            <template v-for="slot in Object.keys($slots)" #[slot]="scope">
              <slot :name="slot" v-bind="scope" />
            </template>
          </component>
        </template>
        <template v-else>
          <component
            :is="subWidget.type + '-widget'"
            :key="swIdx"
            :field="subWidget"
            :designer="null"
            :parent-list="widget.widgetList"
            :index-of-parent-list="swIdx"
            :parent-widget="widget"
            :sub-form-row-id="subFormRowId"
            :sub-form-row-index="subFormRowIndex"
            :sub-form-col-index="subFormColIndex"
          >
            <!-- 递归传递插槽！！！ -->
            <template v-for="slot in Object.keys($slots)" #[slot]="scope">
              <slot :name="slot" v-bind="scope" />
            </template>
          </component>
        </template>
      </template>
    </template>
    <template v-else>
      <van-col>
        <div class="blank-cell">
          <span class="invisible-content">{{ i18nt('render.hint.blankCellContent') }}</span>
        </div>
      </van-col>
    </template>
  </van-col>
</template>

<script>
import { computed, toRefs, reactive, inject, getCurrentInstance } from 'vue'

import { useI18n } from '~@/utils/i18n'
import { useEmitter } from '~@/utils/emitter'
import { useRef } from '../../../components/form-render/refMixin'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget/index'
import { traverseFieldWidgetsOfContainer } from '~@/utils/util'

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
    subFormColIndex: {
      default: -1,
      /* 子表单组件列索引，从0开始计数 */
      type: Number
    },

    subFormRowId: {
      default: '',
      /* 子表单组件行Id，唯一id且不可变 */
      type: String
    },
    subFormRowIndex: {
      default: -1,
      /* 子表单组件行索引，从0开始计数 */
      type: Number
    },
    widget: Object
  },
  setup(props) {
    // const refList = inject('refList')
    // const globalModel = inject('globalModel')
    const getFormConfig = inject('getFormConfig')
    const previewState = inject('previewState')

    const { proxy } = getCurrentInstance()
    const { i18nt } = useI18n()
    const refMixin = useRef()
    const data = reactive({
      layoutProps: {
        md: props.widget.options.md || 12,
        offset: props.widget.options.offset || 0,
        pull: props.widget.options.pull || 0,
        push: props.widget.options.push || 0,
        sm: props.widget.options.sm || 12,
        span: props.widget.options.span,
        xs: props.widget.options.xs || 12
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
      if (!!props.widget.options.responsive) {
        if (!!previewState) {
          data.layoutProps.md = undefined
          data.layoutProps.sm = undefined
          data.layoutProps.xs = undefined

          let lyType = formConfig.value.layoutType
          if (lyType === 'H5') {
            data.layoutProps.span = props.widget.options.xs || 12
          } else if (lyType === 'Pad') {
            data.layoutProps.span = props.widget.options.sm || 12
          } else {
            data.layoutProps.span = props.widget.options.md || 12
          }
        } else {
          data.layoutProps.span = undefined
        }
      } else {
        data.layoutProps.md = undefined
        data.layoutProps.sm = undefined
        data.layoutProps.xs = undefined
      }
    }

    const setDisabled = flag => {
      let fwHandler = fieldWidget => {
        let fwName = fieldWidget.options.name
        let fwRef = refMixin.getWidgetRef(fwName)
        if (!!fwRef && !!fwRef.setDisabled) {
          fwRef.setDisabled(flag)
        }
      }

      traverseFieldWidgetsOfContainer(props.widget, fwHandler)
    }

    const setHidden = flag => {
      // eslint-disable-next-line vue/no-mutating-props
      props.widget.options.hidden = flag

      /* 容器被隐藏后，需要同步清除容器内部字段组件的校验规则 */
      let clearRulesFn = fieldWidget => {
        let fwName = fieldWidget.options.name
        let fwRef = refMixin.getWidgetRef(fwName)
        if (flag && !!fwRef && !!fwRef.clearFieldRules) {
          fwRef.clearFieldRules()
        }

        if (!flag && !!fwRef && !!fwRef.buildFieldRules) {
          fwRef.buildFieldRules()
        }
      }

      traverseFieldWidgetsOfContainer(props.widget, clearRulesFn)
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
