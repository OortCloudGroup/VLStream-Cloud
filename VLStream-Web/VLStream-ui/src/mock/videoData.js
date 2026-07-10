// Mock 视频数据
export const mockVideoData = () => {
  return [
    {
      id: 1,
      name: '大门入口摄像头',
      location: '主楼大门',
      status: 'online',
      thumbnail: 'https://via.placeholder.com/300x200?text=Camera+1'
    },
    {
      id: 2,
      name: '停车场监控',
      location: '地下停车场',
      status: 'online',
      thumbnail: 'https://via.placeholder.com/300x200?text=Camera+2'
    },
    {
      id: 3,
      name: '走廊监控A',
      location: '一楼走廊',
      status: 'offline',
      thumbnail: 'https://via.placeholder.com/300x200?text=Camera+3'
    },
    {
      id: 4,
      name: '会议室监控',
      location: '二楼会议室',
      status: 'online',
      thumbnail: 'https://via.placeholder.com/300x200?text=Camera+4'
    },
    {
      id: 5,
      name: '后门监控',
      location: '后门出入口',
      status: 'online',
      thumbnail: 'https://via.placeholder.com/300x200?text=Camera+5'
    },
    {
      id: 6,
      name: '楼梯间监控',
      location: '安全楼梯',
      status: 'offline',
      thumbnail: 'https://via.placeholder.com/300x200?text=Camera+6'
    },
    {
      id: 7,
      name: '办公区监控',
      location: '三楼办公区',
      status: 'online',
      thumbnail: 'https://via.placeholder.com/300x200?text=Camera+7'
    },
    {
      id: 8,
      name: '电梯监控',
      location: '客用电梯',
      status: 'online',
      thumbnail: 'https://via.placeholder.com/300x200?text=Camera+8'
    }
  ]
}

// Mock 设备数据
export const mockDeviceData = () => {
  return [
    {
      id: 1,
      name: '海康威视DS-2CD2T47G1',
      ip: '192.168.1.101',
      location: '大门入口',
      status: 'online',
      type: '网络摄像机',
      resolution: '4MP',
      lastOnline: '2024-01-15 14:30:25'
    },
    {
      id: 2,
      name: '大华DH-IPC-HFW4431R-Z',
      ip: '192.168.1.102',
      location: '停车场',
      status: 'online',
      type: '网络摄像机',
      resolution: '4MP',
      lastOnline: '2024-01-15 14:29:18'
    },
    {
      id: 3,
      name: '宇视IPC322LR3-VSPF28-D',
      ip: '192.168.1.103',
      location: '一楼走廊',
      status: 'offline',
      type: '网络摄像机',
      resolution: '2MP',
      lastOnline: '2024-01-14 16:45:32'
    },
    {
      id: 4,
      name: '海康威视DS-2CD2347G1',
      ip: '192.168.1.104',
      location: '会议室',
      status: 'online',
      type: '网络摄像机',
      resolution: '4MP',
      lastOnline: '2024-01-15 14:28:45'
    }
  ]
}

// Mock 事件数据
export const mockEventData = () => {
  return [
    {
      id: 1,
      title: '人员入侵检测',
      description: '检测到非授权人员进入禁区',
      deviceName: '大门入口摄像头',
      location: '主楼大门',
      time: '2024-01-15 14:25:30',
      status: 'pending',
      level: 'high',
      type: '安全事件'
    },
    {
      id: 2,
      title: '车辆违停检测',
      description: '检测到车辆在禁停区域停放',
      deviceName: '停车场监控',
      location: '地下停车场',
      time: '2024-01-15 13:45:12',
      status: 'processing',
      level: 'medium',
      type: '交通事件'
    },
    {
      id: 3,
      title: '设备离线告警',
      description: '走廊监控A设备失去连接',
      deviceName: '走廊监控A',
      location: '一楼走廊',
      time: '2024-01-14 16:45:32',
      status: 'completed',
      level: 'low',
      type: '设备故障'
    },
    {
      id: 4,
      title: '人脸识别成功',
      description: '识别到授权人员张三',
      deviceName: '大门入口摄像头',
      location: '主楼大门',
      time: '2024-01-15 09:15:20',
      status: 'completed',
      level: 'info',
      type: '识别事件'
    }
  ]
}

