import request from '@/utils/request'

// 分页查询算法标注数据
export function getAlgorithmAnnotationPage(params) {
  return request({
    url: '/vlsAlgorithmAnnotation/page',
    method: 'get',
    params
  })
}

// 根据ID查询算法标注
export function getAlgorithmAnnotationById(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}`,
    method: 'get'
  })
}

// 新增算法标注
export function createAlgorithmAnnotation(data) {
  return request({
    url: '/vlsAlgorithmAnnotation',
    method: 'post',
    data
  })
}

// 更新算法标注
export function updateAlgorithmAnnotation(id, data) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}`,
    method: 'put',
    data
  })
}

// 删除算法标注
export function deleteAlgorithmAnnotation(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}`,
    method: 'delete'
  })
}

// 批量删除算法标注
export function batchDeleteAlgorithmAnnotation(ids) {
  return request({
    url: '/vlsAlgorithmAnnotation/batch',
    method: 'delete',
    data: ids
  })
}

// 开始标注任务
export function startAnnotationTask(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/start`,
    method: 'post'
  })
}

// 完成标注任务
export function completeAnnotationTask(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/complete`,
    method: 'post'
  })
}

// 重置标注任务
export function resetAnnotationTask(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/reset`,
    method: 'post'
  })
}

// 更新标注进度
export function updateAnnotationProgress(id, annotatedCount) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/progress`,
    method: 'put',
    data: { annotatedCount }
  })
}

// 导出标注数据
export function exportAnnotationData(id) {
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/export`,
    method: 'post',
    responseType: 'blob'
  })
}

// 导入标注数据
export function importAnnotationData(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/import-zip`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取统计数据
export function getStatistics() {
  return request({
    url: '/vlsAlgorithmAnnotation/statistics',
    method: 'get'
  })
}

// 获取类型统计
export function getTypeStatistics() {
  return request({
    url: '/vlsAlgorithmAnnotation/statistics/type',
    method: 'get'
  })
}

// 获取状态统计
export function getStatusStatistics() {
  return request({
    url: '/vlsAlgorithmAnnotation/statistics/status',
    method: 'get'
  })
}

// 获取进度统计
export function getProgressStatistics() {
  return request({
    url: '/vlsAlgorithmAnnotation/statistics/progress',
    method: 'get'
  })
}

// 获取工作量统计
export function getWorkloadStatistics() {
  return request({
    url: '/vlsAlgorithmAnnotation/statistics/workload',
    method: 'get'
  })
}

// 搜索算法标注
export function searchAlgorithmAnnotation(params) {
  return request({
    url: '/vlsAlgorithmAnnotation/search',
    method: 'get',
    params
  })
}

// 批量操作
export function batchOperation(operation, ids) {
  return request({
    url: '/vlsAlgorithmAnnotation/batch-operation',
    method: 'post',
    data: { operation, ids }
  })
}

// 保存标注数据到服务器
export function saveDataset(id, annotationData) {
  const formData = new FormData()
  formData.append('annotationData', annotationData)
  return request({
    url: `/vlsAlgorithmAnnotation/${id}/save-dataset`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}


// 删除标注实例
export function deleteAnnotationInstance(instanceId) {
  return request({
    url: `/api/annotation-label/instances/${instanceId}`,
    method: 'delete'
  })
}

// 删除图片
export function deleteImage(fileName) {
  return request({
    url: '/image/delete',
    method: 'delete',
    params: { fileName }
  })
}



// 标注类型常量
export const ANNOTATION_TYPES = {
  OBJECT_DETECTION: 'object_detection',
  IMAGE_CLASSIFICATION: 'image_classification',
  INSTANCE_SEGMENTATION: 'instance_segmentation',
  SEMANTIC_SEGMENTATION: 'semantic_segmentation'
}

// 标注状态常量
export const ANNOTATION_STATUS = {
  NONE: 'none',
  PARTIAL: 'partial',
  COMPLETED: 'completed'
}

// 标注类型显示映射
export const ANNOTATION_TYPE_LABELS = {
  [ANNOTATION_TYPES.OBJECT_DETECTION]: '物体检测',
  [ANNOTATION_TYPES.IMAGE_CLASSIFICATION]: '图像分类',
  [ANNOTATION_TYPES.INSTANCE_SEGMENTATION]: '实例分割',
  [ANNOTATION_TYPES.SEMANTIC_SEGMENTATION]: '语义分割'
}

// 标注状态显示映射
export const ANNOTATION_STATUS_LABELS = {
  [ANNOTATION_STATUS.NONE]: '未标注',
  [ANNOTATION_STATUS.PARTIAL]: '标注中',
  [ANNOTATION_STATUS.COMPLETED]: '已完成'
}

// 获取进度百分比
export function getProgressPercentage(annotatedCount, totalCount) {
  if (totalCount === 0) return 0
  return Math.round((annotatedCount / totalCount) * 100)
}

// 获取状态对应的标签类型
export function getStatusTagType(status) {
  switch(status) {
    case ANNOTATION_STATUS.NONE:
      return 'info'
    case ANNOTATION_STATUS.PARTIAL:
      return 'warning'
    case ANNOTATION_STATUS.COMPLETED:
      return 'success'
    default:
      return 'info'
  }
} 