export function hiddenGrantedMenuIds(nodes, grantedIds) {
  const visible = new Set()
  const collect = items => items.forEach(item => {
    visible.add(String(item.id))
    collect(item.children || [])
  })
  collect(nodes || [])
  return (grantedIds || []).map(String).filter(id => !visible.has(id))
}

export function selectedMenuIds(checked, halfChecked) {
  return [...new Set([...(checked || []), ...(halfChecked || [])].map(String))]
}

// Initialize children before parents and never cascade an existing parent grant.
export function orderedVisibleGrants(nodes, grantedIds) {
  const depths = new Map()
  const visit = (items, depth) => items.forEach(item => {
    depths.set(String(item.id), depth)
    visit(item.children || [], depth + 1)
  })
  visit(nodes || [], 0)
  return [...new Set((grantedIds || []).map(String))]
    .filter(id => depths.has(id))
    .sort((a, b) => depths.get(b) - depths.get(a))
}

// Element Tree infers parent checks from child state. An untouched dialog must
// round-trip the exact original grants instead of persisting inferred checks.
export function changedVisibleGrants(original, initialSelection, currentSelection, touchedIds = []) {
  const result = new Set((original || []).map(String))
  const initial = new Set((initialSelection || []).map(String))
  const current = new Set((currentSelection || []).map(String))
  current.forEach(id => { if (!initial.has(id)) result.add(id) })
  initial.forEach(id => { if (!current.has(id)) result.delete(id) })
  touchedIds.map(String).forEach(id => { if (current.has(id)) result.add(id); else result.delete(id) })
  return [...result]
}

export function preserveGrantableHiddenGrants(nodes, original, grantable, visibleSelection) {
  const allowed = new Set((grantable || []).map(String))
  const hidden = hiddenGrantedMenuIds(nodes, original).filter(id => allowed.has(id))
  return [...new Set([...(visibleSelection || []).map(String).filter(id => allowed.has(id)), ...hidden])]
}
