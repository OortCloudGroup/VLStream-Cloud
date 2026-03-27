
import gcoord from 'gcoord'
import L from 'leaflet'
/**
 * BD-09 → EPSG:3857
 * @param {number} lng  BD-09 经度
 * @param {number} lat  BD-09 纬度
 * @return {[number,number]} [x,y] 单位：米
 */
export const bd09To3857 = ([lng, lat], returnType = 'array') => {
  // 1) BD-09 → WGS84
  const [wgsLng, wgsLat] = gcoord.transform([lng, lat], gcoord.BD09, gcoord.WGS84)
  // 2) WGS84 → EPSG:3857
  const [x, y] = gcoord.transform([wgsLng, wgsLat], gcoord.WGS84, gcoord.EPSG3857)
  const point = L.CRS.EPSG3857.unproject(L.point(x, y))
  if (returnType === 'array') {
    return [point.lng, point.lat]
  } else {
    return point
  }
}
