import { apiFetch } from './apiClient.js'

const BASE = '/api/pdf'

export async function exportPdf(html, landscape = false, gridType = null, gridColor = null, autoBlank = false, annotations = null) {
  const r = await apiFetch(`${BASE}/export`, {
    method: 'POST',
    body: JSON.stringify({ html, landscape, gridType, gridColor, autoBlank, annotations })
  })
  if (!r.ok) throw new Error(await r.text())
  return r.blob()
}

export async function mergePdfs(items) {
  const r = await apiFetch(`${BASE}/merge`, {
    method: 'POST',
    body: JSON.stringify({ items })
  })
  if (!r.ok) throw new Error(await r.text())
  return r.blob()
}

export async function previewPdf(pdfBase64) {
  const r = await apiFetch(`${BASE}/preview`, {
    method: 'POST',
    body: JSON.stringify({ pdfBase64 })
  })
  if (!r.ok) throw new Error(await r.text())
  return r.json()
}

export async function annotatePdf(pdfBase64, annotations) {
  const r = await apiFetch(`${BASE}/annotate`, {
    method: 'POST',
    body: JSON.stringify({ pdfBase64, annotations })
  })
  if (!r.ok) throw new Error(await r.text())
  return r.blob()
}
