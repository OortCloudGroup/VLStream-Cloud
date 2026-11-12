import { test } from 'node:test'
import assert from 'node:assert/strict'
import { vmBuildEnvironment } from '../scripts/build-vm.mjs'

test('VM build supplies its deployed API routes without a production.local file', () => {
  const environment = vmBuildEnvironment({ PATH: 'tools' })
  assert.equal(environment.VITE_API_BASE_URL, '/bus/vls-server')
  assert.equal(environment.VITE_WVP_API_BASE_URL, '/bus/apaas-wvp-server')
  assert.equal(environment.VITE_ZLM_API_BASE_URL, '/bus/zlm-server')
  assert.equal(environment.VITE_ZLM_RTC_LAN_HOST, '192.168.50.200')
  assert.equal(environment.PATH, 'tools')
})

test('VM build replaces another deployment routing and RTC overrides', () => {
  const input = {
    VITE_API_BASE_URL: '/bus/apaas-vls-server',
    VITE_ZLM_API_BASE_URL: '/bus/apaas-zlmediakit',
    VITE_ZLM_RTC_LAN_HOST: '192.168.50.14',
    VITE_ZLM_RTC_CAND_TCP: '192.168.50.14:32742',
    VITE_ZLM_RTC_CAND_UDP: '192.168.50.14:32741'
  }
  const environment = vmBuildEnvironment(input)
  assert.equal(environment.VITE_API_BASE_URL, '/bus/vls-server')
  assert.equal(environment.VITE_ZLM_API_BASE_URL, '/bus/zlm-server')
  assert.equal(environment.VITE_ZLM_RTC_LAN_HOST, '192.168.50.200')
  assert.equal(environment.VITE_ZLM_RTC_CAND_TCP, '')
  assert.equal(environment.VITE_ZLM_RTC_CAND_UDP, '')
  assert.equal(input.VITE_ZLM_RTC_CAND_TCP, '192.168.50.14:32742')
})
