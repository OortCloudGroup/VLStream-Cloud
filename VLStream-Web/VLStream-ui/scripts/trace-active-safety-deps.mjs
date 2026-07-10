/**
 * Trace all imports reachable from active-safety (主动安全) entry points.
 * Finds unused files in apaas-copied directories.
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const PROJECT_ROOT = path.resolve(__dirname, '..')
const SRC = path.join(PROJECT_ROOT, 'src')
const IS_WIN = process.platform === 'win32'

const ALIASES = [
  { find: '@/pages/events/views', replacement: path.join(SRC, 'views/events') },
  { find: '@', replacement: SRC },
  { find: '~@', replacement: path.join(SRC, 'components/VForm') },
  { find: '@im', replacement: path.join(SRC, 'components/im') },
]

const EXTENSIONS = ['.ts', '.tsx', '.js', '.jsx', '.vue', '.json', '.scss', '.sass', '.css', '.mjs', '.cjs']
const ASSET_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.gif', '.webp', '.svg', '.ico', '.woff', '.woff2', '.ttf', '.eot', '.mp3', '.mp4', '.wav']

const APAAS_SCAN_DIRS = [
  'pages/processui',
  'pages/task_center',
  'pages/Intelligent_approval',
  'pages/messageui',
  'api/smartCity',
  'api/processui',
  'api/unifiedUsert',
  'api/login',
  'api/sso_apaas',
  'api/aiApaas',
  'store',
  'config',
  'directive',
  'hooks',
  'locales',
  'assets',
  'components',
  'utils',
]

// Vite svg-icons-register dirs (from vite.config.js) - svgs referenced by name at runtime
const VITE_SVG_DIRS = [
  'assets/img/svg',
  'assets/img/processui/svgs/VForm/svg',
  'assets/img/processui/svgs',
  'assets/img/unifi/svgs',
  'assets/img/message/svgs',
]

function normPath(p) {
  const n = path.normalize(p)
  return IS_WIN ? n.toLowerCase() : n
}

function resolveAlias(specifier) {
  for (const { find, replacement } of ALIASES) {
    if (specifier === find || specifier.startsWith(find + '/')) {
      return specifier.replace(find, replacement)
    }
  }
  return specifier
}

function resolveFile(specifier, fromFile) {
  if (!specifier || specifier.startsWith('virtual:') || specifier.startsWith('element-plus') ||
      specifier.startsWith('@element-plus') || specifier.startsWith('@smallwei') ||
      specifier.startsWith('vue') || specifier.startsWith('pinia') ||
      specifier.startsWith('axios') || specifier.startsWith('dayjs') ||
      specifier.startsWith('lodash') || specifier.startsWith('echarts') ||
      (/^[a-z@][\w.-]*$/i.test(specifier) && !specifier.startsWith('.') && !specifier.startsWith('@/') && !specifier.startsWith('~@') && !specifier.startsWith('@im'))) {
    return null
  }

  let resolved = resolveAlias(specifier)

  if (resolved.startsWith('.')) {
    resolved = path.resolve(path.dirname(fromFile), resolved)
  } else if (resolved.startsWith('/src/')) {
    resolved = path.join(PROJECT_ROOT, resolved.slice(1))
  } else if (!path.isAbsolute(resolved)) {
    return null
  }

  resolved = path.normalize(resolved)

  if (fs.existsSync(resolved) && fs.statSync(resolved).isFile()) {
    return resolved
  }

  for (const ext of EXTENSIONS) {
    const withExt = resolved + ext
    if (fs.existsSync(withExt)) return withExt
  }

  if (fs.existsSync(resolved) && fs.statSync(resolved).isDirectory()) {
    for (const ext of EXTENSIONS) {
      const indexFile = path.join(resolved, 'index' + ext)
      if (fs.existsSync(indexFile)) return indexFile
    }
  } else {
    for (const ext of EXTENSIONS) {
      const indexFile = path.join(resolved, 'index' + ext)
      if (fs.existsSync(indexFile)) return indexFile
    }
  }

  for (const ext of ASSET_EXTENSIONS) {
    const withExt = resolved + ext
    if (fs.existsSync(withExt)) return withExt
  }
  if (fs.existsSync(resolved)) return resolved

  return null
}

function expandGlobPattern(pattern, fromFile) {
  let globPattern = resolveAlias(pattern)
  if (globPattern.startsWith('/src/')) {
    globPattern = globPattern.slice('/src/'.length)
  }

  let baseDir
  let filePattern
  if (globPattern.startsWith('.')) {
    baseDir = path.dirname(fromFile)
    filePattern = globPattern.replace(/^\.\//, '')
  } else if (path.isAbsolute(globPattern)) {
    baseDir = path.dirname(globPattern)
    filePattern = path.basename(globPattern)
  } else {
    baseDir = SRC
    filePattern = globPattern
  }

  // Vite-style ** globs: match recursively by extension when pattern is like **/*.vue
  const extMatch = filePattern.match(/^\*\*\/\*\.(\w+)$/)
  if (extMatch) {
    const ext = '.' + extMatch[1]
    const results = []
    function walkAll(d) {
      if (!fs.existsSync(d)) return
      for (const name of fs.readdirSync(d)) {
        const full = path.join(d, name)
        if (fs.statSync(full).isDirectory()) walkAll(full)
        else if (full.endsWith(ext)) results.push(full)
      }
    }
    walkAll(baseDir)
    return results
  }

  const regexStr = '^' + filePattern
    .replace(/\./g, '\\.')
    .replace(/\*\*/g, '___DS___')
    .replace(/\*/g, '[^/]*')
    .replace(/___DS___/g, '.*')
    + '$'

  const regex = new RegExp(regexStr, 'i')
  const results = []
  const recursive = filePattern.includes('**')

  function walk(d) {
    if (!fs.existsSync(d)) return
    for (const name of fs.readdirSync(d)) {
      const full = path.join(d, name)
      const stat = fs.statSync(full)
      if (stat.isDirectory()) {
        if (recursive) walk(full)
      } else {
        const relFromDir = path.relative(baseDir, full).replace(/\\/g, '/')
        if (regex.test(relFromDir)) results.push(full)
      }
    }
  }

  walk(baseDir)
  return results
}

