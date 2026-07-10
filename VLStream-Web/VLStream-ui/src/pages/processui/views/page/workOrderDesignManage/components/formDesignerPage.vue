<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div class="form_designer_page">
    <VFormDesigner ref="vfDesignerRef" :global-dsv="globalDsv" @saveForm="saveForm" @saveForComponent="openSaveComponent" @close="closeDesigner" />
    <el-dialog
      v-model="dialogVisible"
      title="新增自定义组件"
      align-center
      width="46%"
      :close-on-click-modal="false"
    >
      <el-form ref="ruleFormRef" :model="formComponents" :rules="rules" label-width="80px">
        <el-form-item label="组件名称" prop="formName">
          <el-input v-model="formComponents.formName" placeholder="请输入组件名称" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="formComponents.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="text-align: right;">
          <el-button @click="dialogVisible = false">
            取消
          </el-button>
          <el-button type="primary" @click="saveForComponent">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, nextTick } from 'vue'
import { getForm, updateForm, addForm, listForm } from '@/api/processui/index.ts'
import { ElMessage } from 'element-plus'

const globalDsv = ref({
  testApiHost: 'http://www.test.com/api',
  testPort: 8080
})

const vfDesignerRef = ref(null)

const emits = defineEmits(['close'])
const closeDesigner = () => {
  emits('close')
}

const props = defineProps({
  formObject: {
    type: Object,
    default: null
  }
})
const form = ref({
  content: undefined,
  formName: '',
  remark: '',
  isFormComponents: '0'
})
function getFormDetail() {
  const params = {
    formId: props.formObject.formId
  }
  getForm(params).then(res => {
    if (res.code === 200 && res.data) {
      form.value.formId = res.data.formId
      form.value.formName = res.data.formName
      form.value.remark = res.data.remark
      if (res.data.content) {
        try {
          form.value.content = JSON.parse(res.data.content)
          let contentObj = JSON.parse(res.data.content)
          if (contentObj.formConfig) {
            vfDesignerRef.value.setFormJson(contentObj)
          }
        } catch (error) {
          console.error('解析表单内容失败')
        }
      }
    } else {
      vfDesignerRef.value.setFormJson({})
    }
  })
}
async function saveForm(data) {
  if (form.value.formName === '') {
    return
  }
  if (data) {
    form.value.content = JSON.stringify(data)
  }
  let res = null
  if (form.value.formId) {
    res = await updateForm(form.value)
  }
  if (res.code === 200) {
    ElMessage({
      type: 'success',
      message: '保存成功'
    })
    dialogVisible.value = false
    // // 路由返回上一层
    // emits('close')
  } else {
    ElMessage.error(res.msg)
  }
}

const dialogVisible = ref(false)
const formComponents = ref({
  content: undefined,
  formName: '',
  remark: '',
  isFormComponents: '1'
})
async function saveForComponent() {
  if (formComponents.value.formName === '') {
    ElMessage({
      type: 'warning',
      message: '请输入组件名称'
    })
    return
  }
  let res = await addForm(formComponents.value)
  if (res.code === 200) {
    ElMessage({
      type: 'success',
      message: '操作成功'
    })
    dialogVisible.value = false
    getFormComponentList()
  } else {
    ElMessage.error(res.msg)
  }
}

const componentList = ref([])
const queryParams = reactive({
  pageNum: 1,
  pageSize: 999,
  isFormComponents: '1'
})
function getFormComponentList() {
  listForm(queryParams).then(response => {
    componentList.value = response.rows || []
    // 生产环境组件可能异步加载，使用 nextTick + 重试
    setCustomFieldsWithRetry()
  })
}

// 重试机制：确保组件完全初始化后再调用
function setCustomFieldsWithRetry(retryCount = 0) {
  nextTick(() => {
    if (vfDesignerRef.value && typeof vfDesignerRef.value.setCustomFields === 'function') {
      vfDesignerRef.value.setCustomFields(componentList.value)
    } else if (retryCount < 3) {
      // 最多重试3次，每次间隔300ms
      setTimeout(() => setCustomFieldsWithRetry(retryCount + 1), 300)
    } else {
      console.warn('VFormDesigner 组件初始化超时，setCustomFields 调用失败')
    }
  })
}

function openSaveComponent(data) {
  dialogVisible.value = true
  formComponents.value.content = JSON.stringify(data)
}

onMounted(() => {
  if (props.formObject.formId) {
    getFormDetail()
  }
  getFormComponentList()
})

const triggerSaveForm = () => {
  vfDesignerRef.value.saveForm(0)
}

defineExpose({
  triggerSaveForm
})

</script>

<style lang="scss">

.form_designer_page{
  height: 100%;
  /* 拖拽组件会选中文字的 */
  user-select: none;
  &_header{
    height: 60px;
    display: flex;
    align-items: center;
    span {
      margin-left: 10px;
      color: #666;
      font-size: 14px;
    }
  }
}

</style>
