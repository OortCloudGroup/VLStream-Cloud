<template>
  <form-item-wrapper
    :designer="designer"
    :field="field"
    :design-state="designState"
    :title="field.options.label"
    :parent-widget="parentWidget"
    :parent-list="parentList"
    :index-of-parent-list="indexOfParentList"
    :sub-form-row-index="subFormRowIndex"
    :sub-form-col-index="subFormColIndex"
    :sub-form-row-id="subFormRowId"
  >
    <van-field
      v-model="cascaderValue"
      :name="field.options.name"
      clickable
      readonly
      :label="fieldLabel"
      is-link
      :label-align="field.options.labelAlign"
      :label-width="fieldLabelWidth"
      :placeholder="field.options.placeholder"
      :disabled="fieldDisabled"
      :required="field.options.required"
      :rules="rules"
      :size="size"
      @click="showPopup"
    >
      <template v-if="fieldModel.length > 0" #input>
        <div class="multiple-display-wrapper">
          <van-tag v-for="(item, index) in displayArray()" :key="index" color="#F0F2F5" text-color="#909399" size="medium" class="display-item" :closeable="false">
            {{ item }}
          </van-tag>
        </div>
      </template>
    </van-field>
    <van-popup v-model:show="showPopupFlag" round position="bottom" teleport="body">
      <van-cascader
        ref="fieldEditor"
        v-model="selectedValue"
        :title="field.options.placeholder"
        :options="cascaderOptionItems"
        :field-names="fieldNames"
        @close="showPopupFlag = false"
        @finish="handleFinish"
        @change="onCascaderSelected"
      >
        <template v-if="field.options.multiple" #option="{ option }">
          <div class="option-wrapper">
            <van-checkbox :model-value="isSelected(option)" :disabled="option.disabled" shape="square" :icon-size="px2rem('14px')">
              {{ option.label }}
            </van-checkbox>
          </div>
        </template>
      </van-cascader>
    </van-popup>
  </form-item-wrapper>
</template>

