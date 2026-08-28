<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
  Created by: ChaoQun Lei
  Updated by: ChaoQun Lei
-->

<!--
 * @Created by:
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div>
    <el-form ref="ruleFormRef" :model="form" :rules="rules" label-width="120">
      <el-form-item label="模型标识" prop="modelKey">
        <el-input v-model="form.modelKey" disabled placeholder="请输入模型标识" />
      </el-form-item>
      <!-- task - group -->
      <el-form-item v-if="props.isType===0" label="分组" prop="processCategoryId">
        <el-select v-model="form.processCategoryId" placeholder="请选择" clearable>
          <el-option v-for="(item,i) in menuList" :key="i" :label="item.processCategoryName" :value="item.processCategoryId" />
        </el-select>
      </el-form-item>
      <el-form-item label="模型名称" prop="modelName">
        <el-input v-model="form.modelName" placeholder="请输入模型名称" />
      </el-form-item>
      <el-form-item label="模型图标" prop="code">
        <div class="licenseBox">
          <el-upload
            class="avatar-uploader"
            :action="upfileURL"
            :headers="headers"
            accept=".png, .jpg, .jpeg"
            :show-file-list="false"
            :multiple="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <oort-img v-if="iconId" :src="iconId" class="avatar1" />
            <div v-else class="licenseItem flexRowAC">
              <img class="avatar1" src="@/assets/img/payui/add.png" />
            </div>
          </el-upload>
        </div>
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <div class="flexRowAC" style="position: relative;width: 100%;">
          <el-input
            v-model="form.description"
            type="textarea"
            rows="4"
            style="flex: 1"
            placeholder="请输入描述"
            maxlength="200"
            show-word-limit
          />
          <aiIconRemark
            v-model="form.description"
            :number="200"
            mod="描述"
            :keyword="form.modelName"
            keyword-empty-tips="请先输入模型名称"
          />
        </div>
      </el-form-item>
      <el-form-item label="移动端显示">
        <el-switch
          v-model="form.showMobile"
          active-color="#13ce66"
          inactive-color="#999"
          :active-value="1"
          :inactive-value="0"
        />
      </el-form-item>
    </el-form>
    <div style="text-align: right;">
      <el-button class="bigBtn common_btn" @click="emits('close')">
        取消
      </el-button>
      <el-button v-preReClick class="bigBtn" type="primary" @click="saveCategory(ruleFormRef)">
        保存
      </el-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { addModel, copyModel, processCategoryList, updateModel } from '@/api/processui/approval'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import Config from '@/config'
import { getStoredWorkOrderAppContext } from '@/utils/workOrderAppContext'

const props = defineProps(['item', 'type', 'app', 'isType'])/* type:0新增 1copy 2编辑 isType:工单1 流程0 */
const emits: any = defineEmits(['handle', 'close'])
const headers = ref(Config.headers)
const upfileURL = ref(Config.URL + Config.gateWay + 'apaas-fastdfsservice/fastdfs/v1/uploadFile')
let ruleFormRef = ref(null)
const iconId = ref('')
const addModelType = ref(props.type || 0) // 0 Add 、1, copy , 2
let appObj = ref<any>('') // task app
let menuList = ref<any>([]) // group( )
appObj.value = props.app?.appId ? props.app : getStoredWorkOrderAppContext() || {}
const form = reactive({
  modelId: undefined,
  modelKey: 'Process_' + new Date().getTime(),
  modelName: '',
  category: props.app?.appId || appObj.value?.appId,
  description: undefined, //
  processCategoryId: undefined, // group
  showMobile: 0,
  wfCategory: undefined,
  workOrderCategory: undefined
})
const rules = reactive({
  modelKey: [
    { required: true, message: '请输入模型标识', trigger: 'blur' }
  ],
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ],
  processCategoryId: [
    { required: true, message: '请选择', trigger: 'blur' }
  ]
})

// group
const processCategoryFn = () => {
  let data = {
    pageNum: 1,
    pageSize: 999,
    appId: appObj.value?.appId
  }
  processCategoryList(data).then((res: any) => {
    menuList.value = res.rows || []
  })
}

// model
function handleAvatarSuccess(res) {
  if (res.code === 200) {
    iconId.value = res.data.url
  } else {
    ElMessage.error(res.msg + '，上传失败')
  }
}

//
const beforeAvatarUpload = (file: any) => {
  let isLt2M = file.size / 1024 / 1024 < 5
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 5MB!')
  }
  return isLt2M
}

//
const saveCategory = async(formEl: any) => {
  if (!formEl) return
  await formEl.validate(async(valid: boolean) => {
    if (valid) {
      if (!form.category) {
        ElMessage.error('工单应用分类未初始化，无法保存模型')
        return
      }
      // form- work order is workflow-form
      form['type'] = props.isType // 0 workflow 1 work order
      form['categoryId'] = appObj.value?.categoryId // form , whether need to form
      let c = form.category
      // work order-workOrderCategory workflow-wfCategory
      if (props.isType === 0) { // work order1 workflow0
        form['wfCategory'] = c
      } else {
        form.workOrderCategory = c
      }
      let res: any = null
      if (iconId.value) {
        form['iconId'] = iconId.value
      }
      if (form.modelId) {
        if (addModelType.value === 2) {
          res = await updateModel(form)
        } else {
          res = await copyModel(form)
        }
      } else {
        res = await addModel(form)
      }
      if (res.code === 200) {
        if (addModelType.value === 0) ElMessage.success('新增模型成功')
        if (addModelType.value === 1) ElMessage.success('复制模型成功')
        if (addModelType.value === 2) ElMessage.success('编辑模型成功')
        emits('close')
        emits('handle')
      } else {
        ElMessage.error(res.msg)
      }
    } else {
      return false
    }
  })
}

if (props.item) {
  let id_ = props.item?.appId || props.item?.synthesisId
  if (props.isType === 0) { // work order1 workflow0
    form['wfAppAll'] = true
    form['wfCategory'] = id_
  } else {
    // work order-workOrderCategory workflow-wfCategory
    // work order WorkOrderAppAll work order -WorkOrderSynthesisAll workflow -wfAppAll workflow -wfAppAll
    form['WorkOrderAppAll'] = true
    form.workOrderCategory = id_
  }
  form.category = id_
}
if (props.item && props.type) {
  let row = props.item
  if (props.type === 1) { // type:0新增 1copy 2编辑
    form.modelName = row.modelName + '-Copy'
    addModelType.value = 1
    form['copyModelId'] = row.modelId
  } else {
    addModelType.value = 2
    form.modelName = row.modelName
    form.modelKey = row.modelKey
  }
  form.modelId = row.modelId
  form.processCategoryId = row.processCategoryId
  form.description = row.description
  form.category = row.category
  form.showMobile = row.showMobile
  iconId.value = row.iconId
}

onMounted(() => {
  processCategoryFn()
})

</script>
<style lang="scss" scoped>
.el-form{
  width: 90%;
}

.licenseBox {
  cursor: pointer;
  width: 108px;
  height: 108px;
  border-radius: 4px;

  .avatar-uploader, :deep(.el-upload), licenseItem {
    width: 100%;
    height: 100%;
  }

  .licenseItem {
    flex-direction: column;
    justify-content: center;
  }

  .avatar1 {
    width: 108px;
    height: 108px;
    background-size: cover;
  }
}

</style>
