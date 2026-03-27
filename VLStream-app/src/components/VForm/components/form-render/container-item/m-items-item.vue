<!-- eslint-disable vue/no-v-for-template-key -->
<template>
  <container-item-wrapper :widget="widget">
    <div
      v-show="!widget.options.hidden"
      :key="widget.id"
      :ref="widget.id"
      class="item-container"
      :class="[customClass]"
    >
      <template v-for="(colWidget, colIdx) in widget.items" :key="colIdx">
        <div class="detail-item">
          <span>明细{{ colIdx + 1 }}</span>
        </div>
        <itemC
          :ref="`itemC${colIdx}`"
          :field="field"
          :widget="colWidget"
          :parent-list="widget.items"
          :index-of-parent-list="0"
          :parent-widget="widget"
        />
      </template>
      <template v-for="(colWidget, colIdx) in copyItems" :key="colIdx">
        <div class="detail-item">
          <span>明细{{ colIdx + 2 }}</span>
          <van-button
            v-if="!widget.options.disabled"
            icon="delete-o"
            type="primary"
            plain
            size="mini"
            class="delete-btn"
            @click="deleteDetail(colIdx)"
          />
        </div>
        <itemC
          :ref="`itemC${colIdx + 1}`"
          :widget="colWidget"
          :designer="designer"
          :field="field"
          :parent-list="widget.items"
          :index-of-parent-list="colIdx + 1"
          :parent-widget="widget"
          :col-height="widget.options.colHeight"
        />
      </template>
      <van-button
        v-if="!widget.options.disabled"
        :disabled="widget.options.disabled"
        type="primary"
        class="add-detail-btn"
        icon="plus"
        plain
        size="small"
        @click="addDetail"
      >
        <span>添加明细</span>
      </van-button>
    </div>
  </container-item-wrapper>
</template>

<script>
import { computed, onBeforeUnmount, onMounted, ref, reactive, getCurrentInstance } from 'vue'

import { useEmitter } from '~@/utils/emitter'
import { useRef } from '../../../components/form-render/refMixin'
import { useContainer } from './containerItemMixin'
import { px2rem } from '~@/utils/util'
import ContainerItemWrapper from './container-item-wrapper'
import itemC from './m-itemC'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'

export default {
  componentName: 'ItemsItem',
  name: 'MItemsItem',
  components: {
    ContainerItemWrapper,
    itemC
  },
  props: {
    field: Object,
    widget: Object
  },
  setup(props) {
    // const refList = inject('refList')
    // const globalModel = inject('globalModel')
    // const sfRefList = inject('sfRefList')

    const refMixin = useRef()
    const containerMixin = useContainer()
    const emitterMixin = useEmitter()

    const data = reactive({
      fieldModel: null,
      noChangeEventFlag: true, // van-field组件没有change事件！！
      oldFieldValue: null // field组件change之前的值
    })
    const fieldMixin = useField(data)

    const customClass = computed(() => {
      return props.widget.options.customClass || ''
    })

    onMounted(() => {
      emitterMixin.off$('itemsItemChange') // 移除原有事件监听
      emitterMixin.on$('itemsItemChange', (data) => {
        localDefaultValue.value[data[0]][data[1]] = data[2]
        fieldMixin.handleChangeEvent(localDefaultValue.value)
      })
    })

    onBeforeUnmount(() => {
      containerMixin.unregisterFromRefList()
    })

    refMixin.initRefList()

    const copyItems = ref([])

    // 找到值对象（setup语法糖写法）
    const tempObj = {}
    if (props.widget.items && props.widget.items.length > 0) {
      props.widget.items[0].widgetList.forEach(item => {
        tempObj[item.id] = item.options.defaultValue
      })
    }
    const localDefaultValue = ref([])
    if (!props.widget.options.defaultValue || props.widget.options.defaultValue.length === 0) {
      localDefaultValue.value = JSON.parse(JSON.stringify([tempObj]))
    } else {
      localDefaultValue.value = JSON.parse(JSON.stringify(props.widget.options.defaultValue))
    }

    // 根据 localDefaultValue 的值来创建 copyItems，并把值传递到对应的 option 的 defaultValue
    copyItems.value = []
    if (
      Array.isArray(localDefaultValue.value) &&
      props.widget.items &&
      props.widget.items.length > 0
    ) {
      localDefaultValue.value.forEach((rowValue, index) => {
        if (index === 0) {
          props.widget.items[0].widgetList.forEach(widget => {
            if (Object.prototype.hasOwnProperty.call(rowValue, widget.id)) {
              widget.options.defaultValue = rowValue[widget.id]
            }
          })
        } else {
          // 深拷贝一份 items[0] 作为每一行的 copyItem
          let itemCopy = JSON.parse(JSON.stringify(props.widget.items[0]))
          // 将 rowValue 的值赋值到 itemCopy 的 widgetList 的每个组件的 options.defaultValue
          if (itemCopy.widgetList && Array.isArray(itemCopy.widgetList)) {
            itemCopy.widgetList.forEach(widget => {
              if (Object.prototype.hasOwnProperty.call(rowValue, widget.id)) {
                widget.options.defaultValue = rowValue[widget.id]
              }
            })
          }
          copyItems.value.push(itemCopy)
        }
      })
    }

    const deleteDetail = (colIdx) => {
      copyItems.value.splice(colIdx, 1)
      localDefaultValue.value.splice(colIdx, 1)
    }

    const addDetail = () => {
      if (props.widget.items.length > 0) {
        let temp = JSON.parse(JSON.stringify(props.widget.items[0]))
        copyItems.value.push(temp)
        localDefaultValue.value.push(JSON.parse(JSON.stringify(tempObj)))
      }
    }

    const { proxy } = getCurrentInstance()
    const setDisabled = (flag = true) => {
      proxy.$refs.itemC0[0].setDisabled(flag)
      copyItems.value.forEach((item, colIdx) => {
        proxy.$refs[`itemC${colIdx + 1}`][0].setDisabled(flag)
      })
      // eslint-disable-next-line vue/no-mutating-props
      props.field.options.disabled = flag
    }

    return {
      ...emitterMixin,
      ...containerMixin,
      customClass,
      px2rem,
      copyItems,
      addDetail,
      deleteDetail,
      setDisabled
    }
  }
}
</script>

<style lang="scss" scoped>

.item-container {
    border-radius: 0;
    display: flex;
    flex-direction: column;
    min-height: 50px;
    outline: 3px solid #f9f9f9;
    padding: 15px;
    .form-widget-list {
      min-height: 28px;
    }

    .add-detail-btn {
      margin: 0 auto;
      width: 100%;
      min-width: 100px;
    }
  }

.detail-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 16px;
    background-color: #f9f9f9;
    border-left: 2px solid #dcdcdc;
    margin-bottom: 4px;
    span {
      margin-left: 10px;
      font-size: 12px;
      color: #9c9c9c;
    }
  }

  :deep(.field-wrapper) {
    margin: 0;
    border-radius: 0;
  }

  .delete-btn {
    width: 18px;
    height: 16px;
  }

</style>
