<template>
  <div class="multi-image-upload">
    <!-- 已上传的图片列表 -->
    <div class="image-list">
      <div
        v-for="(url, index) in modelValue"
        :key="index"
        class="image-item"
        @click="handlePreview(url)"
      >
        <img
          :src="url"
          class="image-thumbnail"
          @error="handleImageError(index)"
        />
        <!-- 右上角删除按钮 -->
        <div class="delete-btn" @click.stop="handleRemove(index)">
          <el-icon><CircleCloseFilled /></el-icon>
        </div>
        <!-- 底部操作按钮组 -->
        <div class="action-buttons">
          <div class="action-btn preview-btn" @click.stop="handlePreview(url)">
            <el-icon><View /></el-icon>
          </div>
          <div class="action-btn download-btn" @click.stop="handleDownload(url)">
            <el-icon><Download /></el-icon>
          </div>
        </div>
      </div>
    </div>
    <!-- 上传按钮 -->
    <el-upload
      v-if="modelValue.length < maxCount"
      :action="uploadURL"
      :headers="headers"
      :accept="accept"
      :show-file-list="false"
      :multiple="false"
      :on-success="handleSuccess"
      :before-upload="beforeUpload"
    >
      <div class="upload-trigger">
        <el-icon class="upload-icon">
          <Plus />
        </el-icon>
        <div class="upload-text">
          {{ tip }}
        </div>
      </div>
    </el-upload>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="50%">
      <img v-if="previewImage" :src="previewImage" style="width: 100%;" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, CircleCloseFilled, Download, View } from '@element-plus/icons-vue'
import Config from '@/config'

const props = defineProps({
  modelValue: {
    type: Array as () => string[],
    default: () => []
  },
  accept: {
    type: String,
    default: 'image/*'
  },
  sizeLimit: {
    type: Number,
    default: 2
  },
  tip: {
    type: String,
    default: '上传图片'
  },
  maxCount: {
    type: Number,
    default: 10
  }
})

const emit = defineEmits(['update:modelValue'])

const uploadURL = ref(Config.URL + Config.gateWay + 'apaas-fastdfsservice/fastdfs/v1/uploadFile')
const headers = ref(Config.headers)

const previewVisible = ref(false)
const previewImage = ref('')

// 上传成功处理
const handleSuccess = (res: any, _file: any) => {
  if (res.code === 200) {
    const newUrl = res.data.url
    const currentUrls = [...props.modelValue]

    // 检查是否超过最大数量限制
    if (currentUrls.length >= props.maxCount) {
      ElMessage.warning(`最多只能上传${props.maxCount}张图片`)
      return
    }

    // 添加新图片URL到数组
    currentUrls.push(newUrl)
    emit('update:modelValue', currentUrls)

    ElMessage.success('上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

// 文件大小检查
const beforeUpload = (file: File) => {
  const isLimit = file.size / 1024 / 1024 < props.sizeLimit
  if (!isLimit) {
    ElMessage.error(`图片大小不能超过${props.sizeLimit}MB!`)
  }
  return isLimit
}

// 删除图片
const handleRemove = (index: number) => {
  const currentUrls = [...props.modelValue]
  currentUrls.splice(index, 1)
  emit('update:modelValue', currentUrls)
  ElMessage.success('删除成功')
}

// 下载图片
const handleDownload = (url: string) => {
  const link = document.createElement('a')
  link.href = url
  link.download = `image_${Date.now()}.jpg`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  ElMessage.success('下载成功')
}

// 预览图片
const handlePreview = (url: string) => {
  previewImage.value = url
  previewVisible.value = true
}

// 图片加载错误处理
const handleImageError = (index: number) => {
  ElMessage.error(`图片加载失败: ${props.modelValue[index]}`)
}
</script>

<style scoped lang="scss">
.multi-image-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .image-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .image-item {
    position: relative;
    width: 100px;
    height: 100px;
    border: 1px solid #d9d9d9;
    border-radius: 6px;
    overflow: hidden;
    cursor: pointer;
    transition: border-color 0.3s;

    &:hover {
      border-color: var(--el-color-primary);
    }

    .image-thumbnail {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .delete-btn {
      position: absolute;
      top: 4px;
      right: 4px;
      width: 20px;
      height: 20px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: rgb(212, 209, 209);
      font-size: 24px;
      cursor: pointer;
      transition: all 0.3s;
      opacity: 0;
      visibility: hidden;

      &:hover {
        background: rgba(0, 0, 0, 0.8);
      }
    }

    .action-buttons {
      position: absolute;
      bottom: 4px;
      left: 50%;
      top: 50%;
      transform: translate(-50%,-50%);
      display: flex;
      gap: 4px;
      align-items: center;
      opacity: 0;
      visibility: hidden;
      transition: all 0.3s;
    }

    .action-btn {
      width: 20px;
      height: 20px;
    //   background: rgba(0, 0, 0, 0.6);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: rgb(212, 209, 209);
      font-size: 24px;
      font-weight: 700;
      cursor: pointer;
      transition: background-color 0.3s;

      &:hover {
        color: rgb(65, 65, 65);
      }
    }

    &:hover {
      .delete-btn,
      .action-buttons {
        opacity: 1;
        visibility: visible;
      }
    }
  }

  .upload-trigger {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100px;
    height: 100px;
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    transition: border-color 0.3s;

    &:hover {
      border-color: var(--el-color-primary);
    }
  }

  .upload-icon {
    font-size: 28px;
    color: #8c939d;
    margin-bottom: 8px;
  }

  .upload-text {
    font-size: 12px;
    color: #8c939d;
    text-align: center;
  }

  :deep(.el-upload) {
    display: inline-block;
  }
  .download-btn{
    margin-left: 15px;
  }
}
</style>
