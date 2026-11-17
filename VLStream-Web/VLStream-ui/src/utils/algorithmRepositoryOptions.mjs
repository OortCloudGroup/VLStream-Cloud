// Preserve Java Long IDs as strings; selectable nodes use the authorized catalog only.
export function algorithmRepositoryOptions(rows) {
  const nodes = new Map((rows || []).filter(row => row.id != null).map(row => [String(row.id), {
    id: String(row.id), name: row.name, parentId: String(row.parentId || '0'), children: [], disabled: Number(row.status) !== 1
  }]))
  const roots = []
  for (const node of nodes.values()) {
    const seen = new Set([node.id])
    let parent = nodes.get(node.parentId)
    let cycle = false
    while (parent) {
      if (seen.has(parent.id)) { cycle = true; break }
      if (parent.disabled) node.disabled = true
      seen.add(parent.id)
      parent = nodes.get(parent.parentId)
    }
    if (!cycle && nodes.has(node.parentId)) nodes.get(node.parentId).children.push(node)
    else roots.push(node)
  }
  return roots
}
