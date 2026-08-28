/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

/**
 * configuration
 * service configuration and Process
 */

export class MapConfig {
  // service ( )
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

  // service
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

  // current service
  static currentStandardIndex = 0
  static currentSatelliteIndex = 0

  /**
   * Get current service configuration
   */
  static getCurrentStandardServer() {
    return this.standardServers[this.currentStandardIndex]
  }

  /**
   * Get current service configuration
   */
  static getCurrentSatelliteServer() {
    return this.satelliteServers[this.currentSatelliteIndex]
  }

  /**
   * service
   */
  static switchToNextStandardServer() {
    this.currentStandardIndex = (this.currentStandardIndex + 1) % this.standardServers.length
    return this.getCurrentStandardServer()
  }

  /**
   * service
   */
  static switchToNextSatelliteServer() {
    this.currentSatelliteIndex = (this.currentSatelliteIndex + 1) % this.satelliteServers.length
    return this.getCurrentSatelliteServer()
  }

  /**
   * layer
   */
  static createStandardLayer() {
    const server = this.getCurrentStandardServer()
    return L.tileLayer(server.url, {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 18,
      errorTileUrl: '/src/assets/error-tile.svg',
      timeout: 10000, // 10
      retryUrl: true //
    })
  }

  /**
   * layer
   */
  static createSatelliteLayer() {
    const server = this.getCurrentSatelliteServer()
    return L.tileLayer(server.url, {
      attribution: '&copy; <a href="https://www.esri.com/">Esri</a> contributors',
      maxZoom: 18,
      errorTileUrl: '/src/assets/error-tile.svg',
      timeout: 10000, // 10
      retryUrl: true //
    })
  }

  /**
   * service
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
   * service
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

// configuration
export const mapStyles = {
  container: {
    width: '100%',
    height: '100%',
    position: 'relative'
  },

  //
  controls: {
    position: 'absolute',
    top: '10px',
    right: '10px',
    zIndex: 1000
  },

  // Load prompt / tip
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