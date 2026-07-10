<template>
  <div class="event-detail">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <span class="breadcrumb-item" @click="goBack">事件列表</span>
      <span class="breadcrumb-separator">></span>
      <span class="breadcrumb-item active">事件详情</span>
    </div>

    <div class="detail-container">
      <!-- 事件详情 -->
      <div class="detail-section">
        <h2 class="section-title">事件详情</h2>
        
        <div class="detail-content">
          <!-- 事件描述 -->
          <div class="detail-row">
            <div class="detail-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="detail-info">
              <span class="detail-label">事件描述</span>
              <div class="detail-value">
                <span>{{ eventInfo.description }}</span>
                <el-tag type="warning" size="small" class="status-tag">待处理</el-tag>
              </div>
            </div>
          </div>

          <!-- 事件ID -->
          <div class="detail-row">
            <div class="detail-icon">
              <span class="icon-text">ID</span>
            </div>
            <div class="detail-info">
              <span class="detail-label">事件ID</span>
              <span class="detail-value">{{ eventInfo.id }}</span>
            </div>
          </div>

          <!-- 上报设备 -->
          <div class="detail-row">
            <div class="detail-icon">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="detail-info">
              <span class="detail-label">上报设备</span>
              <span class="detail-value">{{ eventInfo.device }}</span>
            </div>
          </div>

          <!-- 上报图像 -->
          <div class="detail-row">
            <div class="detail-icon">
              <el-icon><Picture /></el-icon>
            </div>
            <div class="detail-info">
              <span class="detail-label">上报图像</span>
              <div class="detail-images">
                <div class="image-item" v-for="(image, index) in eventInfo.images" :key="index">
                  <img :src="image" :alt="`上报图像${index + 1}`" />
                </div>
              </div>
            </div>
          </div>

          <!-- 上报时间 -->
          <div class="detail-row">
            <div class="detail-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="detail-info">
              <span class="detail-label">上报时间</span>
              <span class="detail-value">{{ eventInfo.reportTime }}</span>
            </div>
          </div>

          <!-- 上报位置 -->
          <div class="detail-row">
            <div class="detail-icon">
              <el-icon><Location /></el-icon>
            </div>
            <div class="detail-info">
              <span class="detail-label">上报位置</span>
              <span class="detail-value">{{ eventInfo.location }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 事件反馈 -->
      <div class="feedback-section">
        <h2 class="section-title">事件反馈</h2>
        
        <div class="feedback-content">
          <!-- 反馈描述 -->
          <div class="feedback-row">
            <div class="detail-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="feedback-info">
              <span class="detail-label">反馈描述</span>
              <el-input
                v-model="feedbackForm.description"
                type="textarea"
                :rows="4"
                placeholder="请输入"
                class="feedback-textarea"
              />
            </div>
          </div>

          <!-- 上传图像 -->
          <div class="feedback-row">
            <div class="detail-icon">
              <el-icon><Picture /></el-icon>
            </div>
            <div class="feedback-info">
              <span class="detail-label">上传图像</span>
              <div class="upload-area">
                <el-upload
                  class="upload-demo"
                  drag
                  :action="uploadAction"
                  multiple
                  :before-upload="beforeUpload"
                  :on-success="handleUploadSuccess"
                  :file-list="feedbackForm.images"
                >
                  <div class="upload-placeholder">
                    <el-icon class="upload-icon"><Plus /></el-icon>
                  </div>
                </el-upload>
              </div>
            </div>
          </div>

          <!-- 事件状态 -->
          <div class="feedback-row">
            <div class="detail-icon">
              <el-icon><Flag /></el-icon>
            </div>
            <div class="feedback-info">
              <span class="detail-label">事件状态</span>
              <div class="status-options">
                <el-radio-group v-model="feedbackForm.status">
                  <el-radio label="pending">未完成</el-radio>
                  <el-radio label="completed">已完成</el-radio>
                </el-radio-group>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button type="primary" size="large" @click="handleSubmit">确定</el-button>
          <el-button size="large" @click="handleCancel">取消</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  Monitor,
  Picture,
  Clock,
  Location,
  Plus,
  Flag
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 事件信息
const eventInfo = ref({
  id: '2019-10-20 13:22',
  description: '装作捅有某样品质，实际上就等于承认了自己并没有这种品质。——叩一回肠一断，三春三月忆三巴',
  device: '3219831',
  images: [
    'https://via.placeholder.com/120x80/4A90E2/ffffff?text=脑部图像',
    'https://via.placeholder.com/120x80/7ED321/ffffff?text=天空图像'
  ],
  reportTime: '2019-10-20 13:22',
  location: '深圳市福田区松岭路56号'
})

// 反馈表单
const feedbackForm = reactive({
  description: '',
  images: [],
  status: 'pending'
})

// 上传配置
const uploadAction = ref('#')

// 方法
const goBack = () => {
  router.push('/event-management')
}

const beforeUpload = (file) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('上传图片只能是 JPG/PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}

const handleUploadSuccess = (response, file) => {
  ElMessage.success('图片上传成功')
  feedbackForm.images.push(file)
}

const handleSubmit = () => {
  if (!feedbackForm.description.trim()) {
    ElMessage.warning('请输入反馈描述')
    return
  }

  ElMessage.success('事件反馈提交成功')
  // 这里可以调用API提交反馈数据
  console.log('提交反馈:', feedbackForm)
  
  // 提交成功后返回列表页
  setTimeout(() => {
    router.push('/event-management')
  }, 1500)
}

const handleCancel = () => {
  router.push('/event-management')
}

onMounted(() => {
  // 从路由参数获取事件ID
  const eventId = route.query.id
  if (eventId) {
    // 这里可以根据eventId获取具体的事件信息
    console.log('事件ID:', eventId)
  }
})
</script>

<style scoped>
.event-detail {
  padding: 16px 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 面包屑导航 */
.breadcrumb {
  margin-bottom: 16px;
  font-size: 14px;
  color: #606266;
}

.breadcrumb-item {
  cursor: pointer;
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

/* 详情容器 */
.detail-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 区域标题 */
.section-title {
  font-size: 18px;
  color: #409eff;
  margin: 0;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fafbfc;
}

/* 详情区域 */
.detail-section {
  border-bottom: 1px solid #f0f0f0;
}

.detail-content {
  padding: 20px 24px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24px;
}

.detail-row:last-child {
  margin-bottom: 0;
}

.detail-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
  margin-top: 4px;
}

.detail-icon .icon-text {
  font-size: 12px;
  font-weight: bold;
  color: #606266;
}

.detail-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-label {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-tag {
  margin-left: 8px;
}

/* 图像展示 */
.detail-images {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.image-item {
  width: 120px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 反馈区域 */
.feedback-section {
  
}

.feedback-content {
  padding: 20px 24px;
}

.feedback-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24px;
}

.feedback-row:last-child {
  margin-bottom: 0;
}

.feedback-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feedback-textarea {
  width: 100%;
  max-width: 500px;
}

/* 上传区域 */
.upload-area {
  width: 120px;
  height: 80px;
}

:deep(.upload-demo) {
  width: 100%;
  height: 100%;
}

:deep(.el-upload) {
  width: 100%;
  height: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
  height: 100%;
  border: 2px dashed #409eff;
  border-radius: 6px;
  background-color: #fafbfc;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.upload-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.upload-icon {
  font-size: 24px;
  color: #409eff;
}

/* 状态选项 */
.status-options {
  display: flex;
  gap: 20px;
}

:deep(.el-radio) {
  margin-right: 0;
}

/* 操作按钮 */
.action-buttons {
  padding: 20px 24px;
  display: flex;
  justify-content: center;
  gap: 16px;
  border-top: 1px solid #f0f0f0;
  background-color: #fafbfc;
}

.action-buttons .el-button {
  min-width: 100px;
  height: 40px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .detail-row,
  .feedback-row {
    flex-direction: column;
  }
  
  .detail-icon {
    margin-bottom: 8px;
  }
  
  .detail-images {
    justify-content: flex-start;
  }
}
</style> 