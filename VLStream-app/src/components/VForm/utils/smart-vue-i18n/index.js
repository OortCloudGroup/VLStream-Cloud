import { reactive } from 'vue'
import { get } from './utils'

let locale = reactive({
  lang: localStorage.getItem('vm_form_locale') || 'zh-CN'
})

export function createI18n(options) {
  return {
    $mt(path, ...args) {
      const message = get(this.messages[locale.lang], path)
      return typeof message === 'function' ? message(...args) : message !== null ? message : path
    },

    $mt2(path, path2) {
      let messages = this.messages[locale.lang]
      const message = get(messages, path)
      return message !== null ? message : get(messages, path2)
    },

    messages: options.messages,

    setLang(lang) {
      locale.lang = lang
    }
  }
}
