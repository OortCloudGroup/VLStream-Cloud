/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// device related

// configuration
export const weekDays = {
  monday: '星期一',
  tuesday: '星期二',
  wednesday: '星期三',
  thursday: '星期四',
  friday: '星期五',
  saturday: '星期六',
  sunday: '星期日'
}

// device
export const deviceStatus = {
  online: 1,
  offline: 0,
  error: 2
}

// device
export const deviceStatusText = {
  [deviceStatus.online]: '在线',
  [deviceStatus.offline]: '离线',
  [deviceStatus.error]: '故障'
}

// device
export const deviceStatusType = {
  [deviceStatus.online]: 'success',
  [deviceStatus.offline]: 'danger',
  [deviceStatus.error]: 'warning'
}

