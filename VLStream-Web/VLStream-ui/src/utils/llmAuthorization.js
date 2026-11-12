import { authorizeOortCloudLlm, getOortCloudLlmAuthorization } from '@/api/llmReview'
import { getOortCloudTokenKey, getOortCloudTokenList } from '@/api/oortCloudAccount'
import { getModelHubUserInfo } from '@/api/modelHubUser'
import { getOortCloudSession } from '@/utils/oortCloudSession'
import { authManager } from '@/utils/auth'
import { getOortCloudLoginIntent, finishOortCloudLoginIntent, getOortCloudAuthorizationRevision } from '@/utils/oortCloudLoginIntent.mjs'

// Share the same guarded flow between default authorization and the management action.
export async function syncLlmAuthorization(tenantMode, { automatic = true, isCurrent = () => true } = {}) {
  const session = getOortCloudSession(tenantMode)
  const identity = authManager.getIdentitySnapshot()
  const loginIntent = getOortCloudLoginIntent(session)
  const background = automatic && !loginIntent
  const revision = getOortCloudAuthorizationRevision()
  const current = () => {
    const now = getOortCloudSession(tenantMode)
    return revision === getOortCloudAuthorizationRevision() && isCurrent() && authManager.isIdentityCurrent(identity)
      && now.accessToken === session.accessToken && now.tenantId === session.tenantId
      && now.usesPlatformSession === session.usesPlatformSession
  }
  if (!identity.token || !session.accessToken) {
    if (automatic) return null
    throw new Error('请先登录 OortCloud')
  }
  const status = await getOortCloudLlmAuthorization()
  if (!current()) return null
  if (Number(status?.code) !== 200) throw new Error('加载 OortCloud 授权失败')
  if (background && status.data?.revoked) return status.data
  const response = await getModelHubUserInfo({ desensitize: true }, session)
  if (!current()) return null
  if (Number(response?.code) !== 200 || !response.data) throw new Error('OortCloud 登录校验失败')
  const outer = response.data
  const user = outer.userInfo || outer.user || outer
  const platformUserId = user.userId || user.user_id || user.oort_uuid || user.id
  const platformUserName = user.userName || user.user_name || user.oort_name || user.realName || user.name || ''
  if (!platformUserId) throw new Error('OortCloud 用户标识不能为空')
  // A tenant may already hold another account's credential. Only skip when the
  // verified current account (including its latest display name) still matches.
  if (automatic && status.data?.authorized
    && String(status.data.platformUserId || '') === String(platformUserId)
    && String(status.data.platformUserName || '') === String(platformUserName)) {
    finishOortCloudLoginIntent(loginIntent)
    return status.data
  }
  const tokens = await getOortCloudTokenList(session)
  if (!current()) return null
  const selected = tokens.find(token => token.status === 1)
  if (!selected) throw new Error('当前 OortCloud 账户没有启用的 API 身份，未执行授权')
  const apiKey = await getOortCloudTokenKey(selected.id, session)
  if (!current()) return null
  if (typeof apiKey !== 'string' || !apiKey.trim()) throw new Error('OortCloud 可用令牌不能为空')
  const saved = await authorizeOortCloudLlm({ platformUserId: String(platformUserId), platformUserName,
    apiKey, automatic: String(background) }, identity, current)
  if (!current()) return null
  if (Number(saved?.code) !== 200 || (!background && !saved.data?.authorized)) throw new Error('视觉复核授权未确认，请检查授权状态后再试')
  finishOortCloudLoginIntent(loginIntent)
  window.dispatchEvent(new CustomEvent('llmAuthorizationChanged'))
  return saved.data
}
