import { currentLocale, translatePhrase } from './index'

const translatedAttributes = ['placeholder', 'title', 'aria-label', 'alt']
const originalText = new WeakMap()
const appliedText = new WeakMap()
const originalAttributes = new WeakMap()
let observer
let scheduled = false

const translateTextNode = node => {
  if (!originalText.has(node) || (appliedText.has(node) && appliedText.get(node) !== node.nodeValue)) {
    originalText.set(node, node.nodeValue)
  }
  const source = originalText.get(node)
  const trimmed = source.trim()
  if (!trimmed) return
  const translated = translatePhrase(trimmed)
  if (translated === trimmed && currentLocale.value !== 'zh-CN') return
  const start = source.match(/^\s*/)?.[0] || ''
  const end = source.match(/\s*$/)?.[0] || ''
  const nextValue = `${start}${translated}${end}`
  appliedText.set(node, nextValue)
  if (node.nodeValue !== nextValue) node.nodeValue = nextValue
}

const translateElement = element => {
  if (!(element instanceof Element) || element.closest('[data-i18n-ignore]')) return
  let attributes = originalAttributes.get(element)
  if (!attributes) {
    attributes = {}
    originalAttributes.set(element, attributes)
  }
  translatedAttributes.forEach(name => {
    if (!element.hasAttribute(name)) return
    if (!(name in attributes)) attributes[name] = element.getAttribute(name)
    element.setAttribute(name, translatePhrase(attributes[name]))
  })
}

const translateTree = root => {
  if (!root) return
  if (root.nodeType === Node.TEXT_NODE) {
    translateTextNode(root)
    return
  }
  if (root.nodeType !== Node.ELEMENT_NODE && root.nodeType !== Node.DOCUMENT_FRAGMENT_NODE) return
  if (root.nodeType === Node.ELEMENT_NODE) translateElement(root)
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT)
  let node = walker.nextNode()
  while (node) {
    if (node.nodeType === Node.TEXT_NODE) translateTextNode(node)
    else translateElement(node)
    node = walker.nextNode()
  }
}

const scheduleTranslation = () => {
  if (scheduled) return
  scheduled = true
  requestAnimationFrame(() => {
    scheduled = false
    translateTree(document.body)
  })
}

export const installLegacyDomI18n = () => {
  scheduleTranslation()
  observer = new MutationObserver(records => {
    records.forEach(record => {
      if (record.type === 'characterData') translateTextNode(record.target)
      record.addedNodes.forEach(translateTree)
    })
  })
  const root = document.body
  if (root) observer.observe(root, { childList: true, subtree: true, characterData: true })
  window.addEventListener('vlstream:locale-changed', scheduleTranslation)
}

export const uninstallLegacyDomI18n = () => {
  observer?.disconnect()
  observer = undefined
  window.removeEventListener('vlstream:locale-changed', scheduleTranslation)
}
