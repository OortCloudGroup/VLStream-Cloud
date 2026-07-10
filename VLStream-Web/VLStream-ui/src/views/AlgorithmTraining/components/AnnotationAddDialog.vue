<template>
  <el-dialog
    v-model="visible"
    title="新增标注"
    width="600px"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      label-position="left"
    >
      <el-form-item label="名称" prop="name" required>
        <el-input
          v-model="form.name"
          placeholder="请输入标注名称"
          clearable
        />
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="4"
          placeholder="请输入备注信息"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="标注类型" prop="type" required>
        <div class="annotation-type-grid">
          <div
            v-for="typeOption in annotationTypes"
            :key="typeOption.value"
            class="type-option"
            :class="{ active: form.type === typeOption.value }"
            @click="selectType(typeOption.value)"
          >
            <div class="type-icon">
              <div :class="typeOption.iconClass"></div>
            </div>
            <div class="type-label">{{ typeOption.label }}</div>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="数据集路径" prop="datasetPath" label-width="90">
        <el-input
            v-model="form.datasetPath"
            placeholder="请输入数据集路径"/>
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
  annotationTypes: {
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
  remark: '',
  type: ''
})

const rules = {
  name: [
    { required: true, message: '请输入标注名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择标注类型', trigger: 'change' }
  ]
}

const resetForm = () => {
  form.value = {
    id: null,
    name: '',
    remark: '',
    type: ''
  }
  formRef.value?.resetFields()
}

const syncForm = () => {
  resetForm()
  form.value = {
    id: props.initialData?.id ?? null,
    name: props.initialData?.name || '',
    remark: props.initialData?.remark || '',
    type: props.initialData?.type || '',
    datasetPath: props.initialData?.datasetPath || ''
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

const selectType = (type) => {
  form.value.type = type
}

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

/* 图标样式 */
.icon-image-classification,
.icon-object-detection,
.icon-instance-segmentation,
.icon-semantic-segmentation {
  width: 32px;
  height: 32px;
  position: relative;
}

/* 图像分类图标 */
.icon-image-classification::before {
  content: '';
  position: absolute;
  width: 24px;
  height: 24px;
  top: 4px;
  left: 4px;
  background: linear-gradient(45deg, #409eff 0%, #409eff 50%, transparent 50%);
  border-radius: 4px;
}

.icon-image-classification::after {
  content: '';
  position: absolute;
  width: 12px;
  height: 12px;
  top: 10px;
  right: 4px;
  background: #67c23a;
  border-radius: 50%;
}

/* 物体检测图标 */
.icon-object-detection::before {
  content: '';
  position: absolute;
  width: 20px;
  height: 20px;
  top: 6px;
  left: 6px;
  border: 3px solid #409eff;
  border-radius: 4px;
}

.icon-object-detection::after {
  content: '';
  position: absolute;
  width: 8px;
  height: 8px;
  top: 12px;
  left: 12px;
  background: #e6a23c;
  border-radius: 50%;
}

/* 实例分割图标 */
.icon-instance-segmentation::before {
  content: '';
  position: absolute;
  width: 24px;
  height: 24px;
  top: 4px;
  left: 4px;
  background: conic-gradient(#409eff 0deg 90deg, #67c23a 90deg 180deg, #e6a23c 180deg 270deg, #f56c6c 270deg 360deg);
  border-radius: 50%;
}

/* 语义分割图标 */
.icon-semantic-segmentation::before {
  content: '';
  position: absolute;
  width: 24px;
  height: 12px;
  top: 4px;
  left: 4px;
  background: linear-gradient(to right, #409eff 33%, #67c23a 33% 66%, #e6a23c 66%);
  border-radius: 2px;
}

.icon-semantic-segmentation::after {
  content: '';
  position: absolute;
  width: 24px;
  height: 12px;
  bottom: 4px;
  left: 4px;
  background: linear-gradient(to right, #f56c6c 50%, #909399 50%);
  border-radius: 2px;
}

.annotation-type-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 8px;
}

.type-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
}

.type-option:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.type-option.active {
  border-color: #409eff;
  background: #e6f7ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.type-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.type-label {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.dialog-footer {
  text-align: right;
}
</style>
