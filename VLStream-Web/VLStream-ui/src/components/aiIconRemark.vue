<!--
 *@Created by: 兰舰
 * Email: gglanjian@qq.com
 * Phone: 16620805419
 * @Date: 2025-03-13 11:45:51
 * @Last Modified by:  兰舰
 * @Copyright aPaaS-front-team. All rights reserved.
!-->
<template>
  <el-popover :visible="visible" placement="top-end" :width="selfDefine?clacPXToVW(320):clacPXToVW(180)" trigger="click">
    <span v-if="!selfDefine" class="tips">{{ statusText }}</span>
    <div v-else class="user_input">
      <el-form ref="ruleFormRef" class="user_input_form" :model="form" label-width="auto" :rules="rules">
        <el-form-item label="关键词" prop="keyword">
          <el-input v-model="form.keyword" placeholder="请输入关键词" />
        </el-form-item>
        <el-form-item label="类型描述" prop="mod">
          <el-input v-model="form.mod" placeholder="请输入关键词" />
        </el-form-item>
        <el-form-item label="字数限制" prop="number">
          <el-input v-model.number="form.number" type="number" placeholder="请输入关键词" />
        </el-form-item>
        <el-form-item>
          <div class="ai_button_group">
            <el-button type="primary" @click="submitForm(ruleFormRef)">
              生成
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
    <template #reference>
      <div class="aiIconPage flexRowAC" @click="AIBtnClick">
        <oort-svg-icon class="aiIconRemark" width="14" height="14" name="ai" />
        AI
      </div>
    </template>
  </el-popover>
</template>

<script setup>
import { clacPXToVW } from '@/utils/index'
import { textCompletion, imageGeneration, getTextImage, textImageDownload } from '@/api/aiApaas/index'
import { ref, reactive } from 'vue'
import { useUserStore } from '@/store/modules/useraPaas'

const store = useUserStore()

const props = defineProps({
  selfDefine: { // 是否需要用户自己输入关键字等参数来生成. 默认false.
    type: Boolean,
    default: false
  },
  disabled: { // 是否禁用. 默认false. 禁用显示功能正在开发中的提示
    type: Boolean,
    default: false
  },
  modelValue: {
    type: String,
    default: ''
  },
  keyword: {
    type: String,
    default: ''
  },
  mod: {
    type: String,
    default: '简介'
  },
  number: {
    type: Number,
    default: 100
  },
  keywordEmptyTips: {
    type: String,
    default: '关键词不能为空'
  },
  aiType: {
    type: String,
    default: 'text'
  }
})

const visible = ref(false)

const emits = defineEmits(['update:modelValue'])
const statusText = ref('功能正在开发中，敬请期待')
const genderText = async() => {
  visible.value = true
  if (!props.keyword) {
    statusText.value = props.keywordEmptyTips
    return
  }
  if (!props.mod) {
    statusText.value = '类型描述不能为空'
    return
  }
  if (!props.number) {
    statusText.value = '字数限制不能为空'
    return
  }
  const params = {
    accessToken: store.token,
    keyword: props.keyword,
    mod: props.mod,
    number: props.number
  }
  clickDisabled.value = true
  statusText.value = '内容生成中...'
  emits('update:modelValue', '内容生成中...')
  let res = await textCompletion(params)
  if (res.code === 200) {
    statusText.value = '生成成功'
    emits('update:modelValue', res.data.answer)
    setTimeout(() => {
      clickDisabled.value = false
      visible.value = false
    }, 500)
  }
}

const clickDisabled = ref(false)
const genderImage = async() => {
  visible.value = true
  if (!props.keyword) {
    statusText.value = props.keywordEmptyTips
    return
  }
  const params = {
    accessToken: store.token,
    keyword: props.keyword
  }
  statusText.value = '图片生成中...'
  clickDisabled.value = true
  let res = await imageGeneration(params)
  if (res.code === 200) {
    getTextImageStatus(res.data.workflow_id)
  }
}

const getTextImageStatus = async(workflow_id) => {
  const params = {
    accessToken: store.token,
    workflow_id: workflow_id
  }
  let res = await getTextImage(params)
  if (res.code === 200) {
    if (res.data.status !== 'succeeded') {
      if (res.data.status === 'failed') {
        clickDisabled.value = false
        statusText.value = '生成失败'
      } else {
        setTimeout(() => {
          getTextImageStatus(workflow_id)
        }, 1000)
      }
    } else {
      if (res.data.output['回复'] && res.data.output['回复'].length > 0 && res.data.output['回复'][0].url) {
        getImageDownLoad(res.data.output['回复'][0].url)
      } else {
        clickDisabled.value = false
        statusText.value = '获取url失败'
      }
      // emits('update:modelValue', res.data.answer)
      setTimeout(() => {
        visible.value = false
      }, 500)
    }
  }
}

const getImageDownLoad = async(url) => {
  const params = {
    accessToken: store.token,
    base64: true,
    url: url
  }
  let res = await textImageDownload(params)
  if (res.code === 200) {
    statusText.value = '生成成功'
    clickDisabled.value = false
    emits('update:modelValue', res.data.base64)
    setTimeout(() => {
      visible.value = false
    }, 500)
  }
}

const AIBtnClick = () => {
  if (props.disabled || props.selfDefine || clickDisabled.value) {
    return
  }
  if (props.aiType === 'text') {
    genderText()
  }
  if (props.aiType === 'image') {
    genderImage()
  }
}

const form = reactive({
  keyword: props.keyword || '',
  mod: props.mod || '',
  number: props.number || 100
})
const rules = reactive({
  keyword: [
    { required: true, message: '请输入关键词', trigger: 'blur' }
  ],
  mod: [
    { required: true, message: '请输入关键词', trigger: 'blur' }
  ],
  number: [
    { required: true, message: '字数限制', trigger: 'blur' }
  ]
})

const ruleFormRef = ref(null)
const submitForm = async(formEl) => {
  if (!formEl) return
  await formEl.validate(async(valid, fields) => {
    if (valid) {
      const params = {
        accessToken: store.token,
        keyword: form.keyword,
        mod: form.mod,
        number: form.number
      }
      statusText.value = '内容生成中...'
      emits('update:modelValue', '内容生成中...')
      let res = await textCompletion(params)
      if (res.code === 200) {
        statusText.value = '生成成功'
        emits('update:modelValue', res.data.answer)
        setTimeout(() => {
          visible.value = false
        }, 500)
      }
    } else {
      console.log('error submit!', fields)
    }
  })
}

</script>

<style lang="scss" scoped>
/*引入 position: relative*/
.aiIconPage {
  cursor: pointer;
  justify-content: center;
  width: 54px;
  position: absolute;
  background-color: var(--el-color-primary-hb);
  top: 0;
  right: -64px;
  border-radius: 4px;
  color: var(--el-color-primary);
  font-size: 14px;
  line-height: 22px;
  gap: 6px;
}

.tips {
  font-size: 12px;
  color: #999;
}

.user_input {
  width: 100%;
  padding: 4px;
}

.user_input_form {
  width: 100%;
}

.ai_button_group {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

</style>
