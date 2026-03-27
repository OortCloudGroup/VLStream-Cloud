/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 11:05:16
* @Last Modified by:   兰舰
* @Last Modified time: 2024-11-15 11:05:16
* @Copyright aPaaS-front-team. All rights reserved.
*/
// 文件大小
export default function FileSizeCalc(data:any) {
  if (!data && data !== 0) return data
  if (data === 0) { return '0B' }
  if (data >= 1024 * 1024 * 1024) {
    data = ((Math.ceil(data / 1024 / 1024 / 1024 * 100)) / 100).toFixed(2) + 'G'
  } else if (data >= 1024 * 1024) {
    data = (Math.ceil(data / 1024 / 1024 * 100) / 100).toFixed(2) + 'M'
  } else if (data >= 1024) {
    data = (Math.ceil(data / 1024 * 100) / 100).toFixed(2) + 'K'
  } else {
    data = (data).toFixed(2) + 'B'
  }
  return data
}
