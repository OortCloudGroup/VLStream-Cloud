import wvpRequest from '@/utils/wvpRequest'

export function getConfigKey(configKey) {
  return wvpRequest({
    url: `/system/config/configKey/${encodeURIComponent(configKey)}`,
    method: 'get'
  })
}
