/**
 * 经纬度转地址工具函数
 * 使用天地图API将经纬度转换为地址
 */

// 天地图API配置
// const TIANDITU_CONFIG = {
//   tk: '8025bc543451afde7ace7f8ad0582d84',
//   baseUrl: 'http://api.tianditu.gov.cn/geocoder'
// }

// 使用接口
import { geocoderAddress } from '@/api/geoserver'

/**
 * 使用天地图API将经纬度转换为地址
 * @param lng 经度
 * @param lat 纬度
 * @returns 地址字符串或null
 */
export const getAddressFromTianditu = async(lng: number, lat: number): Promise<object | string | null> => {
  // const url = `${TIANDITU_CONFIG.baseUrl}?postStr=\{'lon':${lng},'lat':${lat},'ver':1\}&type=geocode&tk=${TIANDITU_CONFIG.tk}`
  // try {
  //   const response = await fetch(url)
  //   const data = await response.json()

  try {
    let res = await geocoderAddress({ lon: lng, lat: lat })
    let data = null
    if (res.code === 200) {
      data = JSON.parse(res.data)
    }
    if (data && data.status === '0' && data.result) {
      // 返回格式化后的地址
      const address = data.result.formatted_address
      const province = data.result.addressComponent.province
      const city = data.result.addressComponent.city
      const county = data.result.addressComponent.county
      const street = data.result.addressComponent.street
      const town = data.result.addressComponent.town
      const road = data.result.addressComponent.road
      return { code: 200, data: { address, province, city, county, street, town, road }}
    } else {
      return { code: data?.status || 400, msg: data?.msg || '获取地址失败', data: null }
    }
  } catch (error) {
    return '获取地址失败'
  }
}

/**
 * 主要的经纬度转地址函数
 * 使用天地图API将经纬度转换为地址
 * @param lng 经度
 * @param lat 纬度
 * @param options 配置选项
 * @returns 地址字符串或null
 */
export const getAddressFromCoordinates = async(
  lng: number,
  lat: number,
  options: {
    timeout?: number // 超时时间（毫秒），默认5000
  } = {}
): Promise<string | object | null> => {
  const { timeout = 5000 } = options
  try {
    const address = await Promise.race([
      getAddressFromTianditu(lng, lat),
      new Promise<null>((_, reject) =>
        setTimeout(() => reject(new Error('天地图API超时')), timeout)
      )
    ])
    return address
  } catch (error) {
    return null
  }
}

/**
 * 批量转换多个坐标点
 * @param coordinates 坐标数组，格式：[{lng: number, lat: number}]
 * @param options 配置选项
 * @returns 地址数组
 */
export const batchGetAddresses = async(
  coordinates: Array<{lng: number, lat: number}>,
  options: {
    timeout?: number
    delay?: number // 请求间隔（毫秒），避免API限制
  } = {}
): Promise<Array<string | object | null>> => {
  const { timeout = 5000, delay = 100 } = options
  const results: Array<string | object | null> = []

  for (let i = 0; i < coordinates.length; i++) {
    const coord = coordinates[i]
    const address = await getAddressFromCoordinates(coord.lng, coord.lat, { timeout })
    results.push(address)
    // 添加延迟，避免API限制
    if (i < coordinates.length - 1 && delay > 0) {
      await new Promise(resolve => setTimeout(resolve, delay))
    }
  }

  return results
}

export default getAddressFromCoordinates
