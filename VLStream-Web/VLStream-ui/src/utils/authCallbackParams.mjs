/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

const TOKEN_KEYS = ['accessToken', 'access_token', 'token']
const TENANT_KEYS = ['tenantId', 'tenant_id']
const CALLBACK_KEYS = new Set([...TOKEN_KEYS, ...TENANT_KEYS].map(key => key.toLowerCase()))

function embeddedPattern(keys, flags = 'i', allowEmpty = true) {
  return new RegExp(`[?&#](?:${keys.join('|')})=([^&#]${allowEmpty ? '*' : '+'})`, flags)
}

function firstParam(searchParams, keys) {
  for (const key of keys) {
    const value = searchParams.get(key)
    if (value) return value
  }
  return ''
}

function extractEmbeddedParam(href, keys) {
  const match = href.match(embeddedPattern(keys, 'i', false))
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

function cleanEmbeddedValue(value, depth) {
  let candidate = value
  // Also clean material encoded by a previous URLSearchParams round-trip.
  for (let pass = 0; pass < 3; pass++) {
    try {
      const decoded = decodeURIComponent(candidate)
      if (decoded === candidate || !embeddedPattern([...TOKEN_KEYS, ...TENANT_KEYS]).test(decoded)) break
      candidate = decoded
    } catch { break }
  }
  if (depth < 4 && /^https?:\/\//i.test(candidate)) return cleanUrl(candidate, depth + 1)
  return candidate.replace(embeddedPattern([...TOKEN_KEYS, ...TENANT_KEYS], 'gi'), '')
}

function cleanQuery(query, depth) {
  const result = new URLSearchParams()
  let changed = false
  for (const [key, value] of new URLSearchParams(query)) {
    if (CALLBACK_KEYS.has(key.toLowerCase())) { changed = true; continue }
    const cleanKey = cleanEmbeddedValue(key, depth)
    const cleanValue = cleanEmbeddedValue(value, depth)
    if (cleanKey !== key || cleanValue !== value) changed = true
    if (cleanKey && !CALLBACK_KEYS.has(cleanKey.toLowerCase())) result.append(cleanKey, cleanValue)
    else changed = true
  }
  return changed ? result.toString() : query.replace(/^\?/, '')
}

function cleanUrl(href, depth) {
  const absolute = /^[a-z][a-z\d+.-]*:/i.test(href)
  const url = new URL(href, 'https://callback.invalid')
  url.pathname = cleanEmbeddedValue(url.pathname, depth)
  url.search = cleanQuery(url.search, depth)
  const hash = url.hash.slice(1), queryStart = hash.indexOf('?')
  if (queryStart >= 0) {
    const query = cleanQuery(hash.slice(queryStart + 1), depth)
    url.hash = cleanEmbeddedValue(hash.slice(0, queryStart), depth) + (query ? '?' + query : '')
  } else if (/^(?:accessToken|access_token|token|tenantId|tenant_id)=/i.test(hash)) {
    url.hash = cleanQuery(hash, depth)
  } else {
    url.hash = cleanEmbeddedValue(hash, depth)
  }
  return absolute ? url.toString() : url.pathname + url.search + url.hash
}

/** Remove every callback form accepted by extraction, including embedded query values/hash routes.
 * Non-secret routing parameters survive. Use before async validation and before writing router/history state. */
export function stripAuthCallbackParams(href) {
  return cleanUrl(href, 0)
}
