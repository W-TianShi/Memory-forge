import { apiFetch } from './apiClient.js'

const BASE = '/api/word-sheets'

export async function listSheets() {
  const r = await apiFetch(BASE)
  if (!r.ok) throw new Error(await r.text())
  return r.json()
}

export async function saveSheet(data) {
  const r = await apiFetch(BASE, { method: 'POST', body: JSON.stringify(data) })
  if (!r.ok) throw new Error(await r.text())
  return r.json()
}

export async function getSheet(id) {
  const r = await apiFetch(`${BASE}/${id}`)
  if (!r.ok) throw new Error(await r.text())
  return r.json()
}

export async function deleteSheet(id) {
  const r = await apiFetch(`${BASE}/${id}`, { method: 'DELETE' })
  if (!r.ok) throw new Error(await r.text())
}
