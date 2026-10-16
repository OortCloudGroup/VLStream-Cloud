<template>
  <el-dialog :model-value="modelValue" :title="own ? $tp('修改密码') : $tp('重置密码')"
    width="460px" destroy-on-close :close-on-click-modal="false" @closed="clearForm"
    @update:model-value="emit('update:modelValue', $event)">
    <p v-if="!own">{{ $tp('账号') }}: {{ account }}</p>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item v-if="own" :label="$tp('旧密码')" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-form-item :label="$tp('新密码')" prop="password">
        <el-input v-model="form.password" type="password" show-password autocomplete="new-password" maxlength="20" />
      </el-form-item>
      <el-form-item :label="$tp('确认密码')" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" maxlength="20" />
      </el-form-item>
      <p>{{ $tp('密码长度必须为 5 至 20 个字符') }}</p>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="emit('update:modelValue', false)">{{ $tp('取消') }}</el-button>
      <el-button type="primary" :loading="saving" @click="submit">{{ $tp('确定') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { translatePhrase as uiText } from '@/i18n'
import { changeOwnPassword, resetUserPassword } from '@/api/system/user'
import { localPasswordRules } from '@/utils/localPassword'

const props = defineProps({ modelValue: Boolean, own: Boolean, userId: String, account: String })
const emit = defineEmits(['update:modelValue', 'saved'])
const formRef = ref()
const saving = ref(false)
const form = reactive({ oldPassword: '', password: '', confirmPassword: '' })
const rules = computed(() => ({
  ...localPasswordRules(() => form.password, uiText),
  oldPassword: [{ required: true, message: uiText('请输入旧密码'), trigger: 'blur' }]
}))
function clearForm() {
  Object.assign(form, { oldPassword: '', password: '', confirmPassword: '' })
  formRef.value?.clearValidate()
}
async function submit() {
  if (saving.value) return
  saving.value = true
  try {
    if (!await formRef.value?.validate().catch(() => false)) return
    const response = props.own
      ? await changeOwnPassword(form.oldPassword, form.password)
      : await resetUserPassword(props.userId, form.password)
    if (response?.code !== 200) {
      ElMessage.error(response?.msg || uiText('修改密码失败'))
      return
    }
    ElMessage.success(uiText(props.own ? '修改密码成功' : '重置密码成功'))
    clearForm()
    emit('saved')
    emit('update:modelValue', false)
  } catch {
    // Request errors may carry plaintext request bodies; never log them.
    ElMessage.error(uiText('修改密码失败'))
  } finally {
    saving.value = false
  }
}
</script>
