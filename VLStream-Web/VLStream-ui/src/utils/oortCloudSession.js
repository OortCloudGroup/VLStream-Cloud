import { getPlatformAccessToken } from '@/utils/request'
import { getModelHubAccessToken } from '@/utils/modelHubAuth'

// A platform account and a VLS local session are different credentials.
export function getOortCloudSession(tenantMode) {
  if (tenantMode === 'multi') {
    const params = new URLSearchParams(window.location.search)
    return {
      usesPlatformSession: true,
      accessToken: getPlatformAccessToken() || '',
      tenantId: params.get('tenantId') || params.get('tenant_id')
        || sessionStorage.getItem('tenantId') || localStorage.getItem('tenantId') || ''
    }
  }
  return {
    usesPlatformSession: false,
    accessToken: tenantMode === 'single' ? getModelHubAccessToken() : '',
    tenantId: sessionStorage.getItem('modelHubTenantId') || localStorage.getItem('modelHubTenantId') || ''
  }
}
