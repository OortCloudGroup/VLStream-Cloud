import { isNull, evalFn } from './util'

export const getRegExp = function(validatorName) {
  const commonRegExp = {
    chinese: '/^[\u4e00-\u9fa5]+$/',
    email: '/^([-_A-Za-z0-9.]+)@([_A-Za-z0-9]+\\.)+[A-Za-z0-9]{2,3}$/',
    letter: '/^[A-Za-z]+$/',
    letterAndNumber: '/^[A-Za-z0-9]+$/',
    letterStartNumberIncluded: '/^[A-Za-z]+[A-Za-z\\d]*$/',
    mobilePhone: '/^[1][3-9][0-9]{9}$/',
    noChinese: '/^[^\u4e00-\u9fa5]+$/',
    number: '/^[-]?\\d+(\\.\\d+)?$/',
    url: '/^([hH][tT]{2}[pP]:\\/\\/|[hH][tT]{2}[pP][sS]:\\/\\/)(([A-Za-z0-9-~]+)\\.)+([A-Za-z0-9-~\\/])+$/'
  }

  return commonRegExp[validatorName]
}

const validateFn = function(validatorName, rule, value, callback, defaultErrorMsg) {
  // //空值不校验
  // if (isNull(value) || (value.length <= 0)) {
  //   callback()
  //   return
  // }
  //
  // const reg = eval(getRegExp(validatorName))
  //
  // if (!reg.test(value)) {
  //   let errTxt = rule.errorMsg || defaultErrorMsg
  //   callback(new Error(errTxt))
  // } else {
  //   callback()
  // }

  if (isNull(value) || value.length <= 0) {
    return true
  }

  rule.message = rule.message || defaultErrorMsg
  const reg = evalFn(getRegExp(validatorName))
  return reg.test(value)
}

const FormValidators = {
  /* 必须中文输入 */
  chinese(value, rule, callback) {
    return validateFn('chinese', rule, value, callback, '[' + rule.label + ']只能输入中文字符')
  },

  /* 电子邮箱 */
  email(value, rule, callback) {
    return validateFn('email', rule, value, callback, '[' + rule.label + ']邮箱格式有误')
  },

  /* 字母 */
  letter(value, rule, callback) {
    return validateFn('letter', rule, value, callback, '[' + rule.label + ']包含非字母字符')
  },

  /* 字母和数字 */
  letterAndNumber(value, rule, callback) {
    return validateFn('letterAndNumber', rule, value, callback, '[' + rule.label + ']只能输入字母或数字')
  },

  /* 字母开头，仅可包含数字 */
  letterStartNumberIncluded(value, rule, callback) {
    return validateFn('letterStartNumberIncluded', rule, value, callback, '[' + rule.label + ']必须以字母开头，可包含数字')
  },

  /* 手机号码 */
  mobilePhone(value, rule, callback) {
    return validateFn('mobilePhone', rule, value, callback, '[' + rule.label + ']手机号码格式有误')
  },

  /* 禁止空白字符结尾 */
  noBlankEnd() {
    // 暂未实现
  },

  /* 禁止空白字符开头 */
  noBlankStart() {
    // 暂未实现
  },

  /* 禁止中文输入 */
  noChinese(value, rule, callback) {
    return validateFn('noChinese', rule, value, callback, '[' + rule.label + ']不可输入中文字符')
  },
  /* 数字 */
  number(value, rule, callback) {
    return validateFn('number', rule, value, callback, '[' + rule.label + ']包含非数字字符')
  },

  /* 测试
  test(rule, value, callback, errorMsg) {
    //空值不校验
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
  regExp(value, rule) {
    // 空值不校验
    if (isNull(value) || value.length <= 0) {
      rule.message = rule.message || '[' + rule.label + ']invalid value'
      return true
    }

    if (rule.regExp.length > 0 && rule.regExp[0] !== '/') rule.regExp = '/' + rule.regExp
    if (rule.regExp.length > 0 && rule.regExp[rule.regExp.length - 1] !== '/') rule.regExp = rule.regExp + '/'

    const pattern = evalFn(rule.regExp)
    return pattern.test(value)
  },

  /* URL网址 */

  url(value, rule, callback) {
    return validateFn('url', rule, value, callback, '[' + rule.label + ']URL格式有误')
  }
}

export default FormValidators
