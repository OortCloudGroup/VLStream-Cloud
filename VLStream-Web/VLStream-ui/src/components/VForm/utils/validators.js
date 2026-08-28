/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

import { isNull } from './util'

export const getRegExp = function(validatorName) {
  const commonRegExp = {
    number: '/^[-]?\\d+(\\.\\d+)?$/',
    letter: '/^[A-Za-z]+$/',
    letterAndNumber: '/^[A-Za-z0-9]+$/',
    mobilePhone: '/^[1][3-9][0-9]{9}$/',
    letterStartNumberIncluded: '/^[A-Za-z]+[A-Za-z\\d]*$/',
    noChinese: '/^[^\u4e00-\u9fa5]+$/',
    chinese: '/^[\u4e00-\u9fa5]+$/',
    email: '/^([-_A-Za-z0-9.]+)@([_A-Za-z0-9]+\\.)+[A-Za-z0-9]{2,3}$/',
    url: '/^([hH][tT]{2}[pP]:\\/\\/|[hH][tT]{2}[pP][sS]:\\/\\/)(([A-Za-z0-9-~]+)\\.)+([A-Za-z0-9-~\\/])+$/'
  }

  return commonRegExp[validatorName]
}

const validateFn = function(validatorName, rule, value, callback, defaultErrorMsg) {
  // null / empty value Validate
  if (isNull(value) || (value.length <= 0)) {
    callback()
    return
  }

  // eslint-disable-next-line no-eval
  const reg = eval(getRegExp(validatorName))

  if (!reg.test(value)) {
    let errTxt = rule.errorMsg || defaultErrorMsg
    callback(new Error(errTxt))
  } else {
    callback()
  }
}

const FormValidators = {

  /*  */
  number(rule, value, callback) {
    validateFn('number', rule, value, callback, '[' + rule.label + ']包含非数字字符')
  },

  /*  */
  letter(rule, value, callback) {
    validateFn('letter', rule, value, callback, '[' + rule.label + ']包含非字母字符')
  },

  /* and */
  letterAndNumber(rule, value, callback) {
    validateFn('letterAndNumber', rule, value, callback, '[' + rule.label + ']只能输入字母或数字')
  },

  /*  */
  mobilePhone(rule, value, callback) {
    validateFn('mobilePhone', rule, value, callback, '[' + rule.label + ']手机号码格式有误')
  },

  /* null / empty */
  noBlankStart() {
    // not
  },

  /* null / empty */
  noBlankEnd() {
    // not
  },

  /* , */
  letterStartNumberIncluded(rule, value, callback) {
    validateFn('letterStartNumberIncluded', rule, value, callback, '[' + rule.label + ']必须以字母开头，可包含数字')
  },

  /* in */
  noChinese(rule, value, callback) {
    validateFn('noChinese', rule, value, callback, '[' + rule.label + ']不可输入中文字符')
  },

  /* in */
  chinese(rule, value, callback) {
    validateFn('chinese', rule, value, callback, '[' + rule.label + ']只能输入中文字符')
  },

  /* sub */
  email(rule, value, callback) {
    validateFn('email', rule, value, callback, '[' + rule.label + ']邮箱格式有误')
  },

  /* URL */
  url(rule, value, callback) {
    validateFn('url', rule, value, callback, '[' + rule.label + ']URL格式有误')
  },

  /*
  test(rule, value, callback, errorMsg) {
    // null / empty value Validate
    if (isNull(value) || (value.length <= 0)) {
      callback()
      return
    }

    if (value < 100) {
      callback(new Error('[' + rule.label + ']不能小于100'))
    } else {
      callback()
    }
  },
  */

  regExp(rule, value, callback) {
    // null / empty value Validate
    if (isNull(value) || (value.length <= 0)) {
      callback()
      return
    }

    // eslint-disable-next-line no-eval
    const pattern = eval(rule.regExp)
    if (!pattern.test(value)) {
      let errTxt = rule.errorMsg || '[' + rule.label + ']invalid value'
      callback(new Error(errTxt))
    } else {
      callback()
    }
  }

}

export default FormValidators
