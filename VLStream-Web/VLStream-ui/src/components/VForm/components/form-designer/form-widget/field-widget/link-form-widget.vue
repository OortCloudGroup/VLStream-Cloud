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
    <div
      ref="fieldEditor"
      class="hide-spin-button"
    >
      <el-button @click="showLinkForm">
        <svg-icon :icon-class="field.options.buttonIcon" /> {{ field.options.buttonText }}
      </el-button>
    </div>
    <el-dialog
      v-model="linkFormVisible"
      :title="field.options.buttonText"
      width="65%"
      :show-close="true"
      :destroy-on-close="true"
    >
      <div v-if="linkFormJson && linkFormVisible" class="form-render-wrapper">
        <VFormRender
          ref="linkForm"
          :form-json="linkFormJson"
          :form-data="{}"
          :preview-state="true"
          :option-data="{}"
          :global-dsv="{}"
        />
        <div class="form-render-footer">
          <el-button type="primary" @click="confirmLinkForm">
            确定
          </el-button>
        </div>
      </div>
    </el-dialog>
  </form-item-wrapper>
</template>

<script>
import FormItemWrapper from './form-item-wrapper'
import emitter from '~@/utils/emitter'
import i18n from '~@/utils/i18n'
import fieldMixin from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import SvgIcon from '~@/components/svg-icon/index'
import { ElButton, ElDialog } from 'element-plus'
import { getForm } from '@/api/processui/index.ts'

export default {
  name: 'LinkFormWidget',
  componentName: 'FieldWidget',
  components: {
    ElButton,
    FormItemWrapper,
    SvgIcon,
    ElDialog
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
      linkFormJson: '',
      linkFormVisible: false
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
    'field.options.linkFormID'(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.getFormDetail()
      }
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
    this.getFormDetail()
  },

  mounted() {
    this.handleOnMounted()
  },

  beforeUnmount() {
    this.unregisterFromRefList()
  },

  methods: {
    // 确定表单内容详情
    confirmLinkForm() {
      this.$refs.linkForm.getFormData().then(async(formData) => {
        this.fieldModel = JSON.stringify(formData)
        this.handleChangeEvent(this.fieldModel)
        this.linkFormVisible = false
      })
    },
    showLinkForm() {
      if (this.linkFormJson) {
        this.linkFormVisible = true
        // 禁用
        if (this.field.options.disabled) {
          this.$nextTick(() => {
            this.$refs.linkForm.disableForm()
          })
        }
      } else {
        this.$message.error('未关联表单')
      }
    },
    getFormDetail() {
      if (!this.field.options.linkFormID) {
        return
      }
      const params = {
        formId: this.field.options.linkFormID
      }
      getForm(params).then(res => {
        if (res.code === 200 && res.data) {
          if (res.data.content) {
            try {
              let contentObj = JSON.parse(res.data.content)
              if (contentObj.formConfig) {
                this.linkFormJson = contentObj
                if (this.fieldModel) {
                  try {
                    let tempObj = JSON.parse(this.fieldModel)
                    this.linkFormJson = this.fillInDefaultValue(this.linkFormJson, tempObj)
                  } catch (error) {
                    console.log(error)
                  }
                }
              }
            } catch (error) {
              console.error('解析表单内容失败')
            }
          }
        } else {
          this.linkFormJson = {}
        }
      })
    },
    fillInDefaultValue(linkFJ, tempObj) {
      for (let key in tempObj) {
        if (linkFJ.widgetList) {
          linkFJ.widgetList.forEach(item => {
            if (item.id === key) {
              item.options.defaultValue = tempObj[key]
            }
            if (item.cols) {
              item.cols.forEach(subItem => {
                this.fillInDefaultValue(subItem, tempObj)
              })
            }
          })
        }
      }
      return linkFJ
    }
  }
}
</script>

<style lang="scss" scoped>
  @import "../../../../styles/global.scss"; /* form-item-wrapper已引入，还需要重复引入吗？ */
  .form-render-wrapper.h5-layout {
    margin: 0 auto;
    width: 420px;
    border-radius: 10px;
    box-shadow: 0 0 1px 1px var(--el-color-primary);
    height: calc(100vh - 175px);
    overflow-y: auto;
    overflow-x: hidden;
  }

  .form-render-wrapper.pad-layout {
    margin: 0 auto;
    width: 960px;
    border-radius: 10px;
    box-shadow: 0 0 1px 1px var(--el-color-primary);
    height: calc(100vh - 175px);
    overflow-y: auto;
    overflow-x: hidden;
  }

  .form-render-wrapper {
    display: flex;
    flex-direction: column;
  }

  .form-render-footer {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }

  :deep(.form-render-footer.el-form-item) {
    margin-bottom: 10px;
  }

</style>
