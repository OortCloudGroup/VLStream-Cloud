<template>
  <div class="scene-edit">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="goBack">场景列表</span>
      <span class="breadcrumb-separator">></span>
      <span class="breadcrumb-item active">编辑场景</span>
    </div>

    <div class="edit-container">
      <el-form :model="sceneForm" :rules="sceneRules" ref="sceneFormRef" label-width="120px">
        <!-- 场景名称 -->
        <el-form-item label="场景名称" prop="sceneName">
          <el-input 
            v-model="sceneForm.sceneName" 
            placeholder="请输入场景名称" 
            style="width: 400px;"
          />
        </el-form-item>

        <!-- 分析类型 -->
        <el-form-item label="分析类型" prop="analysisType">
          <el-select 
            v-model="sceneForm.analysisType" 
            placeholder="请选择分析类型"
            style="width: 400px;"
          >
            <template #suffix>
              <el-icon><ArrowRight /></el-icon>
            </template>
            <el-option label="人员" value="person" />
            <el-option label="车辆" value="vehicle" />
            <el-option label="物体" value="object" />
            <el-option label="行为" value="behavior" />
          </el-select>
        </el-form-item>

        <!-- 抓拍截图上传 -->
        <el-form-item label="抓拍截图上传" prop="screenshots">
          <div class="upload-section">
            <div class="upload-info">
              <span class="upload-count">({{ uploadedFiles.length }}/5)</span>
            </div>
            <div class="upload-container">
              <!-- 已上传的图片 -->
              <div 
                v-for="(file, index) in uploadedFiles" 
                :key="index" 
                class="upload-item uploaded"
              >
                <img :src="file.url" :alt="file.name" class="uploaded-image" />
                <div class="upload-overlay">
                  <el-icon class="delete-icon" @click="removeFile(index)">
                    <Close />
                  </el-icon>
                </div>
              </div>
              
              <!-- 上传区域 -->
              <div 
                v-if="uploadedFiles.length < 5"
                class="upload-item upload-area"
                @click="triggerUpload"
              >
                <el-icon class="upload-icon">
                  <Plus />
                </el-icon>
                <input 
                  ref="fileInput"
                  type="file" 
                  accept="image/*" 
                  multiple 
                  style="display: none;"
                  @change="handleFileUpload"
                />
              </div>
            </div>
          </div>
        </el-form-item>

        <!-- 选择区域 -->
        <el-form-item label="选择区域" prop="selectedRegions">
          <div class="region-section">
            <div class="region-tags">
              <el-tag
                v-for="region in sceneForm.selectedRegions"
                :key="region"
                closable
                type="primary"
                class="region-tag"
                @close="removeRegion(region)"
              >
                {{ region }}
              </el-tag>
            </div>
            <el-button 
              class="add-region-btn"
              @click="showRegionDialog = true"
            >
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </el-form-item>

        <!-- 选择摄像头 -->
        <el-form-item label="选择摄像头" prop="selectedCameras">
          <div class="camera-section">
            <div class="camera-tags">
              <el-tag
                v-for="camera in sceneForm.selectedCameras"
                :key="camera"
                closable
                type="primary"
                class="camera-tag"
                @close="removeCamera(camera)"
              >
                {{ camera }}
              </el-tag>
            </div>
            <el-button 
              class="add-camera-btn"
              @click="showCameraDialog = true"
            >
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </el-form-item>

        <!-- 选择时间段 -->
        <el-form-item label="选择时间段" prop="timeRange">
          <div class="time-range-section">
            <DateRangePicker
              v-model="sceneForm.timeRange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="MM/DD HH:mm"
              value-format="MM/DD HH:mm"
              width="400px"
            />
            <el-button type="text" class="time-settings-btn">
              <el-icon><Setting /></el-icon>
            </el-button>
          </div>
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item>
          <div class="form-actions">
            <el-button type="primary" @click="handleSubmit" :loading="saving">申请</el-button>
            <el-button @click="handleCancel">取消</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 选择区域对话框 -->
    <el-dialog
      v-model="showRegionDialog"
      title="选择区域"
      width="500px"
    >
      <div class="dialog-content">
        <el-checkbox-group v-model="tempSelectedRegions">
          <div class="checkbox-grid">
            <el-checkbox 
              v-for="region in availableRegions"
              :key="region.value"
              :label="region.value"
              class="region-checkbox"
            >
              {{ region.label }}
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showRegionDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmRegionSelection">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 选择摄像头对话框 -->
    <el-dialog
      v-model="showCameraDialog"
      title="选择摄像头"
      width="500px"
    >
      <div class="dialog-content">
        <el-checkbox-group v-model="tempSelectedCameras">
          <div class="checkbox-grid">
            <el-checkbox 
              v-for="camera in availableCameras"
              :key="camera.value"
              :label="camera.value"
              class="camera-checkbox"
            >
              {{ camera.label }}
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCameraDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmCameraSelection">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Plus, Close, Setting } from '@element-plus/icons-vue'
import DateRangePicker from '@/components/DateRangePicker.vue'

