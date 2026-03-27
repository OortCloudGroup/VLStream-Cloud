<template>
  <container-item-wrapper :widget="widget">
    <div v-show="!widget.options.hidden" :key="widget.id" class="tab-container">
      <van-tabs
        :ref="widget.id"
        v-model:active="activeTabName"
        :class="[customClass]"
        :type="tabDisplayType"
        :color="widget.options.colorStyle"
        :ellipsis="widget.options.textEllipsis"
        :swipeable="true"
        :lazy-render="false"
        @change="handleTabbarChange"
      >
        <van-tab v-for="(tab, index) in visibleTabs" :key="index" :title="tab.options.label" :disabled="tab.options.disabled" :name="tab.options.name">
          <template v-for="(subWidget, swIdx) in tab.widgetList">
            <template v-if="'container' === subWidget.category">
              <component
                :is="subWidget.type + '-item'"
                :key="swIdx"
                :widget="subWidget"
                :parent-list="tab.widgetList"
                :index-of-parent-list="swIdx"
                :parent-widget="widget"
                :sub-form-row-id="subFormRowId"
                :sub-form-row-index="subFormRowIndex"
                :sub-form-col-index="subFormColIndex"
              >
                <!-- 递归传递插槽！！！ -->
                <template v-for="slot in Object.keys(slots)" #[slot]="scope">
                  <slot :name="slot" v-bind="scope" />
                </template>
              </component>
            </template>
            <template v-else>
              <component
                :is="subWidget.type + '-widget'"
                :key="swIdx"
                :field="subWidget"
                :parent-list="tab.widgetList"
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
        </van-tab>
      </van-tabs>
    </div>
  </container-item-wrapper>
</template>

<script>
import { computed, reactive, toRefs, getCurrentInstance, onMounted, onBeforeUnmount } from 'vue'

// import { useI18n } from '~@/utils/i18n'
import { useEmitter } from '~@/utils/emitter'
import { useRef } from '../../../components/form-render/refMixin'
import { useContainer } from './containerItemMixin'
// import { px2rem } from '~@/utils/util'

import ContainerItemWrapper from './container-item-wrapper'
import FieldComponents from '~@/components/form-designer/form-widget/field-widget/index'

export default {
  componentName: 'ContainerItem',
  name: 'MTabItem',
  components: {
    ContainerItemWrapper,
    ...FieldComponents
  },
  props: {
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
    // const sfRefList = inject('sfRefList')
    // const globalModel = inject('globalModel')

    const { proxy } = getCurrentInstance()

    const data = reactive({
      activeTabName: ''
    })
    const refMixin = useRef()
    const emitterMixin = useEmitter(data)
    const containerMixin = useContainer(data)

    const visibleTabs = computed(() => {
      return props.widget.tabs.filter(tp => {
        return !tp.options.hidden
      })
    })

    const tabDisplayType = computed(() => {
      return props.widget.options.displayType === 'border-card' ? 'card' : 'line'
    })

    const initActiveTab = () => {
      if (props.widget.type === 'm-tab' && props.widget.tabs.length > 0) {
        if (!props.widget.options.active) {
          data.activeTabName = props.widget.tabs[0].options.name
        } else {
          data.activeTabName = props.widget.options.active
        }
      }
    }

    const handleTabbarChange = (name, title) => {
      const index = props.widget.tabs.findIndex(x => x.options.name === name)
      if (props.widget.options.onTabbarChange) {
        let customFunc = new Function('index', 'name', 'title', props.widget.options.onTabbarChange)
        customFunc.call(proxy, index, name, title)
      }
    }

    /**
     * 获取当前激活的页签索引，从0开始计数
     * @return {number}
     */
    const getActiveTabIndex = () => {
      let foundIndex = -1
      props.widget.tabs.forEach((tp, idx) => {
        if (tp.options.name === data.activeTabName) {
          foundIndex = idx
        }
      })

      return foundIndex
    }

    const customClass = computed(() => {
      return props.widget.options.customClass || ''
    })

    onMounted(() => {
      initActiveTab()
    })

    onBeforeUnmount(() => {
      containerMixin.unregisterFromRefList()
    })

    refMixin.initRefList()

    return {
      ...toRefs(data),
      ...emitterMixin,
      ...containerMixin,
      customClass,

      getActiveTabIndex,
      handleTabbarChange,

      slots: proxy.$slots,
      tabDisplayType,
      visibleTabs
    }
  }
}
</script>

<style lang="scss" scoped></style>
