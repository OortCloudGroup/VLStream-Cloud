<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-12-05 10:27:37
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
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
    <ChoosePersonOrDept
      v-model="fieldModel"
      :options="field.options"
      :is-multi="field.options.multiple"
      @change="handleChangeEvent"
    />
  </form-item-wrapper>
</template>

<script>
import FormItemWrapper from './form-item-wrapper'
import emitter from '~@/utils/emitter'
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import ChoosePersonOrDept from './components/ChoosePersonOrDept.vue'

export default {
  name: 'AddressBookWidget',
  componentName: 'FieldWidget',
  components: {
    FormItemWrapper,
    ChoosePersonOrDept
  }, // 必须固定为FieldWidget，用于接收父级组件的broadcast事件
  mixins: [emitter, fieldMixin],
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
      fieldModel: '',
      rules: []
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

    this.initDefaultValue()
  },

  mounted() {
    this.handleOnMounted()
  },

  beforeUnmount() {
    this.unregisterFromRefList()
  },

  methods: {
    initDefaultValue() {
      // 判断默认值类型，如果是固定值，就将固定值赋值给fieldModel
      // TODO 这里的其他值要看下表单怎么去获取，不建议直接使用this.$store，这样表单就有状态了
      switch (this.field.options.addressSetting.defaultValueType) {
        case 1:
          this.fieldModel = ''
          break
        case 2: // 固定值
          this.fieldModel = this.field.options.addressSetting.defaultValue
          break
        case 3: // 提交人
          break
        case 4: // 提交部门
          break
        case 5: // 创建人
          break
        case 6: // 创建人部门
          break
        case 7: // 拥有者
          break
        default:
          break
      }
    }

  }
}
</script>

<style lang="scss" scoped>
  @import "../../../../styles/global.scss"; /* form-item-wrapper已引入，还需要重复引入吗？ */

</style>
