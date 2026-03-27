/**
 Created by  lanjian   on 2020/5/26  10:51
 Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
 */

'use strict'
const path = require('path')

function resolve(dir) {
  return path.join(__dirname, dir)
}

module.exports = {
  context: path.resolve(__dirname, './'),
  resolve: {
    extensions: ['.js', '.vue', '.json'],
    alias: {
      '@': resolve('src')
    }
  }
}
