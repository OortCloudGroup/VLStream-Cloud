import { spawnSync } from 'node:child_process'
import { fileURLToPath, pathToFileURL } from 'node:url'

// The established VM has its own gateway routes and published RTC ports.
// Supply these before Vite loads another deployment's production defaults.
export function vmBuildEnvironment(environment) {
  return {
    ...environment,
    VITE_API_BASE_URL: '/bus/vls-server',
    VITE_WVP_API_BASE_URL: '/bus/apaas-wvp-server',
    VITE_ZLM_API_BASE_URL: '/bus/zlm-server',
    VITE_ZLM_RTC_LAN_HOST: '192.168.50.200',
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
