/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

// Default algorithm covers (300x200) for algorithms without an uploaded image:
// one harmonious family of soft gradients with a faint grid and glow, so the
// catalog reads as deliberate instead of flat colour blocks.
const buildCover = (from, to, glow) => {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="300" height="200" viewBox="0 0 300 200" preserveAspectRatio="xMidYMid slice">
<defs>
<linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${from}"/><stop offset="1" stop-color="${to}"/></linearGradient>
<radialGradient id="r" cx="0.82" cy="0.18" r="0.7"><stop offset="0" stop-color="${glow}" stop-opacity="0.55"/><stop offset="1" stop-color="${glow}" stop-opacity="0"/></radialGradient>
<pattern id="p" width="20" height="20" patternUnits="userSpaceOnUse"><path d="M20 0H0V20" fill="none" stroke="#fff" stroke-opacity="0.07"/></pattern>
</defs>
<rect width="300" height="200" fill="url(#g)"/><rect width="300" height="200" fill="url(#p)"/><rect width="300" height="200" fill="url(#r)"/>
<circle cx="236" cy="150" r="54" fill="none" stroke="#fff" stroke-opacity="0.14" stroke-width="1.5"/>
<circle cx="236" cy="150" r="30" fill="#fff" fill-opacity="0.08"/>
<path d="M28 160h64M28 172h40" stroke="#fff" stroke-opacity="0.22" stroke-width="4" stroke-linecap="round"/>
</svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}

export const ALGORITHM_COVERS = [
  buildCover('#2F5BFF', '#1A2FA8', '#7FA2FF'),
  buildCover('#4F46E5', '#2A2486', '#A5A1FF'),
  buildCover('#0EA5B7', '#0B5F84', '#7EE6F0'),
  buildCover('#7C4DDB', '#3F2A8C', '#C5A8FF'),
  buildCover('#2563EB', '#0F3B8F', '#93C5FD'),
  buildCover('#3B6E9E', '#1D3352', '#9CC3E6')
]

// Older versions persisted one of their generated 300x200 SVG covers as
// imageUrl; those are shown with the current cover family instead.
const LEGACY_GENERATED_COVER_PREFIX = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIg'

export const isLegacyGeneratedCover = url => String(url || '').startsWith(LEGACY_GENERATED_COVER_PREFIX)

// Stable per-algorithm pick; works on Long ids that exceed Number precision.
export const defaultAlgorithmCover = id => {
  const digits = String(id ?? '')
  let remainder = 0
  if (/^\d+$/.test(digits)) {
    for (const digit of digits) remainder = (remainder * 10 + Number(digit)) % ALGORITHM_COVERS.length
  }
  return ALGORITHM_COVERS[remainder]
}

export const algorithmCoverUrl = algorithm => {
  const url = String(algorithm?.imageUrl || '').trim()
  return url && !isLegacyGeneratedCover(url) ? url : defaultAlgorithmCover(algorithm?.id)
}
