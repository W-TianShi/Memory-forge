import { clearAuth, getUsername } from './auth.js'

// Shared fetch wrapper that auto-attaches auth token and handles 401 expiry
export async function apiFetch(url, options = {}) {
  const headers = { ...(options.headers || {}) }

  // Auto-attach auth token
  const token = localStorage.getItem('mf_token')
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  if (!headers['Content-Type'] && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  let res
  try {
    res = await fetch(url, { ...options, headers })
  } catch (e) {
    // Network error — don't clear auth, just rethrow
    throw e
  }

  // 401 / 403 → token expired or invalid, force re-login
  if (res.status === 401 || res.status === 403) {
    const hadAuth = !!getUsername()
    clearAuth()
    // Dispatch a custom event so the app can react (show login modal, redirect, etc.)
    window.dispatchEvent(new CustomEvent('auth-expired', {
      detail: { status: res.status, hadAuth }
    }))
  }

  return res
}