const route = useRoute()
const router = useRouter()

// 保存状态
const saving = ref(false)

// 对话框显示状态
const showRegionDialog = ref(false)
const showCameraDialog = ref(false)

// 占位图片数据URL
const placeholderImage120x90 = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwIiBoZWlnaHQ9IjkwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9IiNmNWY1ZjUiLz48cGF0aCBkPSJNNDAuNSA0NWMwIDguNDE4IDYuMDgyIDE1LjUgMTMuNSAxNS41czEzLjUtNy4wODIgMTMuNS0xNS41UzYyLjQxOCAyOS41IDU0IDI5LjUgNDAuNSAzNi41ODIgNDAuNSA0NXptNDAgNDBMNzAgNzVsLTEwLTEwLTIwIDIweiIgZmlsbD0iI2RkZCIvPjx0ZXh0IHg9IjYwIiB5PSI4MCIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjEyIiBmaWxsPSIjOTk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj4xMjAgeCBwMDwvdGV4dD48L3N2Zz4='

// 文件上传
const fileInput = ref(null)
const uploadedFiles = ref([
  {
    name: 'screenshot1.jpg',
    url: placeholderImage120x90
  },
  {
    name: 'screenshot2.jpg', 
    url: placeholderImage120x90
  }
])

// 临时选择状态
const tempSelectedRegions = ref([])
const tempSelectedCameras = ref([])

// 场景表单数据
const sceneForm = reactive({
  sceneName: '告警场景',
  analysisType: '人员',
  screenshots: [],
  selectedRegions: ['水产大楼', '洪湖大厦'],
  selectedCameras: ['摄像头1', '摄像头2'],
  timeRange: ['08/17 10:20', '08/17 12:20']
})

// 可选区域列表
const availableRegions = ref([
  { label: '水产大楼', value: '水产大楼' },
  { label: '洪湖大厦', value: '洪湖大厦' },
  { label: '教学楼A', value: '教学楼A' },
  { label: '教学楼B', value: '教学楼B' },
  { label: '图书馆', value: '图书馆' },
  { label: '体育馆', value: '体育馆' }
])

// 可选摄像头列表
const availableCameras = ref([
  { label: '摄像头1', value: '摄像头1' },
  { label: '摄像头2', value: '摄像头2' },
  { label: '摄像头3', value: '摄像头3' },
  { label: '摄像头4', value: '摄像头4' },
  { label: '摄像头5', value: '摄像头5' },
  { label: '摄像头6', value: '摄像头6' }
])

// 表单验证规则
const sceneRules = {
  sceneName: [
    { required: true, message: '请输入场景名称', trigger: 'blur' }
  ],
  analysisType: [
    { required: true, message: '请选择分析类型', trigger: 'change' }
  ],
  selectedRegions: [
    { required: true, message: '请选择区域', trigger: 'change' }
  ],
  selectedCameras: [
    { required: true, message: '请选择摄像头', trigger: 'change' }
  ],
  timeRange: [
    { required: true, message: '请选择时间段', trigger: 'change' }
  ]
}

// 表单引用
const sceneFormRef = ref(null)

// 方法
const goBack = () => {
  router.push('/intelligent-analysis-request')
}

const triggerUpload = () => {
  fileInput.value?.click()
}

