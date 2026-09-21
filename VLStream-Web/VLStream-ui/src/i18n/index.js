import { computed, watch } from 'vue'
import { createI18n } from 'vue-i18n'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import es from 'element-plus/es/locale/lang/es'
import ar from 'element-plus/es/locale/lang/ar'
import de from 'element-plus/es/locale/lang/de'
import fr from 'element-plus/es/locale/lang/fr'
import ja from 'element-plus/es/locale/lang/ja'
import ptBr from 'element-plus/es/locale/lang/pt-br'
import ru from 'element-plus/es/locale/lang/ru'
import ko from 'element-plus/es/locale/lang/ko'
import id from 'element-plus/es/locale/lang/id'
import tr from 'element-plus/es/locale/lang/tr'
import { localeOptions, messages, phraseCatalog } from './catalog'
import { tableHeaderEnglish } from './tableHeaders'

export const DEFAULT_LOCALE = 'zh-CN'
export const STORAGE_KEY = 'language'

const aliases = {
  zh: 'zh-CN', 'zh-cn': 'zh-CN', 'zh_cn': 'zh-CN',
  en: 'en-US', 'en-us': 'en-US', 'en_us': 'en-US',
  es: 'es-MX', 'es-mx': 'es-MX', 'es_mx': 'es-MX',
  ar: 'ar',
  de: 'de-DE', 'de-de': 'de-DE', 'de_de': 'de-DE',
  fr: 'fr-FR', 'fr-fr': 'fr-FR', 'fr_fr': 'fr-FR',
  ja: 'ja-JP', 'ja-jp': 'ja-JP', 'ja_jp': 'ja-JP',
  pt: 'pt-BR', 'pt-br': 'pt-BR', 'pt_br': 'pt-BR',
  ru: 'ru-RU', 'ru-ru': 'ru-RU', 'ru_ru': 'ru-RU',
  ko: 'ko-KR', 'ko-kr': 'ko-KR', 'ko_kr': 'ko-KR',
  id: 'id-ID', 'id-id': 'id-ID', 'id_id': 'id-ID',
  tr: 'tr-TR', 'tr-tr': 'tr-TR', 'tr_tr': 'tr-TR'
}

export const normalizeLocale = value => aliases[String(value || '').toLowerCase()] || DEFAULT_LOCALE

const storedLocale = normalizeLocale(localStorage.getItem(STORAGE_KEY) || navigator.language)

export const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: storedLocale,
  fallbackLocale: DEFAULT_LOCALE,
  messages
})

const elementLocales = {
  'zh-CN': zhCn,
  'en-US': en,
  'es-MX': es,
  ar,
  'de-DE': de,
  'fr-FR': fr,
  'ja-JP': ja,
  'pt-BR': ptBr,
  'ru-RU': ru,
  'ko-KR': ko,
  'id-ID': id,
  'tr-TR': tr
}

export const currentLocale = i18n.global.locale
export const elementLocale = computed(() => elementLocales[currentLocale.value] || zhCn)
export const currentLocaleOption = computed(() => localeOptions.find(item => item.code === currentLocale.value) || localeOptions[0])

export const translatePhrase = (source, locale = currentLocale.value) => {
  if (typeof source !== 'string' || !source) return source
  const normalizedLocale = normalizeLocale(locale)
  return phraseCatalog[normalizedLocale]?.[source]
    || (normalizedLocale !== DEFAULT_LOCALE ? tableHeaderEnglish[source] : undefined)
    || source
}

export const formatDateTime = (value, options = {}) => {
  if (value === null || value === undefined || value === '') return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat(currentLocale.value, {
    dateStyle: 'medium',
    timeStyle: 'medium',
    ...options
  }).format(date)
}

export const formatNumber = (value, options = {}) => {
  const number = Number(value)
  return Number.isFinite(number) ? new Intl.NumberFormat(currentLocale.value, options).format(number) : String(value ?? '')
}

const applyDocumentLocale = locale => {
  const option = localeOptions.find(item => item.code === locale) || localeOptions[0]
  document.documentElement.lang = option.code
  document.documentElement.dir = option.dir
  document.documentElement.dataset.locale = option.code
  document.body?.classList.toggle('is-rtl', option.dir === 'rtl')
}

export const setLocale = value => {
  const locale = normalizeLocale(value)
  currentLocale.value = locale
  localStorage.setItem(STORAGE_KEY, locale)
  applyDocumentLocale(locale)
  window.dispatchEvent(new CustomEvent('vlstream:locale-changed', { detail: { locale } }))
  return locale
}

applyDocumentLocale(storedLocale)
watch(currentLocale, applyDocumentLocale, { flush: 'post' })

export { localeOptions, phraseCatalog }
