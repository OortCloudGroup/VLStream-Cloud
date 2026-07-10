/**
 * 地图配置工具类
 * 提供稳定的瓦片服务器配置和错误处理
 */

export class MapConfig {
  // 标准地图瓦片服务器列表（按优先级排序）
  static standardServers = [
    {
      url: 'https://tile.openstreetmap.de/{z}/{x}/{y}.png',
      name: 'OpenStreetMap DE',
      description: '德国镜像服务器'
    },
    {
      url: 'https://tiles.wmflabs.org/osm/{z}/{x}/{y}.png',
      name: 'OpenStreetMap Wikimedia',
      description: 'Wikimedia镜像服务器'
    },
    {
      url: 'https://cartodb-basemaps-{s}.global.ssl.fastly.net/light_all/{z}/{x}/{y}.png',
      name: 'CartoDB Light',
      description: 'CartoDB浅色主题'
    },
    {
      url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}',
      name: 'Esri World Street Map',
      description: 'Esri世界街道地图'
    }
  ]

  // 卫星地图瓦片服务器列表
  static satelliteServers = [
    {
      url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      name: 'Esri World Imagery',
      description: 'Esri世界影像'
    },
    {
      url: 'https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}',
      name: 'Google Satellite',
      description: 'Google卫星影像'
    }
  ]

  // 当前使用的服务器索引
  static currentStandardIndex = 0
  static currentSatelliteIndex = 0

  /**
   * 获取当前标准地图服务器配置
   */
  static getCurrentStandardServer() {
    return this.standardServers[this.currentStandardIndex]
  }

  /**
   * 获取当前卫星地图服务器配置
   */
  static getCurrentSatelliteServer() {
    return this.satelliteServers[this.currentSatelliteIndex]
  }

  /**
   * 切换到下一个标准地图服务器
   */
  static switchToNextStandardServer() {
    this.currentStandardIndex = (this.currentStandardIndex + 1) % this.standardServers.length
    return this.getCurrentStandardServer()
  }

  /**
   * 切换到下一个卫星地图服务器
   */
  static switchToNextSatelliteServer() {
    this.currentSatelliteIndex = (this.currentSatelliteIndex + 1) % this.satelliteServers.length
    return this.getCurrentSatelliteServer()
  }

  /**
   * 创建标准地图图层
   */
  static createStandardLayer() {
    const server = this.getCurrentStandardServer()
    return L.tileLayer(server.url, {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 18,
      errorTileUrl: '/src/assets/error-tile.svg',
      timeout: 10000, // 10秒超时
      retryUrl: true // 启用重试
    })
  }

  /**
   * 创建卫星地图图层
   */
  static createSatelliteLayer() {
    const server = this.getCurrentSatelliteServer()
    return L.tileLayer(server.url, {
      attribution: '&copy; <a href="https://www.esri.com/">Esri</a> contributors',
      maxZoom: 18,
      errorTileUrl: '/src/assets/error-tile.svg',
      timeout: 10000, // 10秒超时
      retryUrl: true // 启用重试
    })
  }

  /**
   * 测试瓦片服务器连接
   */
  static async testServer(server) {
    try {
      const testUrl = server.url.replace('{z}', '0').replace('{x}', '0').replace('{y}', '0').replace('{s}', 'a')
      const response = await fetch(testUrl, { 
        method: 'HEAD', 
        timeout: 5000 
      })
      return response.ok
    } catch (error) {
      console.warn(`瓦片服务器测试失败: ${server.name}`, error)
      return false
    }
  }

  /**
   * 自动选择最优服务器
   */
  static async selectBestServer(servers) {
    for (let i = 0; i < servers.length; i++) {
      const server = servers[i]
      const isWorking = await this.testServer(server)
      if (isWorking) {
        console.log(`选择瓦片服务器: ${server.name}`)
        return i
      }
    }
    console.warn('所有瓦片服务器测试失败，使用默认服务器')
    return 0
  }
}

// 地图样式配置
export const mapStyles = {
  container: {
    width: '100%',
    height: '100%',
    position: 'relative'
  },
  
  // 地图控件样式
  controls: {
    position: 'absolute',
    top: '10px',
    right: '10px',
    zIndex: 1000
  },
  
  // 加载提示样式
  loading: {
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    zIndex: 1001,
    background: 'rgba(255, 255, 255, 0.9)',
    padding: '10px 20px',
    borderRadius: '5px',
    boxShadow: '0 2px 5px rgba(0, 0, 0, 0.2)'
  }
}

export default MapConfig 