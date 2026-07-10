/**
 * 通用工具类
 */
export default class func {
  /**
   * 不为空
   * @param val
   * @returns {boolean}
   */
  static notEmpty(val) {
    return !this.isEmpty(val);
  }

  /**
   * 是否为定义
   * @param val
   * @returns {boolean}
   */
  static isUndefined(val) {
    return val === null || typeof val === 'undefined';
  }

  /**
   * 为空
   * @param val
   * @returns {boolean}
   */
  static isEmpty(val) {
    if (
      val === null ||
      typeof val === 'undefined' ||
      (typeof val === 'string' && val === '' && val !== 'undefined')
    ) {
      return true;
    }
    return false;
  }

  /**
   * 强转int型
   * @param val
   * @param defaultValue
   * @returns {number}
   */
  static toInt(val, defaultValue) {
    if (this.isEmpty(val)) {
      return defaultValue === undefined ? -1 : defaultValue;
    }
    const num = parseInt(val, 0);
    return Number.isNaN(num) ? (defaultValue === undefined ? -1 : defaultValue) : num;
  }

  /**
   * 转为数字型(转换失败则返回原值)
   * @param val
   */
  static toNumber(val) {
    if (this.isEmpty(val)) {
      return '';
    }
    const num = parseFloat(val);
    return Number.isNaN(num) ? val : num;
  }

  /**
   * Json强转为Form类型
   * @param obj
   * @returns {FormData}
   */
  static toFormData(obj) {
    const data = new FormData();
    Object.keys(obj).forEach(key => {
      data.append(key, Array.isArray(obj[key]) ? obj[key].join(',') : obj[key]);
    });
    return data;
  }

  /**
   * date类转为字符串格式
   * @param date
   * @param format
   * @returns {null}
   */
  static format(date, format = 'YYYY-MM-DD HH:mm:ss') {
    return date ? date.format(format) : null;
  }

  /**
   * data类格式化
   * @param timestamp
   * @returns {string}
   */
  static formatDateTime(timestamp) {
    return this.formatDate(new Date(timestamp));
  }

  /**
   * data类格式化
   * @param date
   * @returns {string}
   */
  static formatDate(date) {
    const pad = num => (num < 10 ? '0' + num : num);

    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1); // 月份从0开始，所以+1
    const day = pad(date.getDate());
    const hour = pad(date.getHours());
    const minute = pad(date.getMinutes());
    const second = pad(date.getSeconds());

    return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
  }

  /**
   * 格式化时区解决时间差问题
   * @param datetime
   * @returns {string}
   */
  static toLocalISOString(datetime) {
    let timezoneOffset = datetime.getTimezoneOffset() * 60000; // 获取当前时区与UTC的时间差（以毫秒为单位）
    let localDatetime = new Date(datetime - timezoneOffset); // 调整时间，得到当前时区时间
    return localDatetime.toISOString();
  }

  /**
   * 根据逗号联合
   * @param arr
   * @returns {string}
   */
  static join(arr) {
    return Array.isArray(arr) ? arr.join(',') : arr;
  }

  /**
   * 根据逗号分隔
   * @param str
   * @returns {string}
   */
  static split(str) {
    return str ? String(str).split(',') : '';
  }

  /**
   * 转换空字符串
   * @param str
   * @returns {string|*}
   */
  static toStr(str) {
    if (typeof str === 'undefined' || str === null) {
      return '';
    }
    return str;
  }

  /**
   * 判断是否为数组
   * @param param
   * @returns {boolean}
   */
  static isArrayAndNotEmpty(param) {
    return Array.isArray(param) && param.length > 0;
  }

  /**
   * 格式化URL
   * @param url
   * @returns {*|string}
   */
  static formatUrl(url) {
    if (!url) return url;
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    } else {
      return `http://${url}`;
    }
  }

  /**
   * bytes转换为kb单位
   * @param bytes
   * @returns {string}
   */
  static bytesToKB(bytes) {
    const kb = bytes / 1024;
    return kb.toFixed(2);
  }

  /**
   * json数组转换成key value字符串
   * @param jsonArray "[{enumKey: 'key', enumValue: 'value'}]"
   * @returns {*}
   */
  static jsonArrayToKeyValue(jsonArray) {
    if (this.isEmpty(jsonArray)) {
      return '';
    }
    return jsonArray.map(item => `${item.enumKey}:${item.enumValue}`).join(';');
  }

  /**
   * key value字符串转换成json数组
   * @param keyValue key:value;key:value
   * @returns {*[]}
   */
  static keyValueToJsonArray(keyValue) {
    if (this.isEmpty(keyValue)) {
      return [];
    }
    return keyValue.split(';').map((kv, index) => {
      const [key, value] = kv.split(':');
      return {
        id: index,
        enumKey: key,
        enumValue: value,
      };
    });
  }

  /**
   * 检查字符串str中是否包含子字符串val
   * @param {string} str 要检查的字符串
   * @param {string} val 要查找的子字符串
   * @return {boolean} 如果str包含val则返回true，否则返回false
   */
  static contains(str, val) {
    // 检查str是否为字符串且不为空
    if (typeof str === 'string' && str.length > 0) {
      return str.includes(val);
    }
    return false;
  }

  /**
   * 截取字符串
   * @param str 字符串
   * @param len 截取长度
   * @returns {*|string}
   */
  static truncateString(str, len = 20) {
    if (str.length > len) {
      return str.slice(0, len) + '...';
    }
    return str;
  }

  /**
   * 驼峰转下划线
   * @param str
   * @returns {*}
   */
  static camelCaseString(str) {
    return str.replace(/_([a-z])/g, g => g[1].toUpperCase());
  }

  /**
   * 生成随机字符串
   * @param length 长度
   * @returns {string}
   */
  static strGenerate(length) {
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    const maxLength = 256;
    if (length > maxLength) {
      throw new Error(`长度最大值不能超过 ${maxLength}`);
    }

    return Array.from({ length }, () =>
      characters.charAt(Math.floor(Math.random() * characters.length))
    ).join('');
  }

  /**
   * 生成UUID
   * @returns {string}
   */
  static generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
      const r = (Math.random() * 16) | 0,
        v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }

  /**
   * 过滤空对象
   * @param obj
   * @returns {Object}
   */
  static filterEmptyObject(obj) {
    return Object.fromEntries(
      Object.entries(obj).filter(
        ([, value]) => value !== '' && value !== null && value !== undefined
      )
    );
  }

  /**
   * 获取用户租户ID
   * @param userInfo
   * @returns {string}
   */
  static getUserTenantId(userInfo) {
    if (userInfo && userInfo.tenant_id) {
      return userInfo.tenant_id;
    } else if (userInfo && userInfo.tenantId) {
      return userInfo.tenantId;
    }
  }
}
