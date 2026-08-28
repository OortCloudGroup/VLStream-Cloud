/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

/**
 *
 */
export default class func {
  /**
   * is empty
   * @param val
   * @returns {boolean}
   */
  static notEmpty(val) {
    return !this.isEmpty(val);
  }

  /**
   * whether to
   * @param val
   * @returns {boolean}
   */
  static isUndefined(val) {
    return val === null || typeof val === 'undefined';
  }

  /**
   * is empty
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
   * int
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
   * to (Convert failed value )
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
   * Json to Form
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
   * date to
   * @param date
   * @param format
   * @returns {null}
   */
  static format(date, format = 'YYYY-MM-DD HH:mm:ss') {
    return date ? date.format(format) : null;
  }

  /**
   * data Format
   * @param timestamp
   * @returns {string}
   */
  static formatDateTime(timestamp) {
    return this.formatDate(new Date(timestamp));
  }

  /**
   * data Format
   * @param date
   * @returns {string}
   */
  static formatDate(date) {
    const pad = num => (num < 10 ? '0' + num : num);

    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1); // from 0start, +1
    const day = pad(date.getDate());
    const hour = pad(date.getHours());
    const minute = pad(date.getMinutes());
    const second = pad(date.getSeconds());

    return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
  }

  /**
   * Format
   * @param datetime
   * @returns {string}
   */
  static toLocalISOString(datetime) {
    let timezoneOffset = datetime.getTimezoneOffset() * 60000; // Get current and UTC ( to )
    let localDatetime = new Date(datetime - timezoneOffset); // , current
    return localDatetime.toISOString();
  }

  /**
   *
   * @param arr
   * @returns {string}
   */
  static join(arr) {
    return Array.isArray(arr) ? arr.join(',') : arr;
  }

  /**
   *
   * @param str
   * @returns {string}
   */
  static split(str) {
    return str ? String(str).split(',') : '';
  }

  /**
   * Convert null / empty
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
   * Check whether to array
   * @param param
   * @returns {boolean}
   */
  static isArrayAndNotEmpty(param) {
    return Array.isArray(param) && param.length > 0;
  }

  /**
   * Format URL
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
   * bytesConvert to kb
   * @param bytes
   * @returns {string}
   */
  static bytesToKB(bytes) {
    const kb = bytes / 1024;
    return kb.toFixed(2);
  }

  /**
   * jsonarrayConvert key value
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
   * key value Convert jsonarray
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
   * str in whether sub val
   * @param {string} str need to
   * @param {string} val need to find sub
   * @return {boolean} if str val true, false
   */
  static contains(str, val) {
    // strwhether to is empty
    if (typeof str === 'string' && str.length > 0) {
      return str.includes(val);
    }
    return false;
  }

  /**
   *
   * @param str
   * @param len
   * @returns {*|string}
   */
  static truncateString(str, len = 20) {
    if (str.length > len) {
      return str.slice(0, len) + '...';
    }
    return str;
  }

  /**
   *
   * @param str
   * @returns {*}
   */
  static camelCaseString(str) {
    return str.replace(/_([a-z])/g, g => g[1].toUpperCase());
  }

  /**
   * Generate
   * @param length
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
   * Generate UUID
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
   * null / empty object
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
   * Get usertenant ID
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
