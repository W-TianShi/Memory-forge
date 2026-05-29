const BASE = '/api/auth'

function getToken() {
  return localStorage.getItem('mf_token')
}

export function setAuth(token, username) {
  localStorage.setItem('mf_token', token)
  localStorage.setItem('mf_username', username)
}

export function clearAuth() {
  localStorage.removeItem('mf_token')
  localStorage.removeItem('mf_username')
}

export function getUsername() {
  return localStorage.getItem('mf_username')
}

export function isLoggedIn() {
  return !!getToken()
}

async function post(path, body) {
  const headers = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(BASE + path, {
    method: 'POST',
    headers,
    body: JSON.stringify(body)
  })
  return res.json()
}

export function sendCode(email) {
  return post('/send-code', { email, code: '' })
}

export function register(email, password, code) {
  return post('/register', { email, password, code })
}

export function login(email, password) {
  return post('/login', { email, password })
}

export function verifyCode(email, code) {
  return post('/verify', { email, code })
}
