<template>
  <form-item-wrapper
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
    <van-field
      :name="field.options.name"
      :label="fieldLabel"
      :label-align="field.options.labelAlign"
      :label-width="fieldLabelWidth"
      :disabled="fieldDisabled"
      :required="field.options.required"
      :size="size"
      :rules="rules"
    >
      <template #input>
        <div class="switch-wrapper">
          <template v-if="!isReadMode && !field.options.readonly">
            <span v-if="inactiveText" class="inactive-label" :class="{ diabled: fieldDisabled }" :style="{ color: inactiveColor }">{{ inactiveText }}</span>
            <van-switch
              ref="fieldEditor"
              v-model="fieldModel"
              :size="px2rem(field.options.switchSize)"
              :active-color="field.options.activeColor"
              :inactive-color="field.options.inactiveColor"
              :active-value="field.options.activeValue"
              :inactive-value="field.options.inactiveValue"
              :disabled="fieldDisabled"
              @change="handleChangeEvent"
            />
            <span v-if="activeText" class="active-label" :class="{ diabled: fieldDisabled }" :style="{ color: activeColor }">{{ activeText }}</span>
          </template>
          <template v-if="field.options.readonly || isReadMode">
            <span class="readonly-mode-field" :class="{ diabled: fieldDisabled }">{{ displayValue }}</span>
          </template>
        </div>
      </template>
    </van-field>
  </form-item-wrapper>
</template>

<script>
import { computed, toRefs, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { px2rem } from '~@/utils/util'

export default {
  componentName: 'FieldWidget',
  name: 'MSwitchWidget',
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

    const data = reactive({
      fieldModel: null,
      noChangeEventFlag: false,
      oldFieldValue: null, // field组件change之前的值
      px2rem: px2rem,

      rules: []
    })
    const fieldMixin = useField(data)

    // eslint-disable-next-line vue/return-in-computed-property
    const displayValue = computed(() => {
      if (data.fieldModel === props.field.options.activeValue) return props.field.options.activeLabel || props.field.options.activeText || props.field.options.activeValue
      else if (data.fieldModel === props.field.options.inactiveValue) return props.field.options.inactiveLabel || props.field.options.inactiveText || props.field.options.inactiveValue
    })

    const activeText = computed(() => {
      if (props.field.options.activeLabel !== undefined) {
        return props.field.options.activeLabel || props.field.options.activeValue
      } else {
        return props.field.options.activeText || props.field.options.activeValue
      }
    })
    const inactiveText = computed(() => {
      if (props.field.options.inactiveLabel !== undefined) {
        return props.field.options.inactiveLabel || props.field.options.inactiveValue
      } else {
        return props.field.options.inactiveText || props.field.options.inactiveValue
      }
    })

    const activeColor = computed(() => {
      return data.fieldModel === true ? props.field.options.activeColor || 'var(--van-primary-color)' : undefined
    })
    const inactiveColor = computed(() => {
      return data.fieldModel === false ? props.field.options.inactiveColor || 'var(--van-primary-color)' : undefined
    })

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    fieldMixin.registerToRefList()
    fieldMixin.initFieldModel()
    fieldMixin.initEventHandler()
    // fieldMixin.buildFieldRules()

    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...fieldMixin,
      ...toRefs(data),

      activeColor,
      activeText,
      displayValue,
      inactiveColor,
      inactiveText
    }
  }
}
</script>

<style lang="scss" scoped>
.switch-wrapper {
  display: inline-flex;
  height: 26px;
  line-height: 26px;
  align-items: center;
}
.readonly-mode-field {
  height: 26px;
}
.active-label {
  margin-left: 10px;
  height: 26px;
  line-height: 26px;
  &.diabled {
    color: #c8c9cc;
  }
}
.inactive-label {
  margin-right: 10px;
  height: 26px;
  line-height: 26px;
  &.diabled {
    color: #c8c9cc;
  }
}
</style>
