import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import { phraseCatalog } from '../../VLStream-Web/VLStream-ui/src/i18n/catalog.js'
import { tableHeaderEnglish } from '../../VLStream-Web/VLStream-ui/src/i18n/tableHeaders.js'

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const frontendRoot = path.join(repositoryRoot, 'VLStream-Web/VLStream-ui')
const require = createRequire(path.join(frontendRoot, 'package.json'))
const { parse: parseSfc } = require('@vue/compiler-sfc')
const { parse: parseTemplate } = require('@vue/compiler-dom')
const babel = require('@babel/parser')
const han = /[\u3400-\u9fff]/
const uiAttributes = /^(label|title|placeholder|description|content|empty-text|active-text|inactive-text|aria-label|alt|tip|keyword-empty-tips)$/
const phrases = new Set()
const missing = new Map()
const match = process.argv.find(arg => arg.startsWith('--match='))?.slice(8).toLowerCase()
let fileCount = 0

function check(value, file) {
  if (!han.test(value)) return
  phrases.add(value)
  const english = phraseCatalog['en-US'][value] || tableHeaderEnglish[value]
  if (!english || han.test(english)) {
    const files = missing.get(value) || new Set()
    files.add(path.relative(repositoryRoot, file))
    missing.set(value, files)
  }
}

function visit(node, callback) {
  if (!node || typeof node !== 'object') return
  callback(node)
  for (const [key, value] of Object.entries(node)) {
    if (['loc', 'extra', 'comments'].includes(key)) continue
    if (Array.isArray(value)) value.forEach(child => visit(child, callback))
    else if (value && typeof value === 'object') visit(value, callback)
  }
}

function checkExpression(source, file) {
  const ast = babel.parseExpression(source, { plugins: ['typescript'] })
  visit(ast, node => {
    if (node.type === 'StringLiteral') check(node.value, file)
    if (node.type === 'TemplateLiteral') {
      const phrase = node.quasis.map((part, index) => (part.value.cooked || '') + (index < node.expressions.length ? `{value${index}}` : '')).join('')
      check(phrase, file)
    }
  })
}

function checkReturnedText(node, file) {
  visit(node, child => {
    if (child.type === 'StringLiteral') check(child.value, file)
    if (child.type === 'TemplateLiteral') {
      const phrase = child.quasis.map((part, index) => (part.value.cooked || '') + (index < child.expressions.length ? `{value${index}}` : '')).join('')
      check(phrase, file)
    }
  })
}

function inspect(file) {
  if (match && !path.relative(repositoryRoot, file).toLowerCase().includes(match)) return
  const { descriptor, errors } = parseSfc(fs.readFileSync(file, 'utf8'))
  if (errors.length) throw new Error(`${file}: ${errors.join(', ')}`)
  fileCount++
  function walk(node) {
    if (['pre', 'code', 'script', 'style', 'textarea'].includes(node.tag) || (node.props || []).some(prop => prop.name === 'data-i18n-ignore')) return
    if (node.type === 2) check(node.content.replace(/\s+/g, ' ').trim(), file)
    if (node.type === 5) checkExpression(node.content.content, file)
    if (/^el-(radio|checkbox)(-button)?$/.test(node.tag || '')) {
      const props = node.props || []
      const localizedLabel = props.some(prop => prop.type === 7 && prop.arg?.content === 'label' && /\$tp\(/.test(prop.exp?.content || ''))
      const hasValue = props.some(prop => prop.type === 6 && prop.name === 'value' || prop.type === 7 && prop.arg?.content === 'value')
      if (localizedLabel && !hasValue) throw new Error(`${file}:${node.loc.start.line}: translated radio/checkbox labels require an independent value`)
    }
    for (const prop of node.props || []) {
      if (prop.type === 6 && uiAttributes.test(prop.name) && prop.value) check(prop.value.content.replace(/\s+/g, ' ').trim(), file)
      if (prop.type === 7 && prop.name === 'bind' && uiAttributes.test(prop.arg?.content || '') && prop.exp) checkExpression(prop.exp.content, file)
    }
    for (const child of node.children || []) walk(child)
  }
  if (descriptor.template) walk(parseTemplate(descriptor.template.content))
  const script = descriptor.scriptSetup || descriptor.script
  if (script) visit(babel.parse(script.content, { sourceType: 'module', plugins: ['typescript'] }), node => {
    if (node.type === 'CallExpression' && node.callee?.type === 'MemberExpression' && ['ElMessage', 'ElMessageBox'].includes(node.callee.object?.name)) {
      node.arguments.filter(arg => arg.type !== 'SpreadElement').forEach(arg => checkExpression(script.content.slice(arg.start, arg.end), file))
    }
    if (node.type === 'CallExpression' && ['uiText', 'translatePhrase', 'tp'].includes(node.callee?.name) && node.arguments[0]?.type === 'StringLiteral') check(node.arguments[0].value, file)
    if (node.type === 'ObjectProperty' && /^(label|title|placeholder|description|message|text|content|emptyText)$/.test(node.key.name || node.key.value || '') && node.value.type === 'StringLiteral') check(node.value.value, file)
    if (node.type === 'VariableDeclarator' && node.init?.type === 'ObjectExpression' && /names|states|units|labels|descriptions|types/i.test(node.id.name || '')) {
      node.init.properties.forEach(prop => { if (prop.value?.type === 'StringLiteral') check(prop.value.value, file) })
    }
    if (node.type === 'NewExpression' && node.callee?.name === 'Error' && node.arguments[0]?.type === 'StringLiteral') check(node.arguments[0].value, file)
    if (node.type === 'ReturnStatement') checkReturnedText(node.argument, file)
    if (node.type === 'ArrowFunctionExpression' && node.body.type !== 'BlockStatement') checkReturnedText(node.body, file)
  })
}

function scan(directory) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const file = path.join(directory, entry.name)
    if (entry.isDirectory()) scan(file)
    else if (entry.isFile() && file.endsWith('.vue')) inspect(file)
  }
}

scan(path.join(frontendRoot, 'src'))
console.log(`Checked ${fileCount} complete Vue files and ${phrases.size} interface phrases, including nested templates, dynamic messages, display maps, and validation text.`)
console.log(`Missing English translations: ${missing.size}`)
for (const [phrase, files] of missing) console.log(`${JSON.stringify(phrase)}\n  ${[...files].join('\n  ')}`)
if (process.argv.includes('--strict') && missing.size) process.exitCode = 1
