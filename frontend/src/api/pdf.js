const BASE = '/api/pdf'

function h() {
  const t = localStorage.getItem('mf_token')
  return t ? { 'Content-Type': 'application/json', 'Authorization': `Bearer ${t}` } : { 'Content-Type': 'application/json' }
}

export async function exportPdf(html, landscape = false, gridType = null, gridColor = null, autoBlank = false, annotations = null) {
  const r = await fetch(`${BASE}/export`, {
    method: 'POST',
    headers: h(),
    body: JSON.stringify({ html, landscape, gridType, gridColor, autoBlank, annotations })
  })
  if (!r.ok) throw new Error(await r.text())
  return r.blob()
}

export async function mergePdfs(items) {
  const r = await fetch(`${BASE}/merge`, {
    method: 'POST',
    headers: h(),
    body: JSON.stringify({ items })
  })
  if (!r.ok) throw new Error(await r.text())
  return r.blob()
}

export async function previewPdf(pdfBase64) {
  const r = await fetch(`${BASE}/preview`, {
    method: 'POST',
    headers: h(),
    body: JSON.stringify({ pdfBase64 })
  })
  if (!r.ok) throw new Error(await r.text())
  return r.json() // { pages: [{ pageIndex, width, height, imageBase64 }] }
}

export async function annotatePdf(pdfBase64, annotations) {
  const r = await fetch(`${BASE}/annotate`, {
    method: 'POST',
    headers: h(),
    body: JSON.stringify({ pdfBase64, annotations })
  })
  if (!r.ok) throw new Error(await r.text())
  return r.blob()
}
