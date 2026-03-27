/**
 Created by 兰舰 on 2020/3/28  0:03
 Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
 */
export function TMap(key) {
  if (!key) {
    key = '腾讯地图 API Key'
  }
  return new Promise(function(resolve, reject) {
    window.initMap = function() {
      resolve()// 注意这里
    }
    let script = document.createElement('script')
    script.type = 'text/javascript'
    // callBack 回调函数
    script.src = 'http://map.qq.com/api/js?v=2.exp&callback=initMap&key=' + key + '&libraries=convertor'
    script.onerror = reject
    document.head.appendChild(script)
  })
}

// 加载定位主键
export function TMapLocation() {
  return new Promise(function(resolve, reject) {
    let script = document.createElement('script')
    script.type = 'text/javascript'
    // callBack 回调函数
    script.src = 'http://3gimg.qq.com/lightmap/components/geolocation/geolocation.min.js'
    script.onerror = reject
    document.head.appendChild(script)
    // 循环调用下直达加载完成
    let ss = setInterval(() => {
      if (window.qq.maps.Geolocation) {
        window.clearInterval(ss)
        resolve()
      }
    }, 1000)
  })
}

// 加载百度地图
export function BDMap(ak) {
  if (!ak) {
    ak = '百度地图 API Key'
  }
  return new Promise(function(resolve, reject) {
    window.initBDMap = function() {
      resolve()// 注意这里
    }
    let script = document.createElement('script')
    script.type = 'text/javascript'
    // callBack 回调函数
    script.src = 'http://api.map.baidu.com/api?type=webgl&v=1.0&ak=' + ak + '&callback=initBDMap'
    script.onerror = reject
    document.head.appendChild(script)
  })
}

// const signKey_tenxunMap = 'ZBWBZ-63DW3-7NT3N-YRUYG-FZL4V-BWB4K'

export function bMapTransqqMap(lat, lng) {
  let x_pi = (Math.PI * 3000.0) / 180.0
  let x = lng - 0.0065
  let y = lat - 0.006
  let z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi)
  let theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi)
  let lngs = z * Math.cos(theta)
  let lats = z * Math.sin(theta)
  return {
    longitude: lngs,
    latitude: lats
  }
}
