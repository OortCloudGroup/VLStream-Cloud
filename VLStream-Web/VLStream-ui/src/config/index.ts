/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

import { env } from '@/config/envConfig'

type HEADERS = {
  [key: string]: any
}
type Config = {
  URL: string,
  gateWay: string,
  configJSONData: object
  weixinLogin: boolean
  showDeal: boolean
  headers: HEADERS
  common?: any,
  frontURLStr?: string,
  hyConfig?: any,
  allowIMLoginPlatform?: string[]
}

const config: Config = {
  URL: 'http://oort.oortcloudsmart.com:21410/', // 公司测试环境地址
  // URL: 'http://183.62.103.20:21410/', // IP
  gateWay: 'oort/',
  // configuration
  configJSONData: {},
  // whether
  weixinLogin: false,
  // whether service and user
  showDeal: false,
  //
  headers: {},
  frontURLStr: '/bus/apaas-web',
  highTenantId: '0e391fd7-1033-4f09-88c0-187582fee462', // id will
  adminUserId: '6799ea6d-dec6-4b34-961c-a7b5f8c6c900', // administratorid will ,
  // bigBlueBaseURL: 'https://video-big.myoumuamua.com',
  bigBlueBaseURL: 'https://oort.oortcloudsmart.com:13443',
  bigBlueBaseURLLivep: 'https://livep.oortcloudsmart.com:443'
}

if (env === 'ny') {
  config.URL = 'http://192.168.50.15:32521/'
  config.gateWay = 'bus/'
}

if (env === 'lt') {
  // Workflow and work-order APIs use the current Java backend via apaasServiceUrl.
  config.gateWay = 'bus/'
}

// CameraRTC WebSocket signaling follows the current page origin and is
// forwarded by the frontend reverse proxy.
config.webRTCSocketURL = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/bus/camera-rtc`

//
// config.URL = 'http://192.168.88.52:32610'

config.baseMap = {
  standard: {
    name: '标准地图',
    url: 'http://oort.oortcloudsmart.com:21410/bus/geoserver/geoserver/test/wms?',
    layer: 'test:chenzhou-standard'
  },
  dark: {
    name: '暗黑主题',
    url: 'http://oort.oortcloudsmart.com:21410/bus/geoserver/geoserver/test/wms?',
    layer: 'test:chenzhou-dark'
  },
  satellite: {
    name: '卫星影像',
    url: 'http://oort.oortcloudsmart.com:21410/bus/geoserver/geoserver/test/wms?',
    layer: 'test:chenzhou-satellite'
  }
}

// whether IM . if IM can then need to data app.vue tag
config.allowIMLoginPlatform = ['desktopHome', 'desktopHome_ai', 'commandDispatch', 'XorkWeb',
  'garden_inspection', 'message_app', 'address_book_app', 'loginPage', 'console_manage']

export default config
