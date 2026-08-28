/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// onMountedwhether
console.log('🔧 简单检查onMounted调用...')

// console.log log
const originalLog = console.log
const capturedLogs = []

console.log = function(...args) {
  const message = args.join(' ')
  capturedLogs.push(message)
  originalLog.apply(console, args)

  // whether onMountedrelated log
  if (message.includes('🎬 组件开始挂载')) {
    console.log('✅ 发现onMounted调用日志')
  }
  if (message.includes('🔄 开始调用loadTenantInfo')) {
    console.log('✅ 发现loadTenantInfo调用日志')
  }
  if (message.includes('✅ loadTenantInfo调用完成')) {
    console.log('✅ 发现loadTenantInfo完成日志')
  }
}

//
setTimeout(() => {
  console.log('📋 检查捕获的日志...')

  const onMountedLogs = capturedLogs.filter(log =>
    log.includes('🎬 组件开始挂载') ||
    log.includes('🔄 开始调用loadTenantInfo') ||
    log.includes('✅ loadTenantInfo调用完成')
  )

  console.log('📋 找到的onMounted相关日志:')
  onMountedLogs.forEach(log => console.log('-', log))

  if (onMountedLogs.length === 0) {
    console.log('❌ 没有找到任何onMounted相关日志')
    console.log('🔍 可能的原因:')
    console.log('1. 组件没有正确挂载')
    console.log('2. 有JavaScript错误阻止了执行')
    console.log('3. 页面还没有完全加载')
  } else {
    console.log('✅ 找到了onMounted相关日志')
  }

  // console.log
  console.log = originalLog
}, 3000)

console.log('🔧 简单检查工具已加载，3秒后检查日志...')