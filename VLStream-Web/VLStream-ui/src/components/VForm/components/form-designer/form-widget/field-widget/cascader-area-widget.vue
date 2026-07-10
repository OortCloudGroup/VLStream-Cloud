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
    <div class="full-width-input">
      <el-cascader
        v-if="!field.options.mapCenter"
        ref="fieldEditor"
        v-model="fieldModel"
        :options="field.options.areaOptionItems?.optionItems"
        :disabled="field.options.disabled"
        :size="widgetSize"
        :clearable="field.options.clearable"
        :filterable="field.options.filterable"
        :placeholder="field.options.placeholder || i18nt('render.hint.selectPlaceholder')"
        :show-all-levels="showFullPath"
        :props="{ checkStrictly: field.options.checkStrictly, multiple: field.options.multiple, expandTrigger: 'hover' }"
        @focus="handleFocusCustomEvent"
        @blur="handleBlurCustomEvent"
        @change="handleChangeEvent"
      />
      <template v-else>
        <div v-if="fieldModel" class="address_div">
          <span class="address_span">{{ address }} </span>
          <span class="address_span_jw">({{ showJWD }})</span>
        </div>
        <el-button :disabled="field.options.disabled" :icon="Plus" @click="showBDMAP">
          地图选点
        </el-button>
      </template>
    </div>
    <el-dialog v-model="bMapVisible" title="选择详细地址" width="70%">
      <BaidumapSelectAddress :center="field.options.mapCenter" :area-type="field.options.areaOptionItems?.areaType" :is-edit="false" @change="changeAddress" />
    </el-dialog>
  </form-item-wrapper>
</template>

<script>
import FormItemWrapper from './form-item-wrapper'
import emitter from '~@/utils/emitter'
import i18n, { translate } from '~@/utils/i18n'
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import { ElButton, ElCascader, ElDialog, ElInput } from 'element-plus'
import BaidumapSelectAddress from '~@/components/form-designer/setting-panel/baidumapSelectAddress'
import { Plus } from '@element-plus/icons-vue'

export default {
  name: 'CascaderAreaWidget',
  componentName: 'FieldWidget',
  components: {
    FormItemWrapper,
    ElButton,
    ElCascader,
    BaidumapSelectAddress,
    ElDialog,
    ElInput
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
      bMapVisible: false,
      Plus: Plus,
      address: '',
      showJWD: ''
    }
  },
  computed: {
    showFullPath() {
      return (this.field.options.showAllLevels === undefined) || !!this.field.options.showAllLevels
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
    if (this.field.options.mapCenter) {
      this.initLocationCenter()
    }
  },

  mounted() {
    this.handleOnMounted()
  },

  beforeUnmount() {
    this.unregisterFromRefList()
  },

  methods: {
    initLocationCenter() {

    },
    showBDMAP() {
      this.bMapVisible = true
    },
    changeAddress(data) {
      this.fieldModel = data
      this.address = data.address
      try {
        let tempJW = JSON.parse(data.center)
        this.showJWD = '经度:' + tempJW.lng + ',纬度' + tempJW.lat
      } catch (error) {
        console.warn('error_changeAddress', error)
      }
      this.handleChangeEvent(this.fieldModel)
      this.bMapVisible = false
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

  .address_div {
    display: flex;
    flex-direction: column;
  }

  .address_span {
    margin: 0 10px;
    color: var(--el-color-primary);
  }

  .address_span_jw {
    margin: 0 10px;
    font-size: 10px;
    color: #333;
  }

</style>