const handleFileUpload = (event) => {
  const files = Array.from(event.target.files)
  files.forEach(file => {
    if (uploadedFiles.value.length < 5) {
      const reader = new FileReader()
      reader.onload = (e) => {
        uploadedFiles.value.push({
          name: file.name,
          url: e.target.result
        })
      }
      reader.readAsDataURL(file)
    }
  })
  event.target.value = ''
}

const removeFile = (index) => {
  uploadedFiles.value.splice(index, 1)
}

const removeRegion = (region) => {
  const index = sceneForm.selectedRegions.indexOf(region)
  if (index > -1) {
    sceneForm.selectedRegions.splice(index, 1)
  }
}

const removeCamera = (camera) => {
  const index = sceneForm.selectedCameras.indexOf(camera)
  if (index > -1) {
    sceneForm.selectedCameras.splice(index, 1)
  }
}

const confirmRegionSelection = () => {
  sceneForm.selectedRegions = [...tempSelectedRegions.value]
  showRegionDialog.value = false
}

const confirmCameraSelection = () => {
  sceneForm.selectedCameras = [...tempSelectedCameras.value]
  showCameraDialog.value = false
}

const handleSubmit = async () => {
  try {
    if (sceneFormRef.value) {
      await sceneFormRef.value.validate()
    }
    
    saving.value = true
    
    setTimeout(() => {
      saving.value = false
      ElMessage.success('申请提交成功')
      goBack()
    }, 1000)
    
  } catch (error) {
    ElMessage.error('请完善表单信息')
  }
}

const handleCancel = () => {
  goBack()
}

// 初始化临时选择状态
onMounted(() => {
  tempSelectedRegions.value = [...sceneForm.selectedRegions]
  tempSelectedCameras.value = [...sceneForm.selectedCameras]
})
</script>

<style scoped>
.scene-edit {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.breadcrumb {
  margin-bottom: 20px;
  font-size: 14px;
}

.breadcrumb-item {
  color: #606266;
  cursor: pointer;
}

.breadcrumb-item:hover {
  color: #409eff;
}

.breadcrumb-item.active {
  color: #303133;
  cursor: default;
}

.breadcrumb-separator {
  margin: 0 8px;
  color: #c0c4cc;
}

.edit-container {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  max-width: 800px;
}

/* 上传区域样式 */
.upload-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-info {
  color: #606266;
  font-size: 14px;
}

.upload-container {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-item {
  width: 120px;
  height: 90px;
  border-radius: 6px;
  position: relative;
  overflow: hidden;
}

.upload-item.uploaded {
  border: 1px solid #e4e7ed;
}

.uploaded-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.upload-item.uploaded:hover .upload-overlay {
  opacity: 1;
}

.delete-icon {
  color: white;
  font-size: 20px;
  cursor: pointer;
}

.upload-area {
  border: 2px dashed #c0c4cc;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.3s;
}

.upload-area:hover {
  border-color: #409eff;
}

.upload-icon {
  font-size: 24px;
  color: #409eff;
}

/* 区域和摄像头选择样式 */
.region-section,
.camera-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.region-tags,
.camera-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.region-tag,
.camera-tag {
  margin: 0;
}

.add-region-btn,
.add-camera-btn {
  padding: 4px 8px;
  border: 1px solid #409eff;
  color: #409eff;
  background: white;
}

.add-region-btn:hover,
.add-camera-btn:hover {
  background: #ecf5ff;
}

/* 时间选择样式 */
.time-range-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-settings-btn {
  padding: 8px;
  color: #606266;
}

.time-settings-btn:hover {
  color: #409eff;
}

/* 操作按钮样式 */
.form-actions {
  display: flex;
  gap: 16px;
  padding-top: 20px;
}

.form-actions .el-button {
  min-width: 100px;
  height: 40px;
}

/* 对话框样式 */
.dialog-content {
  padding: 20px 0;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.region-checkbox,
.camera-checkbox {
  margin: 0;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
}

/* 表单样式调整 */
:deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-date-editor) {
  border-radius: 6px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .edit-container {
    padding: 20px;
  }
  
  .upload-container {
    justify-content: center;
  }
  
  .checkbox-grid {
    grid-template-columns: 1fr;
  }
}
</style> 