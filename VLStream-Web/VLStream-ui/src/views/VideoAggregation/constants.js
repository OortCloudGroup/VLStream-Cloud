// 设备管理相关常量

// 占位图片数据URL
export const placeholderImage200x150 = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjE1MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjVmNWY1Ii8+PHBhdGggZD0iTTYwLjUgNjBjMCAxMi40MTggMTAuMDgyIDIyLjUgMjIuNSAyMi41czIyLjUtMTAuMDgyIDIyLjUtMjIuNVM5NS40MTggMzcuNSA4MyAzNy41IDYwLjUgNDcuNTgyIDYwLjUgNjB6bTU4IDYwTDEwMCAxMDBsLTIwLTIwLTMwIDMweiIgZmlsbD0iI2RkZCIvPjx0ZXh0IHg9IjEwMCIgeT0iMTMwIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiPjIwMCB4IDE1MDwvdGV4dD48L3N2Zz4='

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

// 检测项目列表
export const defaultDetectionItems = [
  { name: '音频异常侦测' },
  { name: '区域入侵侦测' },
  { name: '越界侦测' },
  { name: '离开区域侦测' },
  { name: '物品遗留侦测' },
  { name: '物品拿取侦测' }
]

// 模型超市数据
export const modelListData = [
  { id: 1, name: '人流密度', category: 'person', type: '人员检查类', image: placeholderImage200x150, selected: false },
  { id: 2, name: '客流量统计', category: 'video', type: '视频分析类', image: placeholderImage200x150, selected: false },
  { id: 3, name: '人流密度', category: 'person', type: '人员检查类', image: placeholderImage200x150, selected: false },
  { id: 4, name: '客流量统计', category: 'video', type: '视频分析类', image: placeholderImage200x150, selected: false },
  { id: 5, name: '人流密度', category: 'person', type: '人员检查类', image: placeholderImage200x150, selected: true },
  { id: 6, name: '客流量统计', category: 'video', type: '视频分析类', image: placeholderImage200x150, selected: false },
  { id: 7, name: '人流密度', category: 'person', type: '人员检查类', image: placeholderImage200x150, selected: false },
  { id: 8, name: '客流量统计', category: 'video', type: '视频分析类', image: placeholderImage200x150, selected: false },
  { id: 9, name: '人流密度', category: 'person', type: '人员检查类', image: placeholderImage200x150, selected: false },
  { id: 10, name: '客流量统计', category: 'video', type: '视频分析类', image: placeholderImage200x150, selected: false },
  { id: 11, name: '人流密度', category: 'person', type: '人员检查类', image: placeholderImage200x150, selected: false },
  { id: 12, name: '客流量统计', category: 'video', type: '视频分析类', image: placeholderImage200x150, selected: false },
  { id: 13, name: '人脸识别', category: 'behavior', type: '人脸识别类', image: placeholderImage200x150, selected: false },
  { id: 14, name: '行为分析', category: 'video', type: '视频分析类', image: placeholderImage200x150, selected: false },
  { id: 15, name: '异常检测', category: 'person', type: '人员检查类', image: placeholderImage200x150, selected: false },
  { id: 16, name: '车辆识别', category: 'video', type: '视频分析类', image: placeholderImage200x150, selected: false }
]

// 默认设备树数据
export const defaultDeviceTreeData = [
  {
    label: '球机监控',
    type: 'tag',
    expanded: true,
    children: []
  },
  {
    label: '云台',
    type: 'tag',
    expanded: true,
    children: []
  },
  {
    label: '摄像头',
    type: 'tag',
    expanded: true,
    children: []
  },
  {
    label: '枪机',
    type: 'tag',
    expanded: false,
    children: []
  },
  {
    label: '半球',
    type: 'tag',
    expanded: false,
    children: []
  }
]

// 测试视频URL - 用于调试和演示
export const testVideoUrls = {
  // HLS流测试
  hls: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8',
  hlsApple: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8',
  
  // MP4测试视频
  mp4: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
  mp4Small: 'https://www.w3schools.com/html/mov_bbb.mp4',
  
  // YouTube测试
  youtube: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
  youtubeShort: 'https://youtu.be/dQw4w9WgXcQ',
  
  // RTSP流示例（需要实际的RTSP服务器）
  rtsp: 'rtsp://demo:demo@ipvmdemo.dyndns.org:5541/onvif-media/media.amp',
  
  // 本地测试
  local: 'http://localhost:8080/video/test.mp4'
} 