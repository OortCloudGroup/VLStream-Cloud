/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

const TOKEN_KEYS = ['accessToken', 'access_token', 'token']
const TENANT_KEYS = ['tenantId', 'tenant_id']

function firstParam(searchParams, keys) {
  for (const key of keys) {
    const value = searchParams.get(key)
    if (value) return value
  }
  return ''
}

function extractEmbeddedParam(href, keys) {
  const names = keys.join('|')
  const match = href.match(new RegExp(`[?&#](?:${names})=([^&#]+)`, 'i'))
  if (!match?.[1]) return ''
  try {
    return decodeURIComponent(match[1])
  } catch {
    return match[1]
  }
}

/**
 * Parse the callback aliases used by both VLS platform SSO and OortCloud auth.
 * Some platform deployments return `token`; keeping the aliases in one place
 * prevents an OortCloud callback from falling through to the VLS login flow.
 */
export function extractAuthCallbackParams(href) {
  const result = { accessToken: '', tenantId: '' }

  try {
    const url = new URL(href)
    result.accessToken = firstParam(url.searchParams, TOKEN_KEYS)
    result.tenantId = firstParam(url.searchParams, TENANT_KEYS)
  } catch {
    // Fall through to the embedded-query compatibility parser below.
  }

  if (!result.accessToken) {
    result.accessToken = extractEmbeddedParam(href, TOKEN_KEYS)
  }
  if (!result.tenantId) {
    result.tenantId = extractEmbeddedParam(href, TENANT_KEYS)
  }
  return result
}