// Mock算法数据
export const mockAlgorithmData = () => {
  return [
    {
      id: 1,
      name: '人脸识别算法',
      version: 'v2.1.0',
      type: '人脸识别',
      status: 'active',
      accuracy: '98.5%',
      description: '基于深度学习的人脸识别算法，支持多角度人脸检测和识别，具有高准确率和快速响应能力。',
      createTime: '2024-01-01T00:00:00Z',
      updateTime: '2024-01-10T10:30:00Z'
    },
    {
      id: 2,
      name: '车牌识别算法',
      version: 'v1.8.3',
      type: '车牌识别',
      status: 'active',
      accuracy: '96.8%',
      description: '支持多种车牌类型识别，包括蓝牌、黄牌、绿牌等，适用于各种光照条件和角度。',
      createTime: '2024-01-02T00:00:00Z',
      updateTime: '2024-01-12T14:20:00Z'
    },
    {
      id: 3,
      name: '行为分析算法',
      version: 'v1.5.2',
      type: '行为分析',
      status: 'testing',
      accuracy: '92.3%',
      description: '智能行为分析算法，可检测异常行为、人员聚集、打架斗殴等事件。',
      createTime: '2024-01-03T00:00:00Z',
      updateTime: '2024-01-14T16:45:00Z'
    },
    {
      id: 4,
      name: '物体检测算法',
      version: 'v3.0.1',
      type: '物体检测',
      status: 'active',
      accuracy: '94.7%',
      description: '通用物体检测算法，支持多类别物体识别，包括人员、车辆、包裹等。',
      createTime: '2024-01-04T00:00:00Z',
      updateTime: '2024-01-13T11:15:00Z'
    },
    {
      id: 5,
      name: '烟火检测算法',
      version: 'v1.2.0',
      type: '物体检测',
      status: 'inactive',
      accuracy: '89.2%',
      description: '专门用于检测烟雾和火焰的算法，适用于消防安全监控场景。',
      createTime: '2024-01-05T00:00:00Z',
      updateTime: '2024-01-11T09:30:00Z'
    },
    {
      id: 6,
      name: '人员计数算法',
      version: 'v2.3.1',
      type: '行为分析',
      status: 'active',
      accuracy: '95.1%',
      description: '精确的人员计数算法，支持实时人流统计和密度分析。',
      createTime: '2024-01-06T00:00:00Z',
      updateTime: '2024-01-15T08:20:00Z'
    }
  ]
}

// Mock 算法训练数据
export const mockTrainingData = () => {
  return [
    {
      id: 1,
      name: '人脸识别模型训练',
      algorithmType: '人脸识别',
      datasetName: '人脸数据集v1.0',
      datasetSize: '50000张图片',
      status: 'training',
      progress: 75,
      accuracy: 0,
      epoch: 45,
      totalEpochs: 100,
      startTime: '2024-01-15 09:00:00',
      estimatedTime: '2小时30分钟',
      gpuUsage: '85%',
      createTime: '2024-01-15 09:00:00'
    },
    {
      id: 2,
      name: '车牌识别优化训练',
      algorithmType: '车牌识别',
      datasetName: '车牌数据集v2.1',
      datasetSize: '30000张图片',
      status: 'completed',
      progress: 100,
      accuracy: 96.8,
      epoch: 80,
      totalEpochs: 80,
      startTime: '2024-01-14 10:00:00',
      estimatedTime: '已完成',
      gpuUsage: '0%',
      createTime: '2024-01-14 10:00:00'
    },
    {
      id: 3,
      name: '行为检测模型训练',
      algorithmType: '行为分析',
      datasetName: '行为数据集v1.5',
      datasetSize: '80000个视频片段',
      status: 'pending',
      progress: 0,
      accuracy: 0,
      epoch: 0,
      totalEpochs: 120,
      startTime: '',
      estimatedTime: '4小时15分钟',
      gpuUsage: '0%',
      createTime: '2024-01-15 14:30:00'
    }
  ]
}

