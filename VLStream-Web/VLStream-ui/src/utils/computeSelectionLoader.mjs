import { prepareSharedCompute } from './prepareSharedCompute.mjs'

// Each scope owns captured identity assertions, in-flight reads and per-node checks.
// No response cache: submission callers always read an authoritative snapshot.
export function createComputeSelectionLoader({ captureIdentity, read, refresh }) {
  let scope
  const isCurrent = identity => {
    try { identity.assertCurrent(); return true } catch { return false }
  }
  const getScope = () => {
    if (!scope || !isCurrent(scope.identity)) {
      scope = { identity: captureIdentity(), reading: null, checks: new Map(), completed: 0 }
    }
    return scope
  }
  const snapshot = owner => {
    owner.identity.assertCurrent()
    if (!owner.reading) {
      const version = owner.completed
      const pending = Promise.resolve().then(() => {
        owner.identity.assertCurrent()
        return read()
      }).then(response => {
        owner.identity.assertCurrent()
        return { response, version }
      }).finally(() => {
        if (owner.reading === pending) owner.reading = null
      })
      owner.reading = pending
    }
    return owner.reading
  }
  const check = (owner, id) => {
    owner.identity.assertCurrent()
    const key = String(id)
    if (!owner.checks.has(key)) {
      const pending = Promise.resolve().then(() => {
        owner.identity.assertCurrent()
        return refresh(id)
      }).catch(error => {
        owner.identity.assertCurrent()
        // A failed check still needs a fresh server snapshot, never fabricated READY.
        return error
      }).then(() => {
        owner.identity.assertCurrent()
        return ++owner.completed
      }).finally(() => {
        if (owner.checks.get(key) === pending) owner.checks.delete(key)
      })
      owner.checks.set(key, pending)
    }
    return owner.checks.get(key)
  }
  return async onUpdate => {
    const owner = getScope()
    const { response } = await snapshot(owner)
    owner.identity.assertCurrent()
    // Deliberately detached: the first real inventory is not held behind SSH checks.
    void prepareSharedCompute(response.data, async id => {
      const completed = await check(owner, id)
      let result = await snapshot(owner)
      // A GET already in flight when this check finished cannot prove its new state.
      if (result.version < completed) result = await snapshot(owner)
      owner.identity.assertCurrent()
      if (typeof onUpdate === 'function') onUpdate(result.response)
    }).catch(() => {})
    return response
  }
}
