import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

// Import existing static copy for public models only, never account-only resources.
const [catalogPath, siteRoot] = process.argv.slice(2)
if (!catalogPath || !siteRoot) {
  throw new Error('Usage: node tools/i18n/sync-model-hub-translations.mjs <public-catalog.json> <VLStreamSite-root>')
}
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const catalog = JSON.parse(readFileSync(catalogPath, 'utf8'))
const fields = ['name', 'description', 'category', 'model_type']
const texts = [...new Set([
  ...catalog.models.flatMap(model => fields.map(field => model[field])),
  ...catalog.categories.map(item => item.category)
])].filter(value => typeof value === 'string' && /\p{Script=Han}/u.test(value)).sort()
const output = resolve(root, 'VLStream-Web/VLStream-ui/src/i18n/modelHub')
const bundles = ['en', 'es', 'fr', 'de', 'pt', 'ru', 'ar', 'ja', 'ko', 'id', 'tr'].map(locale => {
  const source = JSON.parse(readFileSync(resolve(siteRoot, locale === 'en'
    ? 'src/lang/resource-en.json' : `src/lang/developer/${locale}.json`), 'utf8'))
  const dictionary = locale === 'en' ? source : source.resources
  const missing = texts.filter(text => !Object.hasOwn(dictionary, text) || !dictionary[text])
  if (missing.length) throw new Error(`${locale}: ${missing.length} public model translations missing`)
  return [locale, Object.fromEntries(texts.map(text => [text, dictionary[text]]))]
})
mkdirSync(output, { recursive: true })
for (const [locale, dictionary] of bundles) {
  writeFileSync(resolve(output, `${locale}.json`), `${JSON.stringify(dictionary, null, 2)}\n`)
}
console.log(`Synced ${texts.length} public model texts in ${bundles.length} locales`)
