import { spawnSync } from 'node:child_process'
import { fileURLToPath, pathToFileURL } from 'node:url'

// VM builds use the existing production.local routes/LAN settings, but must not
// inherit a different deployment's explicit RTC address or NAT port.
export function vmBuildEnvironment(environment) {
  return {
    ...environment,
    VITE_ZLM_RTC_CAND_TCP: '',
    VITE_ZLM_RTC_CAND_UDP: ''
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const result = spawnSync(process.execPath, [
    fileURLToPath(new URL('../node_modules/vite/bin/vite.js', import.meta.url)),
    'build', '--mode', 'production', ...process.argv.slice(2)
  ], { env: vmBuildEnvironment(process.env), stdio: 'inherit', windowsHide: true })
  if (result.error) throw result.error
  process.exitCode = result.status ?? 1
}