// Mock 算法模型数据
export const mockModelData = () => {
  return [
    {
      id: 1,
      name: '人脸识别模型v2.1',
      algorithmType: '人脸识别',
      version: 'v2.1.0',
      accuracy: 98.5,
      size: '256MB',
      format: 'ONNX',
      status: 'published',
      downloadCount: 1247,
      deployCount: 8,
      createTime: '2024-01-10 15:30:00',
      publishTime: '2024-01-11 09:00:00'
    },
    {
      id: 2,
      name: '车牌识别模型v1.8',
      algorithmType: '车牌识别',
      version: 'v1.8.3',
      accuracy: 96.8,
      size: '128MB',
      format: 'ONNX',
      status: 'published',
      downloadCount: 856,
      deployCount: 12,
      createTime: '2024-01-08 11:20:00',
      publishTime: '2024-01-09 14:15:00'
    },
    {
      id: 3,
      name: '行为分析模型v1.5',
      algorithmType: '行为分析',
      version: 'v1.5.2',
      accuracy: 92.3,
      size: '512MB',
      format: 'PyTorch',
      status: 'testing',
      downloadCount: 234,
      deployCount: 3,
      createTime: '2024-01-12 16:45:00',
      publishTime: ''
    }
  ]
}

// Mock 容器实例数据
export const mockContainerData = () => {
  return [
    {
      id: 1,
      name: 'face-recognition-01',
      image: 'vlstream/face-recognition:v2.1.0',
      status: 'running',
      cpu: '2 cores',
      memory: '4GB',
      gpu: 'NVIDIA RTX 3080',
      port: '8080',
      uptime: '2天15小时',
      deployTime: '2024-01-13 09:30:00',
      lastUpdate: '2024-01-15 14:25:00',
      logs: [
        { time: '2024-01-15 14:25:00', level: 'INFO', message: '人脸检测任务完成，识别到3张人脸' },
        { time: '2024-01-15 14:20:00', level: 'INFO', message: '开始处理新的视频流' },
        { time: '2024-01-15 14:15:00', level: 'WARNING', message: '检测到低质量图像，跳过处理' }
      ]
    },
    {
      id: 2,
      name: 'license-plate-02',
      image: 'vlstream/license-plate:v1.8.3',
      status: 'running',
      cpu: '1 core',
      memory: '2GB',
      gpu: 'NVIDIA GTX 1660',
      port: '8081',
      uptime: '5天8小时',
      deployTime: '2024-01-10 14:20:00',
      lastUpdate: '2024-01-15 14:20:00',
      logs: [
        { time: '2024-01-15 14:20:00', level: 'INFO', message: '车牌识别完成：京A12345' },
        { time: '2024-01-15 14:18:00', level: 'INFO', message: '检测到新车辆进入' },
        { time: '2024-01-15 14:15:00', level: 'ERROR', message: '车牌区域模糊，识别失败' }
      ]
    },
    {
      id: 3,
      name: 'behavior-analysis-03',
      image: 'vlstream/behavior-analysis:v1.5.2',
      status: 'stopped',
      cpu: '4 cores',
      memory: '8GB',
      gpu: 'NVIDIA RTX 4090',
      port: '8082',
      uptime: '0小时',
      deployTime: '2024-01-12 16:45:00',
      lastUpdate: '2024-01-14 10:30:00',
      logs: [
        { time: '2024-01-14 10:30:00', level: 'INFO', message: '容器正常停止' },
        { time: '2024-01-14 10:25:00', level: 'WARNING', message: '内存使用率过高，准备重启' },
        { time: '2024-01-14 09:00:00', level: 'INFO', message: '行为分析任务启动' }
      ]
    }
  ]
}

// Mock 标签数据
export const mockTagData = () => {
  return [
    {
      id: 1,
      name: '重要区域',
      color: '#f56c6c',
      deviceCount: 8,
      description: '需要重点监控的区域',
      createTime: '2024-01-01 10:00:00'
    },
    {
      id: 2,
      name: '出入口',
      color: '#409eff',
      deviceCount: 6,
      description: '大楼各个出入口位置',
      createTime: '2024-01-02 11:30:00'
    },
    {
      id: 3,
      name: '停车场',
      color: '#67c23a',
      deviceCount: 4,
      description: '地下停车场区域',
      createTime: '2024-01-03 14:15:00'
    },
    {
      id: 4,
      name: '办公区',
      color: '#e6a23c',
      deviceCount: 12,
      description: '办公楼内部区域',
      createTime: '2024-01-04 09:45:00'
    }
  ]
}

