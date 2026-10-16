import { imageUploadRequest, isTrustedApiRequest } from '@/utils/request'

// Element Plus uploads enter the same request-time authentication as other business APIs.
export function uploadTrustedApi(options) {
  const config = {url: options.action, baseURL: '', headers: {...options.headers}}
  if (!isTrustedApiRequest(config)) return Promise.reject(new Error('上传地址不属于受信任业务服务'))
  const form = new FormData()
  for (const [key, value] of Object.entries(options.data || {})) {
    if (Array.isArray(value) && value.length === 2 && value[0] instanceof Blob) form.append(key, value[0], value[1])
    else form.append(key, value)
  }
  form.append(options.filename, options.file, options.file.name)
  return imageUploadRequest.post(options.action, form, {
    ...config,
    onUploadProgress(event) {
      if (event.total) { event.percent = event.loaded / event.total * 100; options.onProgress?.(event) }
    }
  })
}
