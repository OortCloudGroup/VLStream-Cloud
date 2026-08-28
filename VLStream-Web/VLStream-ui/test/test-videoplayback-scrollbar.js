/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

// pagedevice
console.log('🔧 测试视频回放页面设备列表滚动条...')

// etc. pageLoad
setTimeout(() => {
  console.log('📋 开始测试视频回放页面滚动条')

  // whether in page
  const isVideoPlaybackPage = window.location.pathname.includes('VideoPlayback') ||
                             document.querySelector('.video-playback')

  if (!isVideoPlaybackPage) {
    console.log('⚠️ 当前不在视频回放页面，请先导航到视频回放页面')
    console.log('💡 提示：需要访问视频回放页面才能测试设备列表滚动条')
    return
  }

  console.log('✅ 当前在视频回放页面')

  // tableelement
  const tableContent = document.querySelector('.table-content')
  const elTable = document.querySelector('.table-content .el-table')
  const tableBodyWrapper = document.querySelector('.table-content .el-table__body-wrapper')

  console.log('📋 表格元素检查:')
  console.log('- table-content:', tableContent ? '找到' : '未找到')
  console.log('- el-table:', elTable ? '找到' : '未找到')
  console.log('- table-body-wrapper:', tableBodyWrapper ? '找到' : '未找到')

  if (!tableContent || !elTable || !tableBodyWrapper) {
    console.log('❌ 表格元素不完整，无法测试滚动条')
    return
  }

  // tabledata
  const tableRows = document.querySelectorAll('.table-content .el-table__body-wrapper .el-table__row')
  console.log('📋 表格数据检查:')
  console.log('- 表格行数:', tableRows.length)

  if (tableRows.length === 0) {
    console.log('⚠️ 表格中没有数据，请先加载设备数据')
    return
  }

  // table
  const tableContentRect = tableContent.getBoundingClientRect()
  const tableBodyRect = tableBodyWrapper.getBoundingClientRect()

  console.log('📋 表格容器尺寸:')
  console.log('- table-content 宽度:', tableContentRect.width, 'px')
  console.log('- table-content 高度:', tableContentRect.height, 'px')
  console.log('- table-body-wrapper 宽度:', tableBodyRect.width, 'px')
  console.log('- table-body-wrapper 高度:', tableBodyRect.height, 'px')

  //
  const computedStyle = window.getComputedStyle(tableBodyWrapper)
  console.log('📋 滚动条样式检查:')
  console.log('- overflow-x:', computedStyle.overflowX)
  console.log('- overflow-y:', computedStyle.overflowY)
  console.log('- overflow:', computedStyle.overflow)

  // whether
  const hasVerticalScrollbar = tableBodyWrapper.scrollHeight > tableBodyWrapper.clientHeight
  const hasHorizontalScrollbar = tableBodyWrapper.scrollWidth > tableBodyWrapper.clientWidth

  console.log('📋 滚动条状态:')
  console.log('- 垂直滚动条:', hasVerticalScrollbar ? '需要' : '不需要')
  console.log('- 水平滚动条:', hasHorizontalScrollbar ? '需要' : '不需要')

  // Custom
  const scrollbarStyles = [
    '.table-content .el-table__body-wrapper::-webkit-scrollbar',
    '.table-content .el-table__body-wrapper::-webkit-scrollbar-track',
    '.table-content .el-table__body-wrapper::-webkit-scrollbar-thumb',
    '.table-content .el-table__body-wrapper::-webkit-scrollbar-thumb:hover'
  ]

  console.log('📋 自定义滚动条样式检查:')
  scrollbarStyles.forEach(selector => {
    const style = document.querySelector('style')
    if (style && style.textContent.includes(selector.replace('.table-content ', ''))) {
      console.log(`✅ ${selector}: 已定义`)
    } else {
      console.log(`❌ ${selector}: 未定义`)
    }
  })

  // can
  if (hasVerticalScrollbar) {
    console.log('🧪 测试垂直滚动功能...')
    const originalScrollTop = tableBodyWrapper.scrollTop
    tableBodyWrapper.scrollTop = 100

    setTimeout(() => {
      const newScrollTop = tableBodyWrapper.scrollTop
      if (newScrollTop > originalScrollTop) {
        console.log('✅ 垂直滚动功能正常')
      } else {
        console.log('❌ 垂直滚动功能异常')
      }
    }, 100)
  }

  if (hasHorizontalScrollbar) {
    console.log('🧪 测试水平滚动功能...')
    const originalScrollLeft = tableBodyWrapper.scrollLeft
    tableBodyWrapper.scrollLeft = 100

    setTimeout(() => {
      const newScrollLeft = tableBodyWrapper.scrollLeft
      if (newScrollLeft > originalScrollLeft) {
        console.log('✅ 水平滚动功能正常')
      } else {
        console.log('❌ 水平滚动功能异常')
      }
    }, 100)
  }

  // table
  const tableLayout = computedStyle.tableLayout
  console.log('📋 表格布局:')
  console.log('- table-layout:', tableLayout)

  // Set
  const tableColumns = document.querySelectorAll('.table-content .el-table__header-wrapper .el-table__cell')
  console.log('📋 表格列检查:')
  console.log('- 列数:', tableColumns.length)

  tableColumns.forEach((column, index) => {
    const columnStyle = window.getComputedStyle(column)
    console.log(`- 列 ${index + 1}:`)
    console.log(`  - 宽度: ${columnStyle.width}`)
    console.log(`  - min-width: ${columnStyle.minWidth}`)
    console.log(`  - max-width: ${columnStyle.maxWidth}`)
  })

  console.log('✅ 视频回放页面滚动条测试完成')

}, 3000)

console.log('🔧 视频回放页面滚动条测试脚本已加载，3秒后开始检查...')