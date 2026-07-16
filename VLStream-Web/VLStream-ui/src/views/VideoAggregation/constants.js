// 设备管理相关常量

// 星期配置
export const weekDays = {
  monday: '星期一',
  tuesday: '星期二', 
  wednesday: '星期三',
  thursday: '星期四',
  friday: '星期五',
  saturday: '星期六',
  sunday: '星期日'
}

// 设备状态
export const deviceStatus = {
  online: 1,
  offline: 0,
  error: 2
}

// 设备状态文本
export const deviceStatusText = {
  [deviceStatus.online]: '在线',
  [deviceStatus.offline]: '离线', 
  [deviceStatus.error]: '故障'
}

// 设备状态颜色
export const deviceStatusType = {
  [deviceStatus.online]: 'success',
  [deviceStatus.offline]: 'danger',
  [deviceStatus.error]: 'warning'
}

