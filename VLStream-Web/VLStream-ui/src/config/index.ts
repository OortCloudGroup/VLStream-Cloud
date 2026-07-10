/*
* @Created by: 兰舰
* Email: gglanjian@qq.com
* Phone: 16620805419
* @Date: 2024-11-15 10:53:37
 * @Last Modified by: 兰舰
 * @Last Modified time: 2025-07-15 10:52:25
* @Copyright aPaaS-front-team. All rights reserved.
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
  // URL: 'http://183.62.103.20:21410/', // 测试环境IP
  // URL: 'https://workup-dev.myoumuamua.com:6433/', // 测试环境 https
  gateWay: 'oort/',
  // 配置文件
  configJSONData: {},
  // 登录界面是否显示微信扫码登录
  weixinLogin: false,
  // 登录界面是否显示 服务协议和用户政策
  showDeal: false,
  // 请求头
  headers: {},
  frontURLStr: '/bus/apaas-web',
  highTenantId: '0e391fd7-1033-4f09-88c0-187582fee462', // 顶级租户id 固定的不会变
  adminUserId: '6799ea6d-dec6-4b34-961c-a7b5f8c6c900', // 管理员id 固定的不会变,
  // bigBlueBaseURL: 'https://video-big.myoumuamua.com',
  bigBlueBaseURL: 'https://oort.oortcloudsmart.com:13443',
  bigBlueBaseURLLivep: 'https://livep.oortcloudsmart.com:443'
}

if (env === 'ny') {
  config.URL = 'http://192.168.50.15:32521/'
  config.gateWay = 'bus/'
}

if (env === 'lt') {
  // config.URL = 'http://183.62.103.20:21410/'
  config.URL = 'https://workup-dev.myoumuamua.com:6433/'
  // config.URL = 'https://workup.oortcloudsmart.com:2443/'
  config.gateWay = 'bus/'
}

// 播放器播放 摄像头的webSocket地址
config.webRTCSocketURL = 'ws://146.56.220.167:8082'

// 仓库环境本地调试
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

// 是否允许IM 登录。如果用到IM的功能就需要 这里数据时 app.vue 的tag 字符串
config.allowIMLoginPlatform = ['desktopHome', 'desktopHome_ai', 'commandDispatch', 'XorkWeb',
  'garden_inspection', 'message_app', 'address_book_app', 'loginPage', 'console_manage']

export default config
