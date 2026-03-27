/**
 Created by  lanjian   on 2020/9/7  21:06
 Copyright 奥尔特云（深圳）智慧科技有限公司. All rights reserved.
 */
export default function GetQueryString(url) {
  // 把url的参数部分转化成json对象
  let reg_url = /^[^\?]+\?([\w\W]+)$/
  let reg_para = /([^&=]+)=([\w\W]*?)(&|$|#)/g
  let arr_url = reg_url.exec(url)
  let ret = {}
  if (arr_url && arr_url[1]) {
    let str_para = arr_url[1]; let result
    while ((result = reg_para.exec(str_para)) != null) {
      ret[result[1]] = result[2]
    }
  }
  return ret
}
