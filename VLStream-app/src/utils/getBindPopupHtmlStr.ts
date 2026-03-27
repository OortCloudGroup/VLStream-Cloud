
import carIcon from '@/assets/img/ditu/car_active.png'
import trackIcon from '@/assets/img/ditu/trackIcon.png'
import phoneIcon from '@/assets/img/ditu/dh_icon.png'
import userIcon from '@/assets/img/ditu/p3.png'
import locatIcon from '@/assets/img/ditu/p7.png'
import phoneIcon2 from '@/assets/img/ditu/p4.png'
import addressIcon from '@/assets/img/ditu/wz_icon.png'
import gp_activeIcon from '@/assets/img/ditu/gp_active2.png'
import jwdIcon from '@/assets/img/ditu/jwd_icon.png'
import treeIcon from '@/assets/img/ditu/gushu.png'
import slIcon from '@/assets/img/ditu/sl.png'
import zbzlIcon from '@/assets/img/ditu/zbzl.png'
import idIcon from '@/assets/img/ditu/id.png'
import levelIcon from '@/assets/img/ditu/level.png'

import bus from '@/utils/bus'

// 初始化地图弹框的按钮点击事件
export function initWindowMapBtnClick() {
  // 全局的方法， 用于地图弹框的按钮点击事件
  window.mapBtnClick = function(type, ...args) {
    console.log('mapBtnClick-----', type, ...args)
    switch (type) {
      case 'mapGPBtnHistroy':
        bus.$emit('showReplayDialog', { 'type': 'gp', ...args })
        break
      case 'mapGPLocationNow':
        break
      case 'mapGPRestart':
        break
      case 'mapBtnHistroy':
        break
      case 'mapBtnSignList':
        break
      case 'mapBtnReport':
        break
      case 'mapBtnVideo':
        break
      default:
        break
    }
  }
  console.log('window.mapBtnClick', window)
}

export function getHtmlDefaultTemplate(obj: any) {
  let fieldsMap = obj.fieldsMap
  if (!fieldsMap || Object.keys(fieldsMap).length === 0) {
    // 如果没有 fieldsMap,则显示所有属性
    let str2 = ''
    let keys = Object.keys(obj).filter(key => key !== 'fieldsMap' && key !== 'featureId' && key !== 'id')
    keys.sort((a, b) => {
      if (a === 'name') return -1
      if (b === 'name') return 1
      if (a === 'chineseName') return -1
      if (b === 'chineseName') return 1
      return 0
    })

    for (let key of keys) {
      str2 += `<div class="info_window_item">
        <span>${key}: ${obj[key]}</span>
      </div>`
    }
    let tempName = obj.name || obj.userName || ''
    let str = '<div class="info_window">' +
      '      <div class="info_window_item">' +
      '        <span style="font-size: 26px;color:#027Aff">' + tempName + '</span>' +
      '      </div>' +
      '      ' + str2 +
      '    </div>'
    return str
  }
  let str2 = ''
  // 获取所有需要显示的属性
  let keys = Object.keys(obj).filter(key => key !== 'fieldsMap' && key !== 'featureId' && key !== 'id')
  keys.sort((a, b) => {
    if (a === 'name' || a === 'userName') return -1
    if (b === 'name' || b === 'userName') return 1
    if (a === 'chineseName') return -1
    if (b === 'chineseName') return 1
    return 0
  })

  for (let key of keys) {
    if (fieldsMap[key]) {
      str2 += `<div class="info_window_item">
        <span>${fieldsMap[key]}: ${obj[key]}</span>
      </div>`
    }
  }
  let tempName = obj.name || obj.userName || ''
  let str = '<div class="info_window">' +
    '      <div class="info_window_item">' +
    '        <span style="font-size: 26px;color:#027Aff">' + tempName + '</span>' +
    '      </div>' +
    '      ' + str2 +
    '    </div>'
  return str
}

export function getHtmlTreeTemplate(obj) {
  let fieldsMap = obj.fieldsMap
  if (!fieldsMap) return
  let str2 = ''
  // 获取所有需要显示的属性
  let keys = Object.keys(obj).filter(key => key !== 'fieldsMap' && key !== 'featureId' && key !== 'id')
  keys.sort((a, b) => {
    if (a === 'name') return -1
    if (b === 'name') return 1
    if (a === 'chineseName') return -1
    if (b === 'chineseName') return 1
    return 0
  })

  for (let key of keys) {
    let icon = jwdIcon
    if (key === 'name') icon = treeIcon
    if (key === 'chineseName') icon = treeIcon
    if (key === 'growthStatus') icon = zbzlIcon
    if (key === 'treeNo') icon = idIcon
    if (key === 'protectionLevel') icon = levelIcon
    if (key === 'age') icon = slIcon
    str2 += `<div class="info_window_item">
        <img src="${icon}">
        <span>${fieldsMap[key]}: ${obj[key]}</span>
      </div>`
  }
  let tempName = obj.name || ''
  let str = '<div class="info_window">' +
    '      <div class="info_window_item">' +
    '        <span style="font-size: 26px;color:#027Aff">' + tempName + '</span>' +
    '      </div>' +
    '      ' + str2 +
    '    </div>'
  return str
}

