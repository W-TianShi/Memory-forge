const BASE = '/api/pdf'

function h() {
  const t = localStorage.getItem('mf_token')
  return t ? { 'Content-Type': 'application/json', 'Authorization': `Bearer ${t}` } : { 'Content-Type': 'application/json' }
}

export async function exportPdf(html, landscape = false, gridType = null, gridColor = null) {
  const r = await fetch(`${BASE}/export`, {
    method: 'POST',
    headers: h(),
    body: JSON.stringify({ html, landscape, gridType, gridColor })
  })
  if (!r.ok) throw new Error(await r.text())
  return r.blob()
}