<script>
import { reactive, toRefs, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { px2rem, deepClone } from '~@/utils/util'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'

export default {
  componentName: 'FieldWidget',
  name: 'MCascaderAreaWidget',
  components: {
    FormItemWrapper
  },
  props: {
    designState: {
      default: false,
      type: Boolean
    },
    designer: Object,
    field: Object,
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
    }
  },
  setup(props) {
    // const refList = inject('refList')
    // const globalOptionData = inject('globalOptionData')
    // const globalModel = inject('globalModel')

    const { i18nt } = useI18n()

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    const data = reactive({
      cascaderValue: '', // 没有实际作用，为了给field赋值，否则必填校验过不去
      displayValue: '',
      fieldModel: [],
      fieldNames: {
        children: props.field.options.childrenKey || 'children',
        text: props.field.options.labelKey || 'label',
        value: props.field.options.valueKey || 'value'
      },

      noChangeEventFlag: true, // van-field组件没有change事件！！

      oldFieldValue: [], // field组件change之前的值
      rules: [],
      selectedValue: '', // cascader回显数据(因为vant限制仅支持单选的回显)， 多选的回显靠isSelected判断
      showPopupFlag: false
    })
    const fieldMixin = useField(data)

    const getSelectedOptionsByValue = (options, value, layer) => {
      let selectedOptions = []
      for (let i = 0; i < options.length; i++) {
        let option = options[i]

        if (option[data.fieldNames.value] === value[layer]) {
          selectedOptions.push(option)
          if (option[data.fieldNames.children]) {
            selectedOptions.push(...getSelectedOptionsByValue(option[data.fieldNames.children], value, layer + 1))
          }
          break
        }
      }
      return selectedOptions
    }

    // 获取选择项的label
    const getSelectedLabel = selectedItem => {
      if (selectedItem.length === 0) return null
      let selectedOptions = getSelectedOptionsByValue(props.field.options.areaOptionItems.optionItems, selectedItem, 0)
      if (!!selectedOptions && selectedOptions.length > 0) {
        return selectedOptions.map(option => option[data.fieldNames.text]).join('/')
      }
    }

    // 显示的值
    const displayArray = () => {
      let displayLabels = []
      if (props.field.options.multiple) {
        for (let i = 0; i < data.fieldModel.length; i++) {
          let selectedItem = data.fieldModel[i]
          let selectedLabel = getSelectedLabel(selectedItem)
          displayLabels.push(selectedLabel)
        }
        data.cascaderValue = data.fieldModel.map(fieldModelItem => fieldModelItem.join('_')).join(',')
      } else if (data.fieldModel) {
        console.log('data.fieldModel----', data.fieldModel)
        let selectedLabel = getSelectedLabel(data.fieldModel)
        if (selectedLabel) {
          displayLabels.push(selectedLabel)
          data.selectedValue = data.fieldModel[data.fieldModel.length - 1]
          data.cascaderValue = data.fieldModel.join('_')
        }
      } else {
        data.selectedValue = ''
        data.cascaderValue = ''
      }

      return displayLabels
    }

    // 弹出cascader选择
    const showPopup = () => {
      if (!!props.designState) {
        data.showPopupFlag = false
        return
      }

      if (props.field.options.readonly || fieldMixin.isReadMode.value || fieldMixin.fieldDisabled.value) return

      data.showPopupFlag = true
    }

    // 确认选择
    const handleFinish = ({ selectedOptions }) => {
      console.log('selectedOptions', selectedOptions)
      if (!data.fieldModel) data.fieldModel = []

      if (props.field.options.multiple) {
        fieldMixin.handleChangeEvent(data.fieldModel)
      } else {
        data.fieldModel = selectedOptions.map(x => x[data.fieldNames.value])
        console.log(data.fieldNames.value, data.fieldModel)
        fieldMixin.handleChangeEvent(data.fieldModel)
        data.showPopupFlag = false
      }
    }

    // 多选的判断是否进行了选择
    const isSelected = option => {
      let selected = false
      let optionLastValue = option.fullValue[option.fullValue.length - 1]
      for (let i = 0; i < data.fieldModel.length; i++) {
        const x = data.fieldModel[i]
        selected = selected || x.indexOf(optionLastValue) >= 0
      }

      return selected
    }

    // 判断两个数组值相等
    const arraysEqual = (arr1, arr2) => {
      if (arr1.length !== arr2.length) {
        return false
      }
      for (let i = 0; i < arr1.length; i++) {
        if (arr1[i] !== arr2[i]) {
          return false
        }
      }
      return true
    }

    // 选中值
    const onCascaderSelected = ({ selectedOptions }) => {
      // 如果选择的不是最后一级不执行
      if (selectedOptions[selectedOptions.length - 1].children && selectedOptions[selectedOptions.length - 1].children.length > 0) return

      if (props.field.options.multiple) {
        let selectedItem = selectedOptions.map(option => option[data.fieldNames.value])
        let existIndex = -1
        for (let i = 0; i < data.fieldModel.length; i++) {
          if (arraysEqual(data.fieldModel[i], selectedItem)) {
            existIndex = i
            break
          }
        }

        if (existIndex >= 0) {
          data.fieldModel.splice(existIndex, 1)
        } else {
          data.fieldModel.push(selectedItem)
          data.cascaderValue = data.fieldModel.map(fieldModelItem => fieldModelItem.join('_')).join(',')
        }
      } else {
        data.fieldModel = selectedOptions.map(option => option[data.fieldNames.value])
        data.cascaderValue = data.fieldModel.join('_')
      }
    }

    const cascaderOptionItems = computed(() => {
      const optionItems = deepClone(props.field.options.areaOptionItems.optionItems)
      const setFullPath = (optionItem, parentItem) => {
        optionItem.forEach(item => {
          item.fullValue = [...parentItem, item.value]
          if (item.children) {
            setFullPath(item.children, item.fullValue)
          }
        })
      }
      setFullPath(optionItems, [])
      return optionItems
    })

    const setFieldModel = newValue => {
      data.fieldModel = deepClone(newValue)
    }

    fieldMixin.registerToRefList()
    fieldMixin.initOptionItems()
    fieldMixin.initFieldModel()
    fieldMixin.initEventHandler()
    fieldMixin.buildFieldRules()
    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...fieldMixin,
      ...toRefs(data),

      cascaderOptionItems,
      displayArray,
      getSelectedOptionsByValue,
      handleFinish,
      isSelected,
      onCascaderSelected,
      px2rem,
      setFieldModel,
      showPopup
    }
  }
}
</script>

<style scoped lang="scss">
.multiple-display-wrapper {
  display: inline-flex;
  flex-wrap: wrap;

  :deep(.display-item) {
    margin: 2px 10px 2px 0px;
    margin-right: 10px;
    flex-shrink: 0;

    &:last-child {
      margin-right: 0;
    }
  }
}
</style>
