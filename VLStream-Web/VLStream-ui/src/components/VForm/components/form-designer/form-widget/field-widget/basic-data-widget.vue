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
    <div v-if="optionList.length === 0" class="option_no_data">
      <span>请选择基础数据或者基础数据为空</span>
    </div>
    <template v-else>
      <template v-if="field.options.basicOptions.showType === 1">
        <el-radio-group
          v-model="fieldModel"
          :disabled="field.options.disabled"
          :readonly="field.options.readonly"
          @change="dictChange"
        >
          <el-radio v-for="(item, index) in optionList" :key="index" :label="item.name" :value="item.description">
            {{ item.name }}
          </el-radio>
        </el-radio-group>
      </template>
      <template v-if="field.options.basicOptions.showType === 2">
        <el-checkbox-group
          v-model="fieldModel"
          :disabled="field.options.disabled"
          :readonly="field.options.readonly"
          @change="dictChange"
        >
          <el-checkbox v-for="(item, index) in optionList" :key="index" :label="item.name" :value="item.description" />
        </el-checkbox-group>
      </template>
      <template v-if="field.options.basicOptions.showType === 3 || field.options.basicOptions.showType === 4">
        <el-select
          v-model="fieldModel"
          :disabled="field.options.disabled"
          :readonly="field.options.readonly"
          :placeholder="field.options.placeholder"
          :multiple="field.options.basicOptions.showType === 4"
          :clearable="field.options.clearable"
          @change="dictChange"
        >
          <el-option v-for="(item, index) in optionList" :key="index" :label="item.name" :value="item.description" />
        </el-select>
      </template>
    </template>
  </form-item-wrapper>
</template>

<script>
import FormItemWrapper from './form-item-wrapper'
import emitter from '~@/utils/emitter'
import i18n, { translate } from '~@/utils/i18n'
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import { ElSelect, ElOption, ElCheckbox, ElCheckboxGroup, ElRadioGroup, ElRadio } from 'element-plus'
import { dictListByTag } from '@/api/login/indexapaas'

export default {
  name: 'BasicDataWidget',
  componentName: 'FieldWidget',
  components: {
    ElSelect,
    ElOption,
    FormItemWrapper,
    ElCheckbox,
    ElCheckboxGroup, ElRadioGroup, ElRadio
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
      optionList: []
    }
  },
  computed: {
    inputType() {
      if (this.field.options.type === 'number') {
        return 'text' // 当input的type设置为number时，如果输入非数字字符，则v-model拿到的值为空字符串，无法实现输入校验！故屏蔽之！！
      }
      return this.field.options.type
    }
  },
  watch: {
    'field.options.basicOptions.dataTag'() {
      this.getOptionList()
    }
  },
  beforeCreate() {
    /* 这里不能访问方法和属性！！ */
  },

  created() {
    /* 注意：子组件mounted在父组件created之后、父组件mounted之前触发，故子组件mounted需要用到的prop
         需要在父组件created中初始化！！ */
    this.initFieldModel()
    this.registerToRefList()
    this.initEventHandler()
    this.buildFieldRules()

    this.handleOnCreated()
  },

  mounted() {
    this.handleOnMounted()
    this.getOptionList()
  },

  beforeUnmount() {
    this.unregisterFromRefList()
  },

  methods: {
    dictChange() {
      this.handleChangeEvent(this.fieldModel)
    },
    getOptionList() {
      if (this.field.options.basicOptions.dataTag) {
        const params = {
          dict_tag: this.field.options.basicOptions.dataTag
        }
        dictListByTag(params).then(res => {
          if (res.code === 200) {
            this.optionList = res.data.list || []
          }
        })
      } else {
        this.optionList = []
      }
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "../../../../styles/global.scss"; /* form-item-wrapper已引入，还需要重复引入吗？ */

  .option_no_data {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    span {
      font-size: 14px;
      color: #999;
    }
  }
</style>
