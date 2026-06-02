const BASE = '/api/word-sheets'
function h() {
  const t = localStorage.getItem('mf_token')
  return t ? { 'Content-Type': 'application/json', 'Authorization': `Bearer ${t}` } : { 'Content-Type': 'application/json' }
}
export async function listSheets() { const r = await fetch(BASE, { headers: h() }); return r.json() }
export async function saveSheet(data) { const r = await fetch(BASE, { method: 'POST', headers: h(), body: JSON.stringify(data) }); return r.json() }
export async function getSheet(id) { const r = await fetch(`${BASE}/${id}`, { headers: h() }); return r.json() }
export async function deleteSheet(id) { await fetch(`${BASE}/${id}`, { method: 'DELETE', headers: h() }) }
