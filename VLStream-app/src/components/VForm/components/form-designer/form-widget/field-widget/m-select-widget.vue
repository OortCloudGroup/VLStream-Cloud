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
      v-model="displayValue"
      :name="field.options.name"
      :clickable="!isReadMode"
      :is-link="!isReadMode"
      readonly
      :label="fieldLabel"
      :label-align="labelAlign"
      :label-width="fieldLabelWidth"
      :placeholder="field.options.placeholder"
      :required="field.options.required"
      :rules="rules"
      :disabled="fieldDisabled"
      :size="size"
      @click="showPopup"
    >
      <template #right-icon>
        <clearIcon v-if="clearable" />
      </template>
    </van-field>
    <van-popup v-model:show="showPopupFlag" round position="bottom" teleport="body" :class="[field.options.filterable ? 'filter-popup' : '']">
      <van-picker
        v-show="!isReadMode"
        ref="pickerRef"
        v-model="pickerValue"
        show-toolbar
        :columns="field.options.optionItems"
        :columns-field-names="{ text: labelKey, value: valueKey }"
        @cancel="showPopupFlag = false"
        @confirm="handleConfirm"
      >
        <template v-if="field.options.filterable" #toolbar>
          <div class="title-wrapper">
            <div class="title-action-wrapper">
              <button type="button" class="popup-button van-picker__cancel" @click="showPopupFlag = false">
                {{ i18nt('designer.hint.cancel') }}
              </button>
              <button type="button" class="popup-button van-picker__confirm" @click="confirmSelected">
                {{ i18nt('designer.hint.confirm') }}
              </button>
            </div>
            <div class="title-search-wrapper">
              <van-search ref="searchRef" v-model.trim="searchVal" autofocus :placeholder="i18nt('designer.hint.searchPlaceholder')" @search="onSearch" @clear="onSearch" />
            </div>
          </div>
        </template>
        <template v-if="field.options.multiple" #option="option">
          <van-checkbox :model-value="isSelected(option)" :disabled="option.disabled" shape="square" :icon-size="px2rem('14px')" @update:modelValue="val => onMultipleOptionSelected(option, val)">
            {{
              option.label
            }}
          </van-checkbox>
        </template>
      </van-picker>
    </van-popup>
  </form-item-wrapper>
</template>

<script>
import { ref, toRefs, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { px2rem, deepClone } from '~@/utils/util'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import clearIcon from '../components/clear.vue'
export default {
  componentName: 'FieldWidget',
  name: 'MSelectWidget',
  components: {
    FormItemWrapper,
    clearIcon
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

    const pickerRef = ref()
    const data = reactive({
      copyOfOptionItems: null,
      fieldModel: null,
      fieldModelArray: [], // 多选时，临时记录选中的值
      labelKey: 'label',

      noChangeEventFlag: true, // van-field组件没有change事件！！

      oldFieldValue: null, // field组件change之前的值
      pickerValue: [], // 弹出选择框的值,vant要求数组格式
      rules: [],

      searchVal: '',
      showPopupFlag: false,
      valueKey: 'value'
    })
    const fieldMixin = useField(data)

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    const displayValue = computed(() => {
      let labels = []
      if (!!data.fieldModel) {
        if (Array.isArray(data.fieldModel)) {
          props.field.options.optionItems.forEach(option => {
            let opt = data.fieldModel.find(x => x === option.value)
            if (opt) {
              labels.push(option.label)
            }
          })
        } else {
          props.field.options.optionItems.forEach(option => {
            if (option.value === data.fieldModel) {
              labels.push(option.label)
            }
          })
        }
      }
      return labels.join(',')
    })

    // const labelKey = computed(() => {
    //   return props.field.options.labelKey || 'label'
    // })
    // const valueKey = computed(() => {
    //   return props.field.options.valueKey || 'value'
    // })

    const showPopup = e => {
      if (!!props.designState || props.field.options.readonly || fieldMixin.isReadMode.value || fieldMixin.fieldDisabled.value) {
        data.showPopupFlag = false
        return
      }

      if (props.field.options.multiple) {
        data.fieldModelArray = deepClone(data.fieldModel)
        if (data.fieldModelArray.length > 0) {
          data.pickerValue = [data.fieldModelArray[0]]
        }
      } else {
        data.pickerValue = [data.fieldModel]
      }
      data.showPopupFlag = true
      fieldMixin.handleFocusCustomEvent(e)
    }
    const handleConfirm = value => {
      if (!props.field.options.multiple) {
        data.fieldModel = value.selectedOptions[0].value
      } else {
        data.fieldModel = deepClone(data.fieldModelArray)
      }
      fieldMixin.handleChangeEvent(data.fieldModel)
      fieldMixin.handleBlurCustomEvent()

      data.showPopupFlag = false
    }

    const confirmSelected = () => {
      pickerRef.value.confirm()
    }

    const isSelected = option => {
      if (data.fieldModelArray && Array.isArray(data.fieldModelArray)) {
        return data.fieldModelArray.find(x => x === option.value)
      }
    }

    const onMultipleOptionSelected = (option) => {
      if (!data.fieldModelArray) data.fieldModelArray = []
      if (!Array.isArray(data.fieldModelArray)) {
        throw new Error('fieldMode必须是数组类型')
      }

      const index = data.fieldModelArray.indexOf(option.value)

      if (index === -1) {
        data.fieldModelArray.push(option.value)
      } else {
        data.fieldModelArray.splice(index, 1)
      }
    }

    const onSearch = () => {
      // 开启筛选，没有开启远程搜索
      if (!!props.field.options.remote) {
        if (!!props.field.options.onRemoteQuery) {
          fieldMixin.remoteQuery(data.searchVal)
        } else {
          fieldMixin.remoteDataSourceQuery(data.searchVal)
        }
      } else {
        if (data.copyOfOptionItems == null) {
          data.copyOfOptionItems = [...props.field.options.optionItems]
        }
        let filterOptionItems = data.copyOfOptionItems.filter(x => {
          return x.label.toString().indexOf(data.searchVal) > -1
        })
        // eslint-disable-next-line vue/no-mutating-props
        props.field.options.optionItems.splice(0, props.field.options.optionItems.length)
        filterOptionItems.forEach(x => {
          // eslint-disable-next-line vue/no-mutating-props
          props.field.options.optionItems.push(x)
        })
      }
    }

    fieldMixin.registerToRefList()
    // 远程初始化搜索DSV.search避免undeifend
    fieldMixin.initDSVSearch()
    fieldMixin.initOptionItems()
    fieldMixin.initFieldModel()
    fieldMixin.initEventHandler()
    fieldMixin.buildFieldRules()

    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...fieldMixin,
      ...toRefs(data),
      confirmSelected,

      displayValue,
      handleConfirm,
      isSelected,
      onMultipleOptionSelected,
      onSearch,
      pickerRef,
      props,
      px2rem,
      showPopup
    }
  }
}
</script>

<style lang="scss" scoped>
.readonly-mode-field {
  height: 28px;
}

:deep(.title-wrapper) {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
}

:deep(.title-action-wrapper) {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 10px;
  padding-bottom: 0px;
  box-sizing: border-box;
}

:deep(.title-search-wrapper) {
  width: 100%;
  display: block;

  .van-search__action {
    display: inline-flex;
    justify-content: center;
    align-items: center;
  }
}
</style>
<style lang="scss">
.filter-popup {
  .van-picker__toolbar {
    height: 80px;
  }
}
</style>
