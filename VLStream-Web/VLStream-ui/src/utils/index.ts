/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

export function getTimeStampByDate(t) {
    const d = new Date(t);
    return d.getTime();
  }

  export function getDateString(
    t,
    format = "yyyy/MM/dd hh:mm:ss"
  ) {
    const d = new Date(getTimeStampByDate(t));

    const year = d.getFullYear();
    const month = d.getMonth() + 1;
    const date = d.getDate();
    const hours = d.getHours();
    const minutes = d.getMinutes();
    const seconds = d.getSeconds();

    const formatedString = format
      .replace("yyyy", String(year))
      .replace("MM", String(month))
      .replace("dd", String(date))
      .replace("hh", String(hours))
      .replace("mm", String(minutes))
      .replace("ss", String(seconds));

    return formatedString;
  }

  // Convert to
  export function formatDate(value) {
    if (!value) {
      return ''
    }
    if (value.toString().length === 10) {
      value = value * 1000
    }
    if (value.toString().length === 13) {
      return returnTime(value)
    }
    if (value.toString().includes('CST')) {
      value = value.split("CST")[0]
    }
    const date = new Date(value)

    let reStr = returnTime(date.getTime())
    if (!reStr) {
      const dateNumFun = (num) => num < 10 ? `0${num}` : num
      const [Y, M, D, h, m, s] = [
        date.getFullYear(),
        dateNumFun(date.getMonth() + 1),
        dateNumFun(date.getDate()),
        dateNumFun(date.getHours()),
        dateNumFun(date.getMinutes()),
        dateNumFun(date.getSeconds())
      ]
      if (date.getFullYear() !== new Date().getFullYear()) {
        return `${Y}-${M}-${D} ${h}:${m}`
      } else {
        return `${M}-${D} ${h}:${m}`
      }
    }else {
      return reStr
    }
  }

  function returnTime(value) {
      let nowTimes = new Date().getTime()
      if (nowTimes - value < 60 * 1000) {
        if (Math.floor((nowTimes - value) / 1000) < 5 ) {
          return '刚刚'
        } else {
          return Math.floor((nowTimes - value) / 1000) + '秒前'
        }
      }
      if (nowTimes - value < 60 * 60 * 1000) {
        return Math.floor((nowTimes - value) / (60 * 1000)) + '分钟前'
      }
      if (nowTimes - value < 24 * 60 * 60 * 1000) {
        return Math.floor((nowTimes - value) / (60 * 60 * 1000)) + '小时前'
      }
      return getDateString(value, "yyyy-MM-dd")
  }
  export function formatDateToFull(value) {
    if (!value) {
      return ''
    }
    if (value instanceof Date) {
      value = value.getTime()
    } else {
      let tempStr = value + ''
      if (tempStr.length !== 13) {
        value = value * 1000
      }
    }
    const date = new Date(value)
    const dateNumFun = (num) => num < 10 ? `0${num}` : num
    const [Y, M, D, h, m, s] = [
      date.getFullYear(),
      dateNumFun(date.getMonth() + 1),
      dateNumFun(date.getDate()),
      dateNumFun(date.getHours()),
      dateNumFun(date.getMinutes()),
      dateNumFun(date.getSeconds())
    ]
    return `${Y}-${M}-${D} ${h}:${m}:${s}`
  }


  export function formatDateToWeek(value) {
    if (!value) {
      return ''
    }
    if (value instanceof Date) {
      value = value.getTime()
    } else {
      let tempStr = value + ''
      if (tempStr.length !== 13) {
        value = value * 1000
      }
    }
    const date = new Date(value)
    const dateNumFun = (num) => num < 10 ? `0${num}` : num
    const [Y, M, D, h, m, s] = [
      date.getFullYear(),
      dateNumFun(date.getMonth() + 1),
      dateNumFun(date.getDate()),
      dateNumFun(date.getHours()),
      dateNumFun(date.getMinutes()),
      dateNumFun(date.getSeconds())
    ]
    let day = date.getDay()
    let weeks = new Array('星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六')
    let week = weeks[day]
    return `${Y} 年 ${M} 月 ${D} 日  ${week}`
  }

  export function shortRandomStr(prefix = 'node_') {
      const randomPart = Math.random().toString(36).substring(2, 7); // Generate 5
      return prefix + randomPart;
  }

  export function clacPXToVW(data, baseWidth=1920) {
    let retV = window.innerWidth * data / baseWidth
    if (retV < data) {
      return retV + ''
    } else {
      return window.innerWidth * data / baseWidth + ''
    }
  }

  // Convert
  export function getConverDistance(meters: number): string {
    if (meters < 1000) {
      return `${Math.round(meters)}米`
    } else {
      return `${(meters / 1000).toFixed(2)}公里`
    }
  }

  // Format
  export function formatDateToHM(timestamp: number): string {
    const date = new Date(timestamp)
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    return `${hours}:${minutes}`
  }

export function getVideoCover(url) {
  return new Promise(function(resolve, reject) {
    let dataURL = ''
    let video = document.createElement('video')
    video.setAttribute('crossOrigin', 'anonymous')// Process
    video.setAttribute('src', url)
    video.setAttribute('width', 400)
    video.setAttribute('height', 240)
    video.setAttribute('preload', 'auto')
    video.addEventListener('loadeddata', function() {
      let canvas = document.createElement('canvas')
      let width = video.width // canvas and
      let height = video.height
      canvas.width = width
      canvas.height = height
      canvas.getContext('2d').drawImage(video, 0, 0, width, height) // canvas
      dataURL = canvas.toDataURL('image/jpeg') // Convert to base64
      resolve(dataURL)
    })
  })
}


/**
 * Check current pagewhether in main , failed .
 */
export function jugeIsInLocalhostEnv () {
  return ['localhost', '127.0.0.1', '::1', '[::1]', 'lanjian.com'].includes(window.location.hostname)
  // || window.location.hostname === 'workup-dev.myoumuamua.com' // to Set , only
}
