<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2024-11-15 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <div>
    <el-form ref="ruleFormRef" :model="form" :rules="rules" label-width="120">
      <el-form-item label="表单名称" prop="formName">
        <el-input v-model="form.formName" placeholder="请输入表单名称" />
      </el-form-item>
      <el-form-item label="表单类型" prop="formType" class="">
        <div class="messModeBox flexRowAC">
          <div class="messModeItem flexRowAC" :class="{act:form.formType ===0}" @click="form.formType=0">
            <oort-svg-icon class="messModeItem_img" name="select" />
            <img class="messModeItem_dot" src="@/assets/img/processui/formType1.png" />
            流式布局
          </div>
          <div class="messModeItem flexRowAC" :class="{act:form.formType===1}" @click="form.formType=1">
            <oort-svg-icon class="messModeItem_img" name="select" />
            <img class="messModeItem_dot" src="@/assets/img/processui/formType2.png" />
            签批卡片式布局
          </div>
        </div>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <div class="flexRowAC" style="position: relative;width: 100%;">
          <el-input
            v-model="form.remark"
            style="flex: 1"
            type="textarea"
            placeholder="请输入备注"
            :rows="4"
            show-word-limit
            maxlength="256"
          />
          <aiIconRemark
            v-model="form.remark"
            :number="256"
            mod="备注"
            :keyword="form.formName"
            keyword-empty-tips="请先输入表单名称"
          />
        </div>
      </el-form-item>
    </el-form>
    <div style="text-align: right;">
      <el-button @click="emits('close')">
        取消
      </el-button>
      <el-button v-preReClick type="primary" @click="saveCategory(ruleFormRef)">
        保存
      </el-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { addForm, updateForm } from '@/api/processui'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'

const props = defineProps(['item', 'app', 'type']) /* type 工单1 流程0 */
const emits: any = defineEmits(['handle', 'close'])
let ruleFormRef = ref(null)
const form = reactive({
  formName: '',
  content: undefined,
  categoryId: '',
  categoryName: '全部',
  remark: '',
  formId: undefined,
  formType: 0,
  isFormComponents: '0' // 是否是组件还是表单, 0表单  1 组件
})
const rules = reactive({
  formName: [
    { required: true, message: '请输入表单名称', trigger: 'blur' }
  ]
})

// 保存
const saveCategory = async(formEl: any) => {
  if (!formEl) return
  await formEl.validate(async(valid: boolean) => {
    if (valid) {
      let res: any = form.formId ? await updateForm(form) : await addForm(form)
      if (res.code === 200) {
        form.formId ? ElMessage.success('表单编辑成功') : ElMessage.success('表单新增成功')
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

if (props.app) {
  let row = props.app
  form.categoryId = row.appId
  form.categoryName = row.applicationName
  form.formName = row.formName
  form['modelId'] = row.modelId
  form['type'] = 1 // 工单1 流程0
  if (props.type === 0) {
    form['type'] = props.type
  }
}

if (props.item) {
  let row = props.item
  form.formName = row.formName
  form.content = row.content
  form.formId = row.formId
  form.remark = row.remark
}

</script>
<style lang="scss" scoped>
.el-form{
  width: 90%;
}

// 点对点
.messModeBox {
  gap: 12px;

  .messModeItem {
    cursor: pointer;
    justify-content: center;
    flex-direction: column;
    width: 248px;
    border-radius: 4px;
    background: hsl(0, 0%, 100%);
    border: 1px solid hsl(0, 0%, 96.1%);

    .messModeItem_img {
      display: none;
    }
  }

  .messModeItem.act {
    color: var(--el-color-primary);
    border-radius: 4px;
    background: hsl(0, 0%, 100%);
    border: 1px solid var(--el-color-primary);
    position: relative;

    .messModeItem_img {
      display: block;
    }
  }

  .messModeItem_img {
    position: absolute;
    top: 0;
    left: 0;
    width: 20px;
    height: 16px;
  }

  .messModeItem_dot {
    width: 200px;
    height: auto;
  }
}

</style>
