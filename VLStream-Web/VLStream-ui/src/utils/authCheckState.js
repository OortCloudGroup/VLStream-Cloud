import { ref } from 'vue'

// Initial navigation can be cancelled before router-view has a component.
// Keep that state visible without discarding the identity or entering a protected page.
export const authCheckFailed = ref(false)
export const authCheckForbidden = ref(false)
export const authCheckRetryPath = ref('/workspace')

export function beginAuthCheck() {
  authCheckFailed.value = false
  authCheckForbidden.value = false
}

export function failAuthCheck(error, retryPath) {
  authCheckRetryPath.value = retryPath
  authCheckForbidden.value = error?.response?.status === 403 || Number(error?.response?.data?.code) === 403
  authCheckFailed.value = true
}
