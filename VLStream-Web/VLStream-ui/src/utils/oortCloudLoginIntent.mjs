// Only a successful login callback can restore a cancelled authorization.
// Keep this in memory: a page reload or a remembered session is not a new login.
let pendingLogin = null
let revision = 0

export function markOortCloudLogin(session) {
  revision++
  pendingLogin = session.accessToken ? { ...session, expiresAt: Date.now() + 5 * 60 * 1000 } : null
}

export function getOortCloudLoginIntent(session) {
  if (!pendingLogin || pendingLogin.expiresAt < Date.now()) return null
  return pendingLogin.accessToken === session.accessToken
    && String(pendingLogin.tenantId || '') === String(session.tenantId || '')
    && pendingLogin.usesPlatformSession === session.usesPlatformSession ? pendingLogin : null
}

export function finishOortCloudLoginIntent(intent) {
  if (intent === pendingLogin) pendingLogin = null
}

export function cancelOortCloudLoginIntent() {
  pendingLogin = null
  revision++
}

export function getOortCloudAuthorizationRevision() { return revision }
