<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑标签' : '新增标签'"
    width="400px"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      label-position="left"
    >
      <el-form-item label="标签名称" prop="name" required>
        <el-input
          v-model="form.name"
          placeholder="请输入标签名称"
          clearable
        />
      </el-form-item>
      
      <el-form-item label="标签颜色" prop="color" required>
        <div class="color-picker-section">
          <el-color-picker 
            v-model="form.color" 
            :predefine="predefineColors"
            show-alpha
          />
          <div class="color-preview" :style="{ backgroundColor: form.color }">
            <span class="preview-text">{{ form.name || '标签预览' }}</span>
          </div>
        </div>
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import {computed, ref, watch} from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  initialData: {
    type: Object,
    default: () => ({})
  },
  predefineColors: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formRef = ref(null)
const form = ref({
  id: null,
  name: '',
  color: '#409EFF'
})

const rules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { min: 1, max: 10, message: '长度在 1 到 10 个字符', trigger: 'blur' }
  ],
  color: [
    { required: true, message: '请选择标签颜色', trigger: 'change' }
  ]
}

const resetForm = () => {
  form.value = {
    id: null,
    name: '',
    color: '#409EFF'
  }
  formRef.value?.resetFields()
}

const syncForm = () => {
  resetForm()
  form.value = {
    id: props.initialData?.id ?? null,
    name: props.initialData?.name || '',
    color: props.initialData?.color || '#409EFF'
  }
}

watch(() => props.initialData, () => {
  if (visible.value) {
    syncForm()
  }
}, { deep: true })

watch(() => props.modelValue, (val) => {
  if (val) {
    syncForm()
  } else {
    resetForm()
  }
})

const handleClose = () => {
  visible.value = false
}

const handleConfirm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  emit('submit', { ...form.value })
}
</script>

<style scoped>
.color-picker-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.color-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 32px;
  border-radius: 16px;
  color: white;
  font-size: 12px;
  font-weight: 500;
}

.preview-text {
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.dialog-footer {
  text-align: right;
}
</style>
