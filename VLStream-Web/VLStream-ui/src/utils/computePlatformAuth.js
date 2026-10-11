import { ElMessage, ElMessageBox } from 'element-plus'
import { getComputeAuthorizationMode, checkComputePlatformAuthorization } from '@/api/compute'
import { getOortCloudSession } from '@/utils/oortCloudSession'
import { getLocalSessionToken } from '@/utils/request'
import { startModelHubLogin, clearModelHubAuth } from '@/utils/modelHubAuth'
import { redirectToPlatformLogin, clearPlatformAccessToken } from '@/utils/platformSession'
import { translatePhrase as uiText } from '@/i18n'
import SHA256 from 'crypto-js/sha256'

const draftKey = 'vls-training-compute-return'
export function bindComputeDraftToActor(context) {
  try {
    let draft = JSON.parse(sessionStorage.getItem(draftKey) || 'null')
    if (!draft || !context?.vlsUserId || !context?.vlsTenantId) return
    const pick = (value, fields) => Object.fromEntries(fields.filter(key => value?.[key] !== undefined).map(key => [key, value[key]]))
    draft = pick(draft, ['session', 'savedAt', 'task', 'computeNodeId', 'cloudModelSource', 'epochTotal', 'batchSize', 'trainImgSize', 'trainingMode', 'autoPublish', 'selectedDatasets', 'selectedDatasetIds', 'dataStrategy', 'resolution', 'epochMode', 'customValidation'])
    draft.task = { ...pick(draft.task, ['id', 'name', 'algorithmName', 'trainType']), originalData: pick(draft.task?.originalData, ['id', 'taskName', 'algorithmId', 'datasetId', 'epochTotal', 'trainStatus']) }
    draft.selectedDatasets = (draft.selectedDatasets || []).map(value => pick(value, ['value', 'label', 'annotationName', 'typeLabel', 'progress', 'status', 'statusLabel', 'annotatedCount', 'totalCount', 'annotationType', 'trainType']))
    draft.authorizationActor = { userId: context.vlsUserId, tenantId: context.vlsTenantId }
    sessionStorage.setItem(draftKey, JSON.stringify(draft))
  } catch { sessionStorage.removeItem(draftKey) }
}
export async function canRestoreComputeDraft(draft) {
  if (!draft?.task || !Number.isFinite(draft.savedAt) || Date.now() < draft.savedAt || Date.now() - draft.savedAt >= 30 * 60 * 1000) return false
  if (!getLocalSessionToken()) return false
  if (draft.session === SHA256(getLocalSessionToken()).toString()) return true
  if (!draft.authorizationActor) return false
  try {
    const modeResponse = await getComputeAuthorizationMode()
    const mode = modeResponse?.data?.tenantType
    if (!['multi', 'single'].includes(mode)) return false
    const localToken = getLocalSessionToken(), platformToken = getOortCloudSession(mode).accessToken
    const context = (await checkComputePlatformAuthorization(platformToken)).data
    return localToken === getLocalSessionToken() && platformToken === getOortCloudSession(mode).accessToken
      && context?.mode === mode && context?.vlsUserId === draft.authorizationActor.userId && context?.vlsTenantId === draft.authorizationActor.tenantId
  } catch { return false }
}

// Token snapshots live only in this call stack, never in a form draft or payer record.
export const isComputeAuthorizationCurrent = proof => !!proof
  && getLocalSessionToken() === proof.localToken
  && getOortCloudSession(proof.mode).accessToken === proof.platformToken

export async function requireComputePlatformAuthorization(beforeRedirect = () => {}) {
  let mode
  try {
    const response = await getComputeAuthorizationMode()
    mode = response?.data?.tenantType
    if (!['single', 'multi'].includes(mode)) throw new Error('unknown mode')
  } catch { ElMessage.warning(uiText('无法确认平台授权模式，请稍后重试')); return null }
  const proof = { mode, platformToken: getOortCloudSession(mode).accessToken, localToken: getLocalSessionToken() }
  let context
  try { context = (await checkComputePlatformAuthorization(proof.platformToken)).data }
  catch { ElMessage.warning(uiText('平台身份校验暂不可用，请稍后重试')); return null }
  if (!isComputeAuthorizationCurrent(proof)) { ElMessage.warning(uiText('登录账户已变化，请重新选择操作')); return null }
  if (context?.authorized && context.mode === mode) return { ...proof, context }
  const confirmed = await ElMessageBox.confirm(uiText('收费算力需要授权登录平台账户；返回后只恢复配置，不会自动购买、开机或训练。取消可保留当前配置。'), uiText('平台授权'),
    { confirmButtonText: uiText('前往授权'), cancelButtonText: uiText('取消'), type: 'warning' }).then(() => true).catch(() => false)
  if (!confirmed || !isComputeAuthorizationCurrent(proof)) return null
  // The context contains only the backend-authenticated local actor, no credential.
  await beforeRedirect(context)
  if (!isComputeAuthorizationCurrent(proof)) return null
  if (mode === 'multi') { clearPlatformAccessToken(); redirectToPlatformLogin() }
  else { clearModelHubAuth(); startModelHubLogin(null, { returnToCurrent: true }) }
  return null
}
