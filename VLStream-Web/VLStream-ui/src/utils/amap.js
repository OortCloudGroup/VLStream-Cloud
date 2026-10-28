import AMapLoader from '@amap/amap-jsapi-loader'
import { getConfigKey } from '@/api/wvp/systemConfig'

// Shared with the location editor: use its existing WVP configuration and SDK.
let apiPromise
export function loadAMap() {
  if (!apiPromise) {
    let timer
    const loading = Promise.all([getConfigKey('amap.security.key'), getConfigKey('amap.key')])
      .then(([security, key]) => {
        if (!key.msg?.trim()) throw new Error('地图未配置')
        window._AMapSecurityConfig = { securityJsCode: security.msg }
        return AMapLoader.load({
          key: key.msg, version: '2.0',
          plugins: ['AMap.AutoComplete', 'AMap.PlaceSearch', 'AMap.Geolocation', 'AMap.Geocoder',
            'AMap.Marker', 'AMap.ToolBar', 'AMap.Scale', 'AMap.HawkEye', 'AMap.MapType'],
          AMapUI: { version: '1.1', plugins: [] },
        })
      })
    apiPromise = Promise.race([
      loading,
      new Promise((_, reject) => { timer = setTimeout(() => reject(new Error('地图服务加载超时')), 15000) }),
    ]).catch(error => { apiPromise = undefined; throw error }).finally(() => clearTimeout(timer))
  }
  return apiPromise
}

export async function reverseGeocode({ lng, lat, coordinateSystem = 'GCJ02' }) {
  const AMap = await loadAMap()
  let position = [lng, lat]
  if (coordinateSystem !== 'GCJ02') {
    const source = { WGS84: 'gps', BD09: 'baidu' }[coordinateSystem]
    if (!source) throw new Error('坐标系未知')
    position = await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error('坐标转换超时')), 10000)
      AMap.convertFrom([lng, lat], source, (status, result) => {
        clearTimeout(timer)
        const converted = result?.locations?.[0]
        if (status === 'complete' && result?.info === 'ok' && converted) resolve(converted)
        else if (status === 'complete' && result?.info === 'OK' && converted) resolve(converted)
        else reject(new Error('坐标转换失败'))
      })
    })
  }
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('地址解析超时')), 10000)
    new AMap.Geocoder({ radius: 1000, extensions: 'all' }).getAddress(position, (status, result) => {
      clearTimeout(timer)
      const address = result?.regeocode?.formattedAddress?.trim()
      if (status === 'complete' && result?.info === 'OK' && address) resolve(address)
      else reject(new Error('地址解析失败'))
    })
  })
}
