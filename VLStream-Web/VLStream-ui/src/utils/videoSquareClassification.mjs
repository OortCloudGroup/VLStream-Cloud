export function categorySubtreeIds(tree, selectedId) {
  for (const node of tree) {
    if (String(node.id) === String(selectedId)) {
      const ids = new Set()
      const visit = item => { ids.add(String(item.id)); (item.children || []).forEach(visit) }
      visit(node)
      return ids
    }
    const nested = categorySubtreeIds(node.children || [], selectedId)
    if (nested.size) return nested
  }
  return new Set()
}

export function assignmentMatches(assignment, type, categoryIds) {
  if (!assignment) return false
  const ids = type === 'REGION' ? [assignment.regionId] : type === 'GROUP' ? [assignment.groupId] : assignment.tagIds || []
  return ids.some(id => id != null && categoryIds.has(String(id)))
}

// Classification assignments use the protocol's row key, never an unqualified
// logical ID that can collide with another protocol's device identifier.
export function createClassificationReader(fetchAssignment) {
  const cache = new Map()
  return {
    clear: () => cache.clear(),
    async matches(devices, type, categoryIds, isCurrent = () => true) {
      const sources = new Map()
      for (const device of devices) for (const source of device.sources || []) {
        if (source.protocol && source.rowId != null) sources.set(`${source.protocol}:${source.rowId}`, source)
      }
      const queue = [...sources.entries()]
      const matching = new Set()
      let index = 0
      let failed = false
      await Promise.all(Array.from({ length: Math.min(4, queue.length) }, async () => {
        while (index < queue.length && isCurrent() && !failed) {
          const [key, source] = queue[index++]
          if (!cache.has(key)) cache.set(key, Promise.resolve().then(() => fetchAssignment(source.protocol, String(source.rowId)))
            .catch(error => { failed = true; cache.delete(key); throw error }))
          const assignment = await cache.get(key)
          if (assignmentMatches(assignment, type, categoryIds)) matching.add(key)
        }
      }))
      if (!isCurrent()) return []
      return devices.filter(device => (device.sources || []).some(source => matching.has(`${source.protocol}:${source.rowId}`))).map(device => device.id)
    },
  }
}
