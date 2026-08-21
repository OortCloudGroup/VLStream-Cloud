import axios from 'axios'
import request from '@/utils/wvpRequest'

export const getDeviceFirmwarePage = params => request({
  url: '/vlstream/firmware/page',
  method: 'get',
  params
})

export const issueFirmwareUpload = data => request({
  url: '/vlstream/firmware/upload-grant',
  method: 'post',
  data
})

export const uploadFirmwareToMinio = (uploadUrl, file, contentType, onProgress) => axios.put(
  uploadUrl,
  file,
  {
    headers: { 'Content-Type': contentType },
    timeout: 30 * 60 * 1000,
    onUploadProgress: event => {
      if (event.total && onProgress) {
        onProgress(Math.round((event.loaded * 100) / event.total))
      }
    }
  }
)

export const completeFirmwareUpload = id => request({
  url: `/vlstream/firmware/${id}/complete`,
  method: 'post'
})

export const getFirmwareDownloadUrl = id => request({
  url: `/vlstream/firmware/${id}/download-url`,
  method: 'get'
})

export const removeDeviceFirmware = id => request({
  url: `/vlstream/firmware/${id}`,
  method: 'delete'
})
