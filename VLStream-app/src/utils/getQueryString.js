/**
 Created by 兰舰 on 2020/1/15  16:50
 */
export default function GetQueryString(name) {
  let reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)')
  let r = window.location.search.substr(1).match(reg)
  if (r != null) return encodeURIComponent(r[2]); return null
}
