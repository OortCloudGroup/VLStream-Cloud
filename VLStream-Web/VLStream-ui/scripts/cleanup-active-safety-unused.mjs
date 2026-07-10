/**
 * 删除主动安全模块未使用的 apaas 复制文件（保留 VLStream 原有文件）
 *
 * 用法:
 *   node scripts/cleanup-active-safety-unused.mjs          # 仅预览
 *   node scripts/cleanup-active-safety-unused.mjs --apply  # 执行删除
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import { execSync } from 'child_process'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const PROJECT_ROOT = path.resolve(__dirname, '..')
const SRC = path.join(PROJECT_ROOT, 'src')
const APPLY = process.argv.includes('--apply')

const norm = (p) => p.replace(/\\/g, '/')

/** VLStream 原有文件，不可删除 */
const VLS_PROTECTED = new Set([
  'components/ActionButtonGroup.md',
  'components/ActionButtonGroup.vue',
  'components/AdvancedSearch.vue',
  'components/AdvancedSearchTest.vue',
  'components/AnnotationLabelPanel.vue',
  'components/CollapseToggle.vue',
  'components/CronExpressionBuilder.vue',
  'components/DateRangePicker.vue',
  'components/DebugPanel.vue',
  'components/DeviceEditDialog.vue',
  'components/DeviceListPanel.vue',
  'components/DeviceTree.vue',
  'components/PTZControl.vue',
  'components/PTZPanel.vue',
  'components/PlayButton.vue',
  'components/PresetPanel.vue',
  'components/README.md',
  'components/RealSSHTerminal.vue',
  'components/RtspPlayer.vue',
  'components/SSHConnectionConfig.vue',
  'components/SSHTerminal.vue',
  'components/SearchInput.vue',
  'components/SearchResetButtons.vue',
  'components/TagSelector.vue',
  'components/VideoDialog.vue',
  'components/VideoLayoutControls.vue',
  'components/VideoLayoutDialog.vue',
  'components/basic-container/main.vue',
  'utils/auth.js',
  'utils/autoCrossSystemSync.js',
  'utils/cookieTokenSync.js',
  'utils/crossSystemTokenSync.js',
  'utils/func.js',
  'utils/mapConfig.js',
  'utils/oplayer.js',
  'utils/request.js',
  'utils/storageEventSync.js',
  'utils/tokenSync.js',
  'utils/unifiedPlatformTokenFetcher.js',
])

const SCAN_DIRS = [
  'pages',
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
  'assets',
  'components',
  'utils',
]

function walkDir(rel) {
  const abs = path.join(SRC, rel)
  if (!fs.existsSync(abs)) return []
  const files = []
  for (const name of fs.readdirSync(abs)) {
    const r = rel ? norm(`${rel}/${name}`) : norm(name)
    const full = path.join(SRC, r)
    if (fs.statSync(full).isDirectory()) files.push(...walkDir(r))
    else files.push(r)
  }
  return files
}

function removeEmptyDirs(dir) {
  if (!fs.existsSync(dir)) return
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name)
    if (fs.statSync(full).isDirectory()) removeEmptyDirs(full)
  }
  if (dir !== SRC && fs.existsSync(dir) && fs.readdirSync(dir).length === 0) {
    fs.rmdirSync(dir)
    console.log('  removed empty dir:', path.relative(SRC, dir))
  }
}

console.log('Step 1: refresh dependency report...\n')
execSync('node scripts/trace-active-safety-deps.mjs', { cwd: PROJECT_ROOT, stdio: 'inherit' })

const report = JSON.parse(
  fs.readFileSync(path.join(PROJECT_ROOT, 'scripts', 'active-safety-deps-report.json'), 'utf8')
)
const keep = new Set(report.reachableApaasFiles.map(norm))

fs.writeFileSync(
  path.join(PROJECT_ROOT, 'scripts', 'active-safety-KEEP-list.txt'),
  [...keep].sort().join('\n') + '\n'
)

const toDelete = []
for (const dir of SCAN_DIRS) {
  for (const rel of walkDir(dir)) {
    if (!keep.has(rel) && !VLS_PROTECTED.has(rel)) toDelete.push(rel)
  }
}

toDelete.sort()
fs.writeFileSync(
  path.join(PROJECT_ROOT, 'scripts', 'active-safety-DELETE-list.txt'),
  toDelete.join('\n') + (toDelete.length ? '\n' : '')
)

console.log(`\n=== CLEANUP ${APPLY ? 'APPLY' : 'DRY-RUN'} ===`)
console.log(`Keep: ${keep.size} files`)
console.log(`Delete candidates: ${toDelete.length}`)

if (toDelete.length === 0) {
  console.log('\nNo unused apaas-copied files to delete.')
  process.exit(0)
}

toDelete.forEach((f) => console.log(`  ${APPLY ? 'DELETE' : 'WOULD DELETE'}: ${f}`))

if (!APPLY) {
  console.log('\nRun with --apply to delete these files.')
  process.exit(0)
}

for (const rel of toDelete) {
  fs.unlinkSync(path.join(SRC, rel))
}

for (const dir of SCAN_DIRS) {
  removeEmptyDirs(path.join(SRC, dir))
}

console.log(`\nDeleted ${toDelete.length} files.`)