// 人员弹框信息++++
export function getHtmlPersonTemplate({ name, imei, device_type, address }) {
  // 巡逻地址和联系电话 接口没有返回，暂时写死
  let tid = '巡逻地址'
  let phone = '1234567890'
  name = name.replace(/\s+/g, '')
  let str = '<div class="info_window" style="min-width: 350px;">' +
      '      <div class="topIconStyle">' +
      (device_type === 1
        ? ('<img src="' + gp_activeIcon + '">' + '<span>工牌</span>')
        : ('<img src="' + carIcon + '">' + '<span>车牌</span>')
      ) +

      '      </div>' +
      '      <div class="info_window_item">' +
      '        <span style="font-size: 26px;color:#027Aff;margin-left: 10px;">' + name + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + userIcon + '">' +
      '        <span>单位:<span class="space" style="display:inline-block;width:10px"></span>' + tid + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + phoneIcon2 + '">' +
      '        <span>联系电话:<span class="space" style="display:inline-block;width:10px"></span>' + phone + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + locatIcon + '">' +
      '        <span>当前位置:<span class="space" style="display:inline-block;width:10px"></span>' + address + '</span>' +
      '      </div>' +
      '      <div class="lineStyle"></div>' +
      '      <div class="map_btns_group2"> ' +
      '        <div onclick=mapBtnClick("mapGPBtnHistroy","' + imei + '","' + imei + '","' + name + '") class="map_btns_group_item">' +
      '          <img class="trackIcon" src="' + trackIcon + '"><span>历史轨迹</span>' +
      '        </div>' +
      '       </div>' +
      '    </div>'
  return str
}

export function getHtmlTemplate({ tid, phone, name, address, task_id, uuid }) {
  name = name.replace(/\s+/g, '')
  let str = '<div class="info_window">' +
      '      <div class="info_window_item">' +
      '        <span style="font-size: 26px;color:#027Aff">' + name + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + phoneIcon + '">' +
      '        <span>设备ID:' + tid + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + phoneIcon + '">' +
      '        <span>联系电话:' + phone + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + addressIcon + '">' +
      '        <span>当前位置:' + address + '</span>' +
      '      </div>' +
      '      <div class="map_btns_group"> ' +
      '        <div onclick=mapBtnClick("mapBtnHistroy","' + task_id + '","' + uuid + '","' + name + '") class="map_btns_group_item">' +
      '          <span>历史轨迹</span>' +
      '        </div>' +
      '        <div onclick=mapBtnClick("mapBtnSignList","' + task_id + '","' + uuid + '") class="map_btns_group_item">' +
      '          <span>打卡点</span>' +
      '        </div>' +
      '        <div onclick=mapBtnClick("mapBtnReport","' + task_id + '","' + uuid + '","' + name + '") class="map_btns_group_item">' +
      '         <span>上报事件</span>' +
      '        </div>' +
      '        <div onclick=mapBtnClick("mapBtnVideo","' + task_id + '","' + uuid + '","' + name + '") class="map_btns_group_item">' +
      '         <span>视频调度</span>' +
      '        </div>' +
      '       </div>' +
      '    </div>'
  return str
}

// 工牌弹出框
export function getHtmlGPTemplate({ name, online, signal, power, imei, version, position_type, address }) {
  name = name.replace(/\s+/g, '')
  let onlineStr = online ? '是' : '否'
  let positionTypeStr = position_type === 1 ? 'GPS定位' : 'LBS定位'
  let str = '<div class="info_window_sign">' +
      '      <div class="info_window_item">' +
      '        <span style="font-size: 26px;color:#027Aff">' + imei + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>名称: ' + name + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>是否在线: ' + onlineStr + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + phoneIcon + '">' +
      '        <span>信号: ' + signal + '%</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + phoneIcon + '">' +
      '        <span>电量: ' + power + '%</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + phoneIcon + '">' +
      '        <span>软件版本: ' + version + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>地址: ' + address + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>定位类型: ' + positionTypeStr + '</span>' +
      '      </div>' +
      '      <div class="map_btns_group"> ' +
      '        <div onclick=mapBtnClick("mapGPBtnHistroy","' + imei + '","' + imei + '","' + name + '") class="map_btns_group_item">' +
      '          <span>历史轨迹</span>' +
      '        </div>' +
      '        <div onclick=mapBtnClick("mapGPLocationNow","' + imei + '","' + imei + '","' + name + '") class="map_btns_group_item">' +
      '         <span>手动定位</span>' +
      '        </div>' +
      '        <div onclick=mapBtnClick("mapGPRestart","' + imei + '","' + imei + '","' + name + '") class="map_btns_group_item">' +
      '         <span>重   启</span>' +
      '        </div>' +
      '       </div>' +
      '    </div>'
  return str
}

