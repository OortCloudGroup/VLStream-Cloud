import { request } from '@/utils/service'
import config from '@/config'

function commonFunc<T = any, K = any>(interfaceName: string, data: T, method = 'post') {
  return request<K>({
    url: config.URL + config.gateWay + 'vls-server/' + interfaceName,
    method: method,
    data: data
  })
}

/**
 * 获取标注项目的图片列表
 * @param {number} annotationId - 标注项目ID
 * @returns {Promise}
 */
export function getAnnotationImages(annotationId) {
  return commonFunc(`/vlsAnnotationImage/dataset/${annotationId}`, {}, 'GET')
}

/**
 * 上传图片文件并保存信息到annotation_image表
 * @param {Array} files - 图片文件列表
 * @param {number} annotationId - 标注项目ID
 * @param {string} category - 图片分类
 * @returns {Promise}
 */
export function uploadAnnotationImages(files, annotationId) {
  const formData = new FormData()

  // 添加所有文件（后端 @RequestPart("files") MultipartFile[]）
  const fileList = Array.isArray(files) ? files : [files]
  fileList.forEach(file => {
    formData.append('files', file)
  })

  // 添加标注项目ID（后端 @RequestParam("annotationId")）
  formData.append('annotationId', annotationId)

  // 含FormData和multipart头，单独处理并拼接完整URL
  return request({
    url: config.URL + config.gateWay + 'vls-server/' + '/vlsAnnotationImage/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 保存单个图片信息到annotation_image表
 * @param {Object} imageData - 图片信息
 * @returns {Promise}
 */
export function saveAnnotationImage(imageData) {
  return commonFunc('/vlsAnnotationImage/images', imageData)
}

/**
 * 批量保存图片信息到annotation_image表
 * @param {Array} imagesData - 图片信息列表
 * @returns {Promise}
 */
export function batchSaveAnnotationImages(imagesData) {
  return commonFunc('/vlsAnnotationImage/images/batch', imagesData)
}
