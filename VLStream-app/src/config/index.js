/*
 * @Author: lanjian
 * @Date: 2021-12-10 10:35:26
 * @LastEditors: lanjian
 * @LastEditTime: 2021-12-10 10:35:56
 * @FilePath: \cordava_utils\demo\src\config\index.js
 * @Description: Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
 */

let config = {
  headers: {},
  frontURLStr: '/front'
}
config.URL = 'http://183.62.103.20:21410/'
// config.URL = 'https://workup-dev.myoumuamua.com:6433/'
config.token = ''
config.gateWay = 'bus/'

// 播放器播放 摄像头的webSocket地址
config.webRTCSocketURL = 'ws://146.56.220.167:8082'

export default config
