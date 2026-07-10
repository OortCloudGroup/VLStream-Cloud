<template>
  <el-dialog
    v-model="visible"
    title="设备列表 > 编辑设备"
    width="800px"
    :close-on-click-modal="false"
    class="device-edit-dialog"
    @close="handleClose"
  >
    <div class="edit-content">
      <!-- 基本信息 -->
      <div class="section">
        <h3 class="section-title">基本信息</h3>
        
        <el-form
          :model="deviceForm"
          :rules="deviceRules"
          ref="deviceFormRef"
          label-width="100px"
          class="edit-form"
        >
          <el-form-item label="视频流路径" prop="streamPath" class="required-field">
            <el-input
              v-model="deviceForm.streamPath"
              placeholder="rtsp://192.168.88.58:524/stream/aaa"
            />
          </el-form-item>
          
          <el-form-item label="设备名称" prop="name">
            <el-input
              v-model="deviceForm.name"
              placeholder="自动生成名称"
            />
          </el-form-item>
          
          <el-form-item label="设备标签" prop="tags">
            <div class="tags-container">
              <el-tag
                v-for="tag in deviceForm.tags"
                :key="tag"
                closable
                @close="removeTag(tag)"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
              <el-button class="add-tag-btn" size="small" @click="showTagInput">
                + 添加标签
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 更多信息 -->
      <div class="section">
        <h3 class="section-title">更多信息</h3>
        
        <el-form
          :model="deviceForm"
          label-width="100px"
          class="edit-form"
        >
          <el-form-item label="设备ID">
            <el-input
              v-model="deviceForm.deviceId"
              placeholder="输入设备ID"
            />
          </el-form-item>
          
          <el-form-item label="类型">
            <el-input
              v-model="deviceForm.type"
              placeholder="输入类型：枪机、球机"
            />
          </el-form-item>
          
          <el-form-item label="图片路径">
            <el-input
              v-model="deviceForm.imagePath"
              placeholder="输入图片路径"
            />
          </el-form-item>
          
          <el-form-item label="经纬度坐标">
            <div class="coordinate-input">
              <el-input
                v-model="deviceForm.longitude"
                placeholder="输入经度"
                class="coordinate-item"
              />
              <el-input
                v-model="deviceForm.latitude"
                placeholder="输入纬度"
                class="coordinate-item"
              />
              <el-button type="primary" class="locate-btn">地图选点</el-button>
            </div>
          </el-form-item>
          
          <el-form-item label="高度位置">
            <div class="height-options">
              <el-radio-group v-model="deviceForm.heightPosition">
                <el-radio label="高空">高空</el-radio>
                <el-radio label="地面">地面</el-radio>
                <el-radio label="地下">地下</el-radio>
                <el-radio label="其他">其他</el-radio>
              </el-radio-group>
            </div>
          </el-form-item>
          
          <el-form-item label="详细地址">
            <el-input
              v-model="deviceForm.address"
              placeholder="输入详细地址：省/市/区（县）/街道（村）"
            />
          </el-form-item>
          
          <el-form-item label="区划选择">
            <el-select
              v-model="deviceForm.region"
              placeholder="选择区划"
              style="width: 100%"
            >
              <el-option label="选择区划" value="" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="备注">
            <el-input
              v-model="deviceForm.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注信息"
            />
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleSave" class="save-btn">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  deviceData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'save'])

const visible = ref(false)
const deviceFormRef = ref()

const deviceForm = ref({
  streamPath: '',
  name: '',
  tags: ['标签一', '标签二'],
  deviceId: '',
  type: '',
  imagePath: '',
  longitude: '',
  latitude: '',
  heightPosition: '地面',
  address: '',
  region: '',
  remark: ''
})

const deviceRules = {
  streamPath: [{ required: true, message: '请输入视频流路径', trigger: 'blur' }]
}

// 监听props变化
watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.deviceData) {
    // 填充表单数据
    deviceForm.value = {
      streamPath: props.deviceData.streamPath || '',
      name: props.deviceData.name || '',
      tags: props.deviceData.tags || ['标签一', '标签二'],
      deviceId: props.deviceData.deviceId || '',
      type: props.deviceData.type || '',
      imagePath: props.deviceData.imagePath || '',
      longitude: props.deviceData.longitude || '',
      latitude: props.deviceData.latitude || '',
      heightPosition: props.deviceData.heightPosition || '地面',
      address: props.deviceData.address || '',
      region: props.deviceData.region || '',
      remark: props.deviceData.remark || ''
    }
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const removeTag = (tag) => {
  const index = deviceForm.value.tags.indexOf(tag)
  if (index > -1) {
    deviceForm.value.tags.splice(index, 1)
  }
}

const showTagInput = () => {
  ElMessage.info('添加标签功能')
}

const handleClose = () => {
  visible.value = false
}

const handleSave = async () => {
  if (!deviceFormRef.value) return
  
  try {
    await deviceFormRef.value.validate()
    emit('save', deviceForm.value)
    visible.value = false
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}
</script>

<style scoped>
.device-edit-dialog {
  border-radius: 8px;
}

.device-edit-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.device-edit-dialog :deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.device-edit-dialog :deep(.el-dialog__body) {
  padding: 0;
  max-height: 70vh;
  overflow-y: auto;
}

.edit-content {
  padding: 24px;
}

.section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
  margin: 0 0 20px 0;
  padding-bottom: 8px;
  border-bottom: 2px solid #409eff;
}

.edit-form {
  margin: 0;
}

.edit-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.edit-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.required-field :deep(.el-form-item__label)::before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tag-item {
  background: #e1f3ff;
  border: 1px solid #409eff;
  color: #409eff;
}

.add-tag-btn {
  border: 1px dashed #d9d9d9;
  background: transparent;
  color: #666;
}

.add-tag-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.coordinate-input {
  display: flex;
  gap: 12px;
  align-items: center;
}

.coordinate-item {
  flex: 1;
}

.locate-btn {
  flex-shrink: 0;
}

.height-options :deep(.el-radio-group) {
  display: flex;
  gap: 20px;
}

.height-options :deep(.el-radio) {
  margin-right: 0;
}

.height-options :deep(.el-radio__label) {
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.save-btn {
  min-width: 100px;
  height: 40px;
  font-size: 16px;
  background: linear-gradient(135deg, #409eff 0%, #3a8ee6 100%);
  border: none;
}

.save-btn:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%);
}
</style> 