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
    <el-select
      ref="fieldEditor"
      v-model="fieldModel"
      class="full-width-input"
      :disabled="field.options.disabled"
      :size="widgetSize"
      :clearable="field.options.clearable"
      :filterable="field.options.filterable"
      :allow-create="field.options.allowCreate"
      :multiple="field.options.multiple"
      :multiple-limit="field.options.multipleLimit"
      :placeholder="field.options.placeholder || i18nt('render.hint.selectPlaceholder')"
      @focus="handleFocusCustomEvent"
      @blur="handleBlurCustomEvent"
      @change="handleChangeEvent"
    >
      <el-option
        v-for="item in optionsItemList"
        :key="item.value"
        :label="item.label"
        :value="item.value"
        :disabled="item.disabled"
      />
    </el-select>
  </form-item-wrapper>
</template>

<script>
import FormItemWrapper from './form-item-wrapper'
import emitter from '~@/utils/emitter'
import i18n, { translate } from '~@/utils/i18n'
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import { ElSelect, ElOption } from 'element-plus'

export default {
  name: 'LinkSearchWidget',
  componentName: 'FieldWidget',
  components: {
    FormItemWrapper,
    ElSelect,
    ElOption
  }, // 必须固定为FieldWidget，用于接收父级组件的broadcast事件
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
      oldFieldValue: null, // field组件change之前的值
      fieldModel: null,
      rules: [],
      optionsItemList: []
    }
  },
  computed: {
    showFullPath() {
      return (this.field.options.showAllLevels === undefined) || !!this.field.options.showAllLevels
    },
    remoteMethod() {
      if (!!this.field.options.remote && !!this.field.options.onRemoteQuery) {
        return this.remoteQuery
      } else {
        return undefined
      }
    }
  },
  watch: {
    'field.options.linkQuery'() {
      this.getOptionsItems()
    }
  },
  beforeCreate() {
    /* 这里不能访问方法和属性！！ */
  },

  created() {
    /* 注意：子组件mounted在父组件created之后、父组件mounted之前触发，故子组件mounted需要用到的prop
         需要在父组件created中初始化！！ */
    this.initOptionItems()
    this.initFieldModel()
    this.registerToRefList()
    this.initEventHandler()
    this.buildFieldRules()

    this.handleOnCreated()

    this.optionsItemList = this.field.options.optionItems
    this.getOptionsItems()
  },

  mounted() {
    this.handleOnMounted()
  },

  beforeUnmount() {
    this.unregisterFromRefList()
  },

  methods: {
    getOptionsItems() {
      if (this.field.options.linkQuery) {
        const func = new Function(this.field.options.linkQuery)
        this.optionsItemList = func.call(this)
        console.log(this.optionsItemList)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "../../../../styles/global.scss"; /* form-item-wrapper已引入，还需要重复引入吗？ */

  .full-width-input {
    width: 100% !important;

    :deep(.el-cascader) {
      width: 100% !important;
    }
  }

</style>
