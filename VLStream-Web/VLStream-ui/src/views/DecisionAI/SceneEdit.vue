<!--
  SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
  SPDX-License-Identifier: MIT
-->

<template>
  <div class="scene-edit">
    <div class="content-header">
      <div class="breadcrumb">
        <span class="breadcrumb-item" @click="goBack">场景列表</span>
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-item active">编辑场景</span>
      </div>
    </div>

    <div class="edit-container">
      <el-form :model="sceneForm" :rules="sceneRules" ref="sceneFormRef" label-width="120px">
        <!--  -->
        <el-form-item label="场景名称" prop="sceneName">
          <el-input
            v-model="sceneForm.sceneName"
            placeholder="请输入场景名称"
            style="width: 400px;"
          />
        </el-form-item>

        <!--  -->
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

        <!-- snapshot -->
        <el-form-item label="抓拍截图上传" prop="screenshots">
          <div class="upload-section">
            <div class="upload-info">
              <span class="upload-count">({{ uploadedFiles.length }}/5)</span>
            </div>
            <div class="upload-container">
              <!-- already -->
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

              <!--  -->
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

        <!--  -->
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

        <!--  -->
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

        <!--  -->
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

        <!-- operationbutton -->
        <el-form-item>
          <div class="form-actions">
            <el-button type="primary" @click="handleSubmit" :loading="saving" class="common_btn">申请</el-button>
            <el-button @click="handleCancel" class="common_btn">取消</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!--  -->
    <el-dialog
      v-model="showRegionDialog"
      title="选择区域"
      width="30%"
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
          <el-button @click="showRegionDialog = false" class="common_btn">取消</el-button>
          <el-button type="primary" @click="confirmRegionSelection" class="common_btn">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!--  -->
    <el-dialog
      v-model="showCameraDialog"
      title="选择摄像头"
      width="30%"
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
          <el-button @click="showCameraDialog = false" class="common_btn">取消</el-button>
          <el-button type="primary" @click="confirmCameraSelection" class="common_btn">确定</el-button>
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

//
const saving = ref(false)

//
const showRegionDialog = ref(false)
const showCameraDialog = ref(false)

//
const fileInput = ref(null)
const uploadedFiles = ref([])

//
const tempSelectedRegions = ref([])
const tempSelectedCameras = ref([])

// formdata
const sceneForm = reactive({
  sceneName: '',
  analysisType: '',
  screenshots: [],
  selectedRegions: [],
  selectedCameras: [],
  timeRange: []
})

//
const availableRegions = ref([])

//
const availableCameras = ref([])

// form
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

// form
const sceneFormRef = ref(null)

// method
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

// Initialize
onMounted(() => {
  tempSelectedRegions.value = [...sceneForm.selectedRegions]
  tempSelectedCameras.value = [...sceneForm.selectedCameras]
})
</script>

<style scoped>
.scene-edit {
  height: 100%;
  margin: 0;
  padding: 0;
  background: #fff;
  border-radius: var(--common-border-radius) var(--common-border-radius) 0 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.content-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  flex-shrink: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #606266;
}

.breadcrumb-item {
  color: var(--el-color-primary);
  cursor: pointer;
  transition: color 0.3s;
}

.breadcrumb-item:hover {
  color: #3d70ff;
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
  flex: 1;
  min-height: 0;
  background: #fff;
  padding: 20px;
  overflow-y: auto;
}

/*  */
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

/* and */
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

/*  */
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

/* operationbutton */
.form-actions {
  display: flex;
  gap: 16px;
  padding-top: 20px;
}

/*  */
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

/* form */
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

/*  */
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
