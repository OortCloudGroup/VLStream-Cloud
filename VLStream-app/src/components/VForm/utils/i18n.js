import { createI18n } from './smart-vue-i18n/index'

// import enLocaleElement from 'element-plus/es/locale/lang/en';
// import zhLocaleElement from 'element-plus/es/locale/lang/zh-cn';
// import locale from "element-plus/es/locale"

import enLocale from '~@/lang/en-US'
import zhLocale from '~@/lang/zh-CN'
import enLocale_render from '~@/lang/en-US_render'
import zhLocale_render from '~@/lang/zh-CN_render'
import enLocale_extension from '~@/lang/en-US_extension'
import zhLocale_extension from '~@/lang/zh-CN_extension'

const langResources = {
  'en-US': {
    something: {
      // ...
    },
    // ...enLocaleElement,
    ...enLocale,
    ...enLocale_render,
    ...enLocale_extension
  },

  'zh-CN': {
    something: {
      // ...
    },
    // ...zhLocaleElement,
    ...zhLocale,
    ...zhLocale_render,
    ...zhLocale_extension
  }
}

const si18n = createI18n({
  locale: localStorage.getItem('vm_form_locale') || 'zh-CN',
  messages: langResources
})

export const changeLocale = function(langName) {
  si18n.setLang(langName)
  localStorage.setItem('vm_form_locale', langName)
}

export const translate = function(key) {
  return si18n.$mt(key)
}

export const installI18n = () => {
  //
}

export function useChangeLocale(langName) {
  si18n.setLang(langName)
  localStorage.setItem('vm_form_locale', langName)
}

export function useI18n() {
  const i18nt = key => {
    return si18n.$mt(key)
  }

  /* 如果key1不存在，则查找key2 */
  const i18n2t = (key1, key2) => {
    return si18n.$mt2(key1, key2)
  }

  return { i18n2t, i18nt }
}
