// Display policy only. Every mutation remains authorized by the backend.
export const hasComputePermission = (access, permission) => Array.isArray(access?.permissions)
  && (access.permissions.includes('*:*:*') || access.permissions.includes(permission))

export const canManageComputeSupply = (access, row) => Boolean(row)
  && hasComputePermission(access, 'vls:compute:company-publish')
  && hasComputePermission(access, 'vls:compute:publish')
  && hasComputePermission(access, 'vls:compute:configure')

export const supportsComputeProvider = row => Boolean(row)
  && (row.provider === 'autodl' && row.providerProduct === 'AUTODL_PRO'
    || row.provider === 'aliyun' && row.providerProduct === 'ECS')

export function computeProviderActions(row, capability, access) {
  const supported = supportsComputeProvider(row)
  // apiControl is computed by the existing backend adapter, including its service switch.
  // Missing, stale, filtered-out, or mismatched evidence must never enable an action.
  const ready = supported && row.controlMode === 'API' && row.providerCredentialConfigured === true
    && Boolean(row.providerInstanceId) && (row.provider !== 'aliyun' || Boolean(row.providerRegion))
    && capability?.id === row.id && capability.apiControl === true
  return {
    supported,
    configure: Boolean(ready && capability.canManage === true && hasComputePermission(access, 'vls:compute:configure')),
    query: Boolean(ready && capability.canQueryProvider === true && hasComputePermission(access, 'vls:compute:power'))
  }
}
