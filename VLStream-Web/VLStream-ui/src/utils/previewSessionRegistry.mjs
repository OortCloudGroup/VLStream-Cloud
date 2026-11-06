// Remembers live preview leases so they can be released when the page is refreshed or closed.
// The backend only expires an unreleased lease after 2+ minutes, which keeps the camera's RTSP session busy.
export function createPreviewSessionRegistry(release) {
  const sessions = new Set()
  return {
    register(session) {
      sessions.add(session)
      return () => { sessions.delete(session) }
    },
    releaseAll() {
      const pending = [...sessions]
      sessions.clear()
      for (const session of pending) {
        try { release(session) } catch { /* Best effort: the backend lease still expires on its own. */ }
      }
    },
    get size() { return sessions.size },
  }
}