// Mock 智能分析结果数据
export const mockAnalysisResultData = () => {
  return [
    {
      id: 1,
      type: '人脸识别',
      deviceName: '大门入口摄像头',
      result: '识别到员工张三',
      confidence: 98.5,
      timestamp: '2024-01-15 14:30:25',
      thumbnail: 'https://via.placeholder.com/100x100?text=Face',
      details: {
        personId: 'P001',
        personName: '张三',
        department: '技术部',
        age: 28,
        gender: '男'
      }
    },
    {
      id: 2,
      type: '车牌识别',
      deviceName: '停车场监控',
      result: '京A12345',
      confidence: 96.8,
      timestamp: '2024-01-15 14:25:12',
      thumbnail: 'https://via.placeholder.com/100x60?text=Plate',
      details: {
        plateNumber: '京A12345',
        vehicleType: '小型汽车',
        color: '白色',
        direction: '进入'
      }
    },
    {
      id: 3,
      type: '行为分析',
      deviceName: '走廊监控A',
      result: '检测到异常聚集',
      confidence: 89.2,
      timestamp: '2024-01-15 14:20:08',
      thumbnail: 'https://via.placeholder.com/100x100?text=Behavior',
      details: {
        behavior: '人员聚集',
        personCount: 5,
        duration: '3分钟',
        level: '中等'
      }
    }
  ]
}

// Mock 监控告警数据
export const mockAlarmData = () => {
  return [
    {
      id: 1,
      title: '人员入侵告警',
      type: '安全告警',
      level: 'high',
      deviceName: '周界监控01',
      location: '东侧围栏',
      description: '检测到非授权人员翻越围栏',
      status: 'active',
      triggerTime: '2024-01-15 14:35:20',
      acknowledgeTime: '',
      resolveTime: '',
      handler: '',
      thumbnail: 'https://via.placeholder.com/150x100?text=Intrusion'
    },
    {
      id: 2,
      title: '设备离线告警',
      type: '设备告警',
      level: 'medium',
      deviceName: '楼梯间监控',
      location: '安全楼梯',
      description: '设备失去网络连接',
      status: 'acknowledged',
      triggerTime: '2024-01-15 14:20:15',
      acknowledgeTime: '2024-01-15 14:22:30',
      resolveTime: '',
      handler: '王工程师',
      thumbnail: 'https://via.placeholder.com/150x100?text=Offline'
    },
    {
      id: 3,
      title: '车辆违停告警',
      type: '交通告警',
      level: 'low',
      deviceName: '停车场监控',
      location: '消防通道',
      description: '车辆违规停放在消防通道',
      status: 'resolved',
      triggerTime: '2024-01-15 13:45:30',
      acknowledgeTime: '2024-01-15 13:50:15',
      resolveTime: '2024-01-15 14:10:00',
      handler: '保安李师傅',
      thumbnail: 'https://via.placeholder.com/150x100?text=Parking'
    }
  ]
}

// Mock 用户数据
export const mockUserData = () => {
  return [
    {
      id: 1,
      username: 'admin',
      realName: '系统管理员',
      email: 'admin@vlstream.com',
      phone: '13800138000',
      role: '超级管理员',
      department: '技术部',
      status: 'active',
      lastLogin: '2024-01-15 14:30:00',
      createTime: '2024-01-01 00:00:00'
    },
    {
      id: 2,
      username: 'operator',
      realName: '操作员',
      email: 'operator@vlstream.com',
      phone: '13800138001',
      role: '操作员',
      department: '安保部',
      status: 'active',
      lastLogin: '2024-01-15 13:45:00',
      createTime: '2024-01-02 10:30:00'
    },
    {
      id: 3,
      username: 'viewer',
      realName: '观察员',
      email: 'viewer@vlstream.com',
      phone: '13800138002',
      role: '观察员',
      department: '管理部',
      status: 'inactive',
      lastLogin: '2024-01-10 16:20:00',
      createTime: '2024-01-03 14:15:00'
    }
  ]
}

// Mock 系统设置数据
export const mockSystemSettings = () => {
  return {
    general: {
      systemName: 'VLStream Cloud',
      systemLogo: '/logo.png',
      description: '智能视频流管理平台',
      version: 'v1.0.0',
      timezone: 'Asia/Shanghai',
      language: 'zh-CN'
    },
    storage: {
      videoRetentionDays: 30,
      logRetentionDays: 90,
      maxStorageSize: '1TB',
      autoCleanup: true,
      compressionEnabled: true
    },
    notification: {
      emailEnabled: true,
      smsEnabled: false,
      webhookEnabled: true,
      webhookUrl: 'https://api.example.com/webhook'
    },
    algorithm: {
      maxConcurrentTasks: 10,
      defaultConfidenceThreshold: 0.8,
      enableGpuAcceleration: true,
      modelCacheSize: '2GB'
    }
  }
}