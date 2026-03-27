import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

// 分页查询算法标注数据
export function getAlgorithmAnnotationPage(params) {
  return commonFunc('/vlsAlgorithmAnnotation/page', params, 'GET')
}

// 根据ID查询算法标注
export function getAlgorithmAnnotationById(id) {
  return commonFunc(`/vlsAlgorithmAnnotation/${id}`, {}, 'GET')
}

// 新增算法标注
export function createAlgorithmAnnotation(data) {
  return commonFunc('/vlsAlgorithmAnnotation', data)
}

// 更新算法标注
export function updateAlgorithmAnnotation(id, data) {
  return commonFunc(`/vlsAlgorithmAnnotation/${id}`, data, 'PUT')
}

// 删除算法标注
export function deleteAlgorithmAnnotation(id) {
  return commonFunc(`/vlsAlgorithmAnnotation/${id}`, {}, 'DELETE')
}

// 批量删除算法标注
export function batchDeleteAlgorithmAnnotation(ids) {
  return commonFunc('/vlsAlgorithmAnnotation/batch', ids, 'DELETE')
}

// 开始标注任务
export function startAnnotationTask(id) {
  return commonFunc(`/vlsAlgorithmAnnotation/${id}/start`, {}, 'POST')
}

// 完成标注任务
export function completeAnnotationTask(id) {
  return commonFunc(`/vlsAlgorithmAnnotation/${id}/complete`, {}, 'POST')
}

// 重置标注任务
export function resetAnnotationTask(id) {
  return commonFunc(`/vlsAlgorithmAnnotation/${id}/reset`, {}, 'POST')
}

// 更新标注进度
export function updateAnnotationProgress(id, annotatedCount) {
  return commonFunc(`/vlsAlgorithmAnnotation/${id}/progress`, { annotatedCount }, 'PUT')
}

// 导出标注数据（特殊配置单独处理，保留responseType）
export function exportAnnotationData(id) {
  return request({
    url: config.URL + config.gateWay + 'vls-server/' + `/vlsAlgorithmAnnotation/${id}/export`,
    method: 'post',
    data: {},
    responseType: 'blob'
  })
}

// 导入标注数据（特殊配置单独处理，保留FormData和multipart头）
export function importAnnotationData(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: config.URL + config.gateWay + 'vls-server/' + `/vlsAlgorithmAnnotation/${id}/import-zip`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取统计数据
export function getStatistics() {
  return commonFunc('/vlsAlgorithmAnnotation/statistics', {}, 'GET')
}

// 获取类型统计
export function getTypeStatistics() {
  return commonFunc('/vlsAlgorithmAnnotation/statistics/type', {}, 'GET')
}

// 获取状态统计
export function getStatusStatistics() {
  return commonFunc('/vlsAlgorithmAnnotation/statistics/status', {}, 'GET')
}

// 获取进度统计
export function getProgressStatistics() {
  return commonFunc('/vlsAlgorithmAnnotation/statistics/progress', {}, 'GET')
}

// 获取工作量统计
export function getWorkloadStatistics() {
  return commonFunc('/vlsAlgorithmAnnotation/statistics/workload', {}, 'GET')
}

// 搜索算法标注
export function searchAlgorithmAnnotation(params) {
  return commonFunc('/vlsAlgorithmAnnotation/search', params, 'GET')
}

// 批量操作
export function batchOperation(operation, ids) {
  return commonFunc('/vlsAlgorithmAnnotation/batch-operation', { operation, ids }, 'POST')
}

// 保存标注数据到服务器（特殊配置单独处理，保留FormData和multipart头）
export function saveDataset(id, annotationData) {
  const formData = new FormData()
  formData.append('annotationData', annotationData)
  return request({
    url: config.URL + config.gateWay + 'vls-server/' + `/vlsAlgorithmAnnotation/${id}/save-dataset`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除标注实例
export function deleteAnnotationInstance(instanceId) {
  return commonFunc(`/api/annotation-label/instances/${instanceId}`, {}, 'DELETE')
}

// 删除图片
export function deleteImage(fileName) {
  return commonFunc('/image/delete', { fileName }, 'DELETE')
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
  switch (status) {
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