export function getHtmlCarTemplate({ name, online, speed, car_status, imei, position_type, address }) {
  let onlineStr = online ? '是' : '否'
  let positionTypeStr = position_type === 1 ? 'GPS定位' : 'LBS定位'
  let str = '<div class="info_window_sign">' +
      '      <div class="info_window_item">' +
      '        <span style="font-size: 26px;color:#027Aff">' + imei + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>名称: ' + name + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>是否在线: ' + onlineStr + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>速度: ' + speed + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>车辆状态: ' + car_status + '%</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>地址: ' + address + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>定位类型: ' + positionTypeStr + '</span>' +
      '      </div>' +
      '      <div class="map_btns_group"> ' +
      '        <div onclick=mapBtnClick("mapGPBtnHistroy","' + imei + '","' + imei + '","' + name + '") class="map_btns_group_item">' +
      '          <span>历史轨迹</span>' +
      '        </div>' +
      '        <div onclick=mapBtnClick("mapGPLocationNow","' + imei + '","' + imei + '","' + name + '") class="map_btns_group_item">' +
      '         <span>手动定位</span>' +
      '        </div>' +
      '        <div onclick=mapBtnClick("mapGPRestart","' + imei + '","' + imei + '","' + name + '") class="map_btns_group_item">' +
      '         <span>重   启</span>' +
      '        </div>' +
      '       </div>' +
      '    </div>'
  return str
}
export function getHtmlSignTemplate({ address, is_checkin }) {
  let isSignStr = is_checkin ? '是' : '否'
  let str = '<div class="info_window_sign">' +
      '      <div class="info_window_item">' +
      '        <span style="font-size: 26px;color:#027Aff">打卡位置</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>是否打卡:' + isSignStr + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + addressIcon + '">' +
      '        <span>当前打卡点位:' + address + '</span>' +
      '      </div>' +
      '    </div>'
  return str
}

// 事件弹框
export function getHtmlEventTemplate({ address, describe }) {
  let str = '<div class="info_window_sign">' +
      '      <div class="info_window_item">' +
      '        <span style="font-size: 26px;color:#027Aff">事件位置</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>事件描述:' + describe + '</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + addressIcon + '">' +
      '        <span>事件位置:' + address + '</span>' +
      '      </div>' +
      '    </div>'
  return str
}

// getHtmlCameraTemplate 方法 ，按钮有视频播放， 视频 回放， 视频截图
export function getHtmlCameraTemplate({ address, imei, name }) {
  let str = '<div class="info_window_sign">' +
      '      <div class="info_window_item">' +
      '        <span style="font-size: 26px;color:#027Aff">摄像头位置</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>摄像头位置:' + address + '</span>' +
      '      </div>' +
      '      <div class="map_btns_group"> ' +
      '        <div onclick=mapBtnClick("mapGPBtnHistroy","' + imei + '","' + imei + '","' + name + '") class="map_btns_group_item">' +
      '          <span>视频播放</span>' +
      '        </div>' +
      '        <div onclick=mapBtnClick("mapGPLocationNow","' + imei + '","' + imei + '","' + name + '") class="map_btns_group_item">' +
      '         <span>视频回放</span>' +
      '        </div>' +
      '        <div onclick=mapBtnClick("mapGPRestart","' + imei + '","' + imei + '","' + name + '") class="map_btns_group_item">' +
      '         <span>视频截图</span>' +
      '        </div>' +
      '       </div>' +
      '    </div>'
  return str
}
// 绿地的弹框
export function getHtmlGreenTemplate({ address }) {
  let str = '<div class="info_window_sign">' +
      '      <div class="info_window_item">' +
      '        <span style="font-size: 26px;color:#027Aff">绿地位置</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>绿地位置:' + address + '</span>' +
      '      </div>' +
      '    </div>'
  return str
}

// 古树的弹框
export function getHtmlGushuTemplate({ address }) {
  let str = '<div class="info_window_sign">' +
      '      <div class="info_window_item">' +
      '        <span style="font-size: 26px;color:#027Aff">古树位置</span>' +
      '      </div>' +
      '      <div class="info_window_item">' +
      '        <img src="' + jwdIcon + '">' +
      '        <span>古树位置:' + address + '</span>' +
      '      </div>' +
      '    </div>'
  return str
}

