import { apiFetch } from './apiClient.js'

const BASE = '/api/notes'

export async function listNotes() {
  const r = await apiFetch(BASE)
  if (!r.ok) throw new Error(await r.text())
  return r.json()
}

export async function createNote(title, content) {
  const r = await apiFetch(BASE, {
    method: 'POST',
    body: JSON.stringify({ title, content })
  })
  if (!r.ok) throw new Error(await r.text())
  return r.json()
}

export async function updateNote(id, data) {
  const r = await apiFetch(`${BASE}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
  if (!r.ok) throw new Error(await r.text())
  return r.json()
}

export async function deleteNote(id) {
  const r = await apiFetch(`${BASE}/${id}`, { method: 'DELETE' })
  if (!r.ok) throw new Error(await r.text())
}
