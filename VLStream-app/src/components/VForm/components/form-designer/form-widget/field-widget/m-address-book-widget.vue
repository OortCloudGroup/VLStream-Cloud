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
        <div class="preview-fileList">
          <div v-for="(item, index) in fieldModel" :key="index" class="user_dept_group" :class="{ readmode: field.options.readonly || isReadMode }">
            <template v-if="item.type === 'user'">
              <ID2HeadPic :src="item.photo" class="user_dept_group_img" />
              <span class="user_dept_group_span">{{ item.user_name }}</span>
            </template>
            <template v-if="item.type === 'dept'">
              <img v-if="item.dept_photo" :src="item.dept_photo" class="user_dept_group_img" />
              <img v-else src="@/assets/img/dept_default.png" class="user_dept_group_img" />
              <span class="user_dept_group_span">{{ item.dept_name }}</span>
            </template>
            <span v-if="!isReadMode && !fieldDisabled" class="file-action el-icon-delete" @click="handleFileDelete(file, index)">
              <van-icon name="cross" color="#fff" size="10" />
            </span>
          </div>
          <div v-if="!isReadMode && !fieldDisabled" class="add_plus" @click="contactVisible=true">
            <van-icon name="plus" size="24" color="#99999950" />
          </div>
        </div>
      </template>
    </van-field>
    <oort-popup v-model="contactVisible" position="right" style="width: 100%">
      <Contact
        :is-choose-person="field.options.selectMod !== 2"
        :is-choose-dept="field.options.selectMod !== 3"
        :dept-list="fieldModel ? fieldModel.filter(item => item.type === 'dept'):[]"
        :person-list="fieldModel? fieldModel.filter(item => item.type === 'user'):[]"
        @editClose="contactClose"
      />
    </oort-popup>
  </form-item-wrapper>
</template>

<!--

#### isSingle

是否单选人员

#### isSingleDept

是否单选部门

#### isChoosePerson

是否可以勾选人员； 默认true, 如果为false ， 人员不显示

#### isChooseDept

是否可以勾选部门； 默认false, 如果为true， 出现勾选框

  selectMod: 1, // 选择模式 1, 部门和人都可以选， 2 只选部门  3 只选人
        selectScope: 1, // 选择范围  1,全部  2 本部门 3 自定义
        defaultValueType: 1, // 1，无， 2 固定值， 3 提交人， 4 提交部门， 5 创建人， 6 创建人部门
        scopeValue: '', // 自定义范围
        defaultValue: '' // 固定值 -->

<script>
import { toRefs, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from '~@/utils/i18n'
import { useField } from '~@/components/form-designer/form-widget/field-widget/fieldMixin'
import FormItemWrapper from '~@/components/form-designer/form-widget/field-widget/form-item-wrapper'
import { deepClone } from '~@/utils/util'
import OortPopup from '@/components/popup/oort_popup.vue'
import Contact from '@/components/contactTree/Contact.vue'

export default {
  componentName: 'FieldWidget',
  name: 'MAddressBookWidget',
  components: {
    FormItemWrapper,
    OortPopup,
    Contact
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
    const { i18nt } = useI18n()
    const data = reactive({
      contactVisible: false,
      ImagePreview: {},
      fieldModel: [],
      fileList: [], // 上传文件列表

      noChangeEventFlag: true, // van-field组件没有change事件！！
      oldFieldValue: null, // field组件change之前的值
      rules: [],
      uploadBtnHidden: false,

      uploadData: {
        key: '' // 七牛云上传文件名
        // token: '',  //七牛云上传token

        // policy: '',  //又拍云上传policy
        // authorization: '',  //又拍云上传签名
      },

      uploadHeaders: {}
    })
    const fieldMixin = useField(data)

    onMounted(() => {
      fieldMixin.handleOnMounted()
    })

    onBeforeUnmount(() => {
      fieldMixin.unregisterFromRefList()
    })

    const contactClose = ({ userList, deptList }) => {
      let tempArr = []
      userList.forEach(item => {
        tempArr.push({
          type: 'user',
          user_id: item.user_id,
          user_name: item.user_name,
          photo: item.photo,
          tenant_id: item.tenant_id
        })
      })
      deptList.forEach(item => {
        tempArr.push({
          type: 'dept',
          dept_id: item.dept_id,
          dept_code: item.dept_code,
          dept_name: item.dept_name,
          tenant_id: item.tenant_id,
          dept_photo: item.dept_photo
        })
      })
      data.contactVisible = false
      data.fieldModel = tempArr
      fieldMixin.handleChangeEvent(tempArr)
      updateFieldModelAndEmitDataChangeForRemove(null, tempArr, -1)
    }

    const updateFieldModelAndEmitDataChangeForRemove = (file, fileList, deletedIndex) => {
      let oldValue = deepClone(data.fieldModel)
      if (deletedIndex > -1) {
        data.fieldModel.splice(deletedIndex, 1)
        data.fileList.splice(deletedIndex, 1) // 手动删除列表中的文件，m-picture-upload不需要手动处理，删除用的是组件内部方法
      }
      fieldMixin.syncUpdateFormModel(data.fieldModel)
      fieldMixin.emitFieldDataChange(data.fieldModel, oldValue)
    }

    const handleFileDelete = (file, index) => {
      updateFieldModelAndEmitDataChangeForRemove(file, data.fileList, index)
      data.uploadBtnHidden = data.fieldModel.length >= props.field.options.limit
    }

    fieldMixin.registerToRefList()
    fieldMixin.initFieldModel()
    fieldMixin.initEventHandler()
    fieldMixin.buildFieldRules()

    fieldMixin.handleOnCreated()

    return {
      i18nt,
      ...toRefs(data),
      ...fieldMixin,
      handleFileDelete,
      contactClose
    }
  }
}
</script>

<style scoped lang="scss">
.preview-fileList {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  width: 100%;

  .user_dept_group {
    width: 68px;
    height: 68px;
    background-color: #f7f8fa;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: relative;
    &_img {
      margin-top: 4px;
      height: 30px;
      width: 30px;
      border-radius: 100%;
      height: auto;
    }
    &_span {
      margin-top: 2px;
      height: 32px;
      font-size: 12px;
      line-height: 1;
      color: #333;
    }
    &:hover {
      background-color: #f5f7fa;
    }

    &.readmode {
      background-color: transparent;
    }
  }
  .file-action {
    position: absolute;
    top: -0px;
    right: -0px;
    width: 14px;
    height: 14px;
    background-color: #000;
    cursor: pointer;
    border-bottom-left-radius: 80%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.add_plus {
  width: 68px;
    height: 68px;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

</style>