function stripComments(content) {
  // Only strip full-line // comments to avoid breaking strings like './**/*.vue'
  return content.replace(/^\s*\/\/.*$/gm, '')
}

function extractImports(content, filePath) {
  const imports = []
  content = stripComments(content)

  const importFromRe = /(?:import|export)\s+(?:type\s+)?(?:[\w*{}\s,\n]+\s+from\s+)?['"]([^'"]+)['"]/g
  let m
  while ((m = importFromRe.exec(content)) !== null) {
    imports.push(m[1])
  }

  const dynamicRe = /import\s*\(\s*['"]([^'"]+)['"]\s*\)/g
  while ((m = dynamicRe.exec(content)) !== null) {
    imports.push(m[1])
  }

  const requireRe = /require\s*\(\s*['"]([^'"]+)['"]\s*\)/g
  while ((m = requireRe.exec(content)) !== null) {
    imports.push(m[1])
  }

  const scssImportRe = /@(?:import|use)\s+(?:url\()?['"]?([^'";(\s]+)['"]?\)?/g
  while ((m = scssImportRe.exec(content)) !== null) {
    let p = m[1]
    if (p.startsWith('~')) p = p.slice(1)
    if (!p.startsWith('http') && !p.includes('node_modules')) {
      imports.push(p)
    }
  }

  const srcAttrRe = /(?:script|style)[^>]*\ssrc=['"]([^'"]+)['"]/g
  while ((m = srcAttrRe.exec(content)) !== null) {
    imports.push(m[1])
  }

  const urlRe = /url\s*\(\s*['"]?(\.\.?\/[^'")\s]+)['"]?\s*\)/g
  while ((m = urlRe.exec(content)) !== null) {
    imports.push(m[1])
  }

  // Vue template src/href with @/ alias (e.g. src="@/assets/img/foo.png")
  const vueAssetRe = /(?:src|href)=["']@\/([^"']+)["']/g
  while ((m = vueAssetRe.exec(content)) !== null) {
    imports.push('@/' + m[1])
  }

  // import.meta.glob patterns
  const globRe = /import\.meta\.glob\s*\(\s*['"]([^'"]+)['"]/g
  while ((m = globRe.exec(content)) !== null) {
    const globFiles = expandGlobPattern(m[1], filePath)
    imports.push(...globFiles)
  }

  return imports
}

function readFileContent(filePath) {
  try {
    return fs.readFileSync(filePath, 'utf8')
  } catch {
    return ''
  }
}

function getEntryPoints() {
  const entries = []

  function walkDir(dir) {
    if (!fs.existsSync(dir)) return
    for (const name of fs.readdirSync(dir)) {
      const full = path.join(dir, name)
      if (fs.statSync(full).isDirectory()) walkDir(full)
      else entries.push(full)
    }
  }
  walkDir(path.join(SRC, 'views/events'))

  const routerRoutes = [
    '@/views/events/page/eventManagement/secure.vue',
    '@/views/events/page/myWorkorder/myWorkorder.vue',
    '@/views/events/page/workOrderDesignManage/pendingWorkOrder.vue',
    '@/views/events/page/workOrderDesignManage/completedWorkOrder.vue',
    '@/views/events/page/workOrderDesignManage/workOrderClaim.vue',
    '@/views/events/page/eventManagement/sysSecure2.vue',
    '@/views/events/page/eventManagement/sysWorkOrderSetting.vue',
    '@/pages/task_center/views/page/workOrderDetailsStep.vue',
  ]
  for (const r of routerRoutes) {
    const resolved = resolveFile(r, SRC)
    if (resolved && !entries.some(e => normPath(e) === normPath(resolved))) entries.push(resolved)
  }

  const mainImports = [
    '@/assets/style/index.scss',
    '@/assets/style/common.scss',
    '@/components/aiIconRemark.vue',
    '@/components/TableSelf.vue',
    '@/components/buttonGroup.vue',
    '@/components/SearchHeightBox.vue',
    '@/components/exportExcelPdf.vue',
    '@/directive/index',
    '@/components/index.js',
    '@/components/index_vform.js',
  ]
  for (const imp of mainImports) {
    const resolved = resolveFile(imp, path.join(SRC, 'main.js'))
    if (resolved && !entries.some(e => normPath(e) === normPath(resolved))) entries.push(resolved)
  }

  return entries
}

function collectAllFilesUnder(relativeDirs) {
  const files = []
  for (const rel of relativeDirs) {
    const abs = path.join(SRC, rel)
    if (!fs.existsSync(abs)) continue
    function walk(d) {
      for (const name of fs.readdirSync(d)) {
        const full = path.join(d, name)
        const stat = fs.statSync(full)
        if (stat.isDirectory()) walk(full)
        else files.push(full)
      }
    }
    walk(abs)
  }
  return files
}

function traceDependencies(entryPoints) {
  const reachable = new Map() // normPath -> actual path
  const unresolved = []
  const queue = [...entryPoints]

  while (queue.length > 0) {
    const file = queue.shift()
    const key = normPath(file)
    if (reachable.has(key)) continue
    reachable.set(key, file)

    const content = readFileContent(file)
    const imports = extractImports(content, file)

    for (const spec of imports) {
      // Already resolved absolute path from glob expansion
      if (path.isAbsolute(spec) && fs.existsSync(spec)) {
        const k = normPath(spec)
        if (!reachable.has(k)) queue.push(spec)
        continue
      }
      const resolved = resolveFile(spec, file)
      if (resolved) {
        if (!reachable.has(normPath(resolved))) queue.push(resolved)
      } else if (spec.startsWith('.') || spec.startsWith('@/') || spec.startsWith('~@') ||
                 spec.startsWith('@/pages/events/views') || spec.startsWith('@im')) {
        unresolved.push({ from: file, spec })
      }
    }
  }

  return { reachable, unresolved }
}

function extractSvgIconNames(reachableFiles) {
  const names = new Set()
  for (const file of reachableFiles) {
    const content = readFileContent(file)
    const re = /oort-svg-icon[^>]*name=["']([^"']+)["']/gi
    let m
    while ((m = re.exec(content)) !== null) names.add(m[1])
    const re2 = /<oort-svg-icon[^>]*name=["']([^"']+)["']/gi
    while ((m = re2.exec(content)) !== null) names.add(m[1])
  }
  return names
}

function findSvgFilesForNames(names) {
  const found = []
  for (const dir of VITE_SVG_DIRS) {
    const absDir = path.join(SRC, dir)
    if (!fs.existsSync(absDir)) continue
    function walk(d) {
      for (const name of fs.readdirSync(d)) {
        const full = path.join(d, name)
        if (fs.statSync(full).isDirectory()) walk(full)
        else {
          const base = path.basename(name, path.extname(name))
          if (names.has(base)) found.push(full)
        }
      }
    }
    walk(absDir)
  }
  return found
}

function relSrc(filePath) {
  return path.relative(SRC, filePath).replace(/\\/g, '/')
}

// --- Run ---
console.log('Tracing active-safety dependencies (v2)...\n')

const entryPoints = getEntryPoints()
console.log(`Entry points: ${entryPoints.length}`)

let { reachable, unresolved } = traceDependencies(entryPoints)

// Add svg icons referenced by name in reachable vue files
const svgNames = extractSvgIconNames([...reachable.values()])
const svgFiles = findSvgFilesForNames(svgNames)
for (const f of svgFiles) {
  if (!reachable.has(normPath(f))) {
    const sub = traceDependencies([f])
    for (const [k, v] of sub.reachable) reachable.set(k, v)
  }
}

console.log(`Reachable files: ${reachable.size}`)
console.log(`SVG icon names used: ${svgNames.size}, matched files: ${svgFiles.length}`)

const apaasFiles = collectAllFilesUnder(APAAS_SCAN_DIRS)
const reachableSet = new Set([...reachable.keys()])
const unreachable = apaasFiles.filter(f => !reachableSet.has(normPath(f)))
const reachableApaas = apaasFiles.filter(f => reachableSet.has(normPath(f)))

const unreachableByDir = {}
for (const f of unreachable) {
  const rel = relSrc(f)
  let topKey = rel.split('/')[0]
  if (rel.startsWith('pages/')) topKey = 'pages/' + rel.split('/')[1]
  else if (rel.startsWith('api/')) topKey = 'api/' + rel.split('/')[1]
  if (!unreachableByDir[topKey]) unreachableByDir[topKey] = []
  unreachableByDir[topKey].push(rel)
}

const pagesSubdirs = ['processui', 'task_center', 'Intelligent_approval', 'messageui']
const deletableFolders = []
const partialFolders = []
for (const sub of pagesSubdirs) {
  const allInDir = collectAllFilesUnder([`pages/${sub}`])
  const reach = allInDir.filter(f => reachableSet.has(normPath(f))).length
  if (reach === 0) deletableFolders.push(`pages/${sub}`)
  else if (reach < allInDir.length) partialFolders.push({ dir: `pages/${sub}`, reachable: reach, total: allInDir.length, deletable: allInDir.length - reach })
}

// Entire api subdirs deletable?
const apiSubdirs = ['smartCity', 'processui', 'unifiedUsert', 'login', 'sso_apaas', 'aiApaas']
const deletableApiDirs = []
for (const sub of apiSubdirs) {
  const allInDir = collectAllFilesUnder([`api/${sub}`])
  const reach = allInDir.filter(f => reachableSet.has(normPath(f))).length
  if (reach === 0) deletableApiDirs.push(`api/${sub}`)
}

const report = {
  summary: {
    entryPoints: entryPoints.length,
    reachableTotal: reachable.size,
    apaasScanFiles: apaasFiles.length,
    reachableApaas: reachableApaas.length,
    unreachableApaas: unreachable.length,
    unresolvedImports: unresolved.length,
    svgIconNamesUsed: [...svgNames].sort(),
  },
  deletableTopLevelFolders: deletableFolders,
  deletableApiDirs,
  partialPagesFolders: partialFolders,
  unreachableByDirCount: Object.fromEntries(
    Object.entries(unreachableByDir).sort((a, b) => b[1].length - a[1].length).map(([k, v]) => [k, v.length])
  ),
  reachableApaasFiles: reachableApaas.map(relSrc).sort(),
  unreachableApaasFiles: unreachable.map(relSrc).sort(),
  unresolvedImports: unresolved.map(u => ({ from: relSrc(u.from), spec: u.spec })),
}

fs.writeFileSync(
  path.join(PROJECT_ROOT, 'scripts', 'active-safety-deps-report.json'),
  JSON.stringify(report, null, 2)
)

console.log('\n=== SUMMARY ===')
console.log(`Apaas scan dirs total files: ${apaasFiles.length}`)
console.log(`Reachable (MUST KEEP): ${reachableApaas.length}`)
console.log(`Unreachable (candidate DELETE): ${unreachable.length}`)
console.log(`Unresolved imports: ${unresolved.length}`)

console.log('\n=== ENTIRE TOP-LEVEL FOLDERS SAFE TO DELETE ===')
if (deletableFolders.length === 0) console.log('(none under pages/)')
else deletableFolders.forEach(f => console.log(`  ${f}/`))

console.log('\n=== PARTIAL pages/* (keep some, delete rest) ===')
partialFolders.forEach(p => console.log(`  ${p.dir}: keep ${p.reachable}, delete ${p.deletable} of ${p.total}`))

console.log('\n=== UNREACHABLE COUNT BY DIRECTORY ===')
for (const [dir, count] of Object.entries(report.unreachableByDirCount)) {
  console.log(`  ${dir}: ${count}`)
}

console.log(`\nFull report: scripts/active-safety-deps-report.json`)
