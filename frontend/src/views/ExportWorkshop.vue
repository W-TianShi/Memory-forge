<template>
  <div class="editor">
    <!-- ==== Edge-style Toolbar ==== -->
    <div class="tb">
      <div class="tb-l">
        <router-link to="/" class="tb-back"><SvgIcon name="chevronLeft" size="20" /></router-link>
        <span class="tb-title" v-if="fileName">{{ fileName }}</span>
        <span class="tb-title dim" v-else>出稿工坊</span>
      </div>
      <div class="tb-c" v-if="pages.length > 0">
        <button class="tb-btn" @click="goPage(-1)" :disabled="curPage <= 0">◂</button>
        <span class="tb-pn">{{ curPage + 1 }} / {{ pages.length }}</span>
        <button class="tb-btn" @click="goPage(1)" :disabled="curPage >= pages.length - 1">▸</button>
        <span class="tb-div">|</span>
        <button class="tb-btn" @click="zoomOut">−</button>
        <span class="tb-zp">{{ Math.round(zoom * 100) }}%</span>
        <button class="tb-btn" @click="zoomIn">+</button>
      </div>
      <div class="tb-r" v-if="pages.length > 0">
        <button class="tb-btn tl" :class="{ on: tool === 'text' }" @click="tool = 'text'">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="14" y="3" width="7" height="6" rx="1"/><path d="M17 4v13"/><line x1="17" y1="20" x2="17" y2="17"/><line x1="7" y1="6" x2="14" y2="6"/><line x1="7" y1="10" x2="14" y2="10"/><path d="M3 4v16a2 2 0 0 0 2 2h3"/></svg>
          <span>添加文本</span>
        </button>
        <button class="tb-btn" @click="undo" :disabled="!canUndo"><SvgIcon name="undo" size="16" /></button>
        <button class="tb-btn" @click="redo" :disabled="!canRedo"><SvgIcon name="redo" size="16" /></button>
        <button class="tb-save" @click="doSave" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
      </div>
    </div>

    <!-- ==== Main Area ==== -->
    <div class="main" ref="mainRef" @wheel.prevent="onWheel">
      <!-- Drop zone -->
      <div class="dropzone" v-if="!pdfBase64 && !loading && !loadError"
           @click="fileInput?.click()"
           @dragover.prevent @drop.prevent="onDrop">
        <div class="dz-icon">📄</div>
        <p class="dz-title">打开 PDF 文件进行编辑</p>
        <p class="dz-hint">点击选择文件，或拖拽 PDF 到此处</p>
        <input ref="fileInput" type="file" accept=".pdf,application/pdf" @change="onFilePicked" hidden />
        <p class="dz-or">— 或 —</p>
        <p class="dz-hint">从打印队列合并导出后点「送出稿工坊」</p>
      </div>

      <!-- Loading -->
      <div class="status" v-else-if="loading">
        <p class="spinner"></p>
        <p>{{ loadingMsg }}</p>
      </div>

      <!-- Error -->
      <div class="dropzone" v-else-if="loadError" @click="retryLoad">
        <p style="color:#f28b82;font-size:16px">加载失败</p>
        <p class="dz-hint">{{ loadError }}</p>
        <p class="dz-hint" style="margin-top:12px">点击重试</p>
      </div>

      <!-- Pages -->
      <div class="pages" v-else :style="{ transform: `scale(${zoom})`, transformOrigin: 'top center' }">
        <div v-for="(page, pi) in pages" :key="pi"
             class="page" :class="{ active: pi === curPage }"
             :style="{ width: page.dispW + 'px', height: page.dispH + 'px' }"
             @mousedown.left="onPageClick($event, pi)">
          <iframe v-if="page.blobUrl" :src="page.blobUrl"
                  :style="{ width: page.dispW + 'px', height: page.dispH + 'px', border: 'none', pointerEvents: 'none' }"></iframe>
          <!-- Annotations -->
          <div v-for="ann in pageAnns(pi)" :key="ann.id"
               class="ann"
               :class="{ sel: selAnn === ann.id, edit: editAnn === ann.id }"
               :style="{ left: ann.x + 'px', top: ann.y + 'px', fontSize: ann.fontSize + 'px', color: ann.color }"
               @mousedown.left.stop="onAnnDown($event, ann, pi)"
               @click.stop="selectAnn(ann.id)"
               @dblclick.stop="startEdit(ann, pi)">
            <span v-if="editAnn !== ann.id">{{ ann.text }}</span>
            <div v-else class="ann-edit" contenteditable="true"
                 @input="onEditInput($event, ann)"
                 @blur="finishEdit(ann)"
                 @keydown.escape.stop="finishEdit(ann)"
                 @keydown.enter.stop.prevent="finishEdit(ann)"
                 v-text="ann.text"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==== Annotation properties popup ==== -->
    <div class="apop" v-if="selAnnData && popupStyle" :style="popupStyle" @mousedown.stop>
      <button @click="changeSize(-2)">A⁻</button>
      <span class="ap-sz">{{ selAnnData.fontSize }}pt</span>
      <button @click="changeSize(2)">A⁺</button>
      <span class="ap-div">|</span>
      <span v-for="c in colors" :key="c" class="ap-c" :class="{ on: selAnnData.color === c }"
            :style="{ background: c }" @click="setColor(c)"></span>
      <input type="color" class="ap-pick" :value="selAnnData.color" @input="setColor($event.target.value)" />
      <span class="ap-div">|</span>
      <button class="ap-del" @click="delSel">🗑</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import SvgIcon from '../components/SvgIcon.vue'
import { previewPdf, annotatePdf } from '../api/pdf.js'

// ---- State ----
const fileInput = ref(null)
const mainRef = ref(null)
const fileName = ref('')
const pdfBase64 = ref(null)
const pages = ref([])       // [{ img(base64), w(pt), h(pt), dispW(px), dispH(px) }]
const curPage = ref(0)
const zoom = ref(1.0)
const loading = ref(false)
const loadingMsg = ref('')
const loadError = ref(null)
const saving = ref(false)

// Annotations
const annotations = ref([]) // [{ id, pageIndex, x, y, text, fontSize, color }]
const undos = ref([])
const redos = ref([])

// UI
const tool = ref('text')
const selAnn = ref(null)
const editAnn = ref(null)
const drag = ref(null)

const colors = ['#333333', '#000000', '#e74c3c', '#e67e22', '#2ecc71', '#3498db', '#9b59b6', '#1abc9c']

// ---- Computed ----
const selAnnData = computed(() => annotations.value.find(a => a.id === selAnn.value) || null)
const canUndo = computed(() => undos.value.length > 0)
const canRedo = computed(() => redos.value.length > 0)

function pageAnns(pi) { return annotations.value.filter(a => a.pageIndex === pi) }

const popupStyle = computed(() => {
  const ann = selAnnData.value
  if (!ann || !mainRef.value) return null
  const page = pages.value[ann.pageIndex]
  if (!page) return null
  // Find the annotation element in DOM
  const el = document.querySelector(`.ann.sel`)
  if (!el) return null
  const rect = el.getBoundingClientRect()
  const scale = page.dispW / rect.width * zoom.value // rough
  return { left: Math.max(10, rect.left) + 'px', top: Math.max(10, rect.top - 42) + 'px' }
})

// ---- File loading ----
function onFilePicked(e) { const f = e.target.files?.[0]; if (f) loadFile(f) }
function onDrop(e) { const f = e.dataTransfer?.files?.[0]; if (f) loadFile(f) }

async function loadFile(file) {
  fileName.value = file.name
  loading.value = true
  loadingMsg.value = '读取文件...'
  loadError.value = null
  try {
    // Read file as base64
    pdfBase64.value = await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result.split(',')[1])
      reader.onerror = reject
      reader.readAsDataURL(file)
    })
    await loadPreview()
  } catch (e) {
    console.error(e)
    loadError.value = e.message || '文件读取失败'
    loading.value = false
  }
}

async function loadPreview() {
  if (!pdfBase64.value) return
  loading.value = true
  loadingMsg.value = '生成预览...'
  loadError.value = null
  try {
    const result = await previewPdf(pdfBase64.value)
    const list = Array.isArray(result) ? result : (result.pages || [])
    // Clean up old blob URLs
    for (const p of pages.value) {
      if (p.blobUrl) URL.revokeObjectURL(p.blobUrl)
    }
    pages.value = list.map(p => {
      // Create blob URL from per-page PDF base64 → browser renders vector PDF in iframe
      const bytes = Uint8Array.from(atob(p.pdfBase64), c => c.charCodeAt(0))
      const blob = new Blob([bytes], { type: 'application/pdf' })
      const blobUrl = URL.createObjectURL(blob)
      return {
        blobUrl,
        w: p.width,     // PDF points (1/72 inch)
        h: p.height,
        dispW: p.width * 0.75,    // display at 75% for fit
        dispH: p.height * 0.75,
      }
    })
    annotations.value = []
    undos.value = []
    redos.value = []
    selAnn.value = null
    editAnn.value = null
    curPage.value = 0
    loading.value = false
  } catch (e) {
    console.error(e)
    loadError.value = e.message || '预览生成失败'
    loading.value = false
  }
}

async function loadBase64(b64) {
  pdfBase64.value = b64
  fileName.value = '打印合集.pdf'
  await loadPreview()
}

function retryLoad() {
  if (pdfBase64.value) loadPreview()
  else { loadError.value = null; fileInput.value?.click() }
}

// ---- Zoom / Page ----
function zoomIn() { zoom.value = Math.min(3.0, +(zoom.value + 0.1).toFixed(1)) }
function zoomOut() { zoom.value = Math.max(0.25, +(zoom.value - 0.1).toFixed(1)) }
function onWheel(e) {
  if (e.ctrlKey || e.metaKey) {
    zoom.value = Math.max(0.25, Math.min(3.0, +(zoom.value + (e.deltaY > 0 ? -0.1 : 0.1)).toFixed(1)))
  }
}
function goPage(d) {
  const np = curPage.value + d
  if (np >= 0 && np < pages.value.length) curPage.value = np
}

// ---- Add text (Edge-style click) ----
function onPageClick(e, pi) {
  if (tool.value !== 'text') return
  curPage.value = pi
  const page = pages.value[pi]
  const el = e.currentTarget
  const rect = el.getBoundingClientRect()
  const sc = zoom.value
  const x = (e.clientX - rect.left) / sc / (page.dispW / page.w) * 72 / 96 * 25.4 // mm -> later
  const y = (e.clientY - rect.top) / sc / (page.dispH / page.h) * 72 / 96 * 25.4

  // Simple px coordinates relative to displayed page
  const px = (e.clientX - rect.left) / sc
  const py = (e.clientY - rect.top) / sc

  pushUndo()
  const ann = {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 5),
    pageIndex: pi, x: px, y: py,
    text: '', fontSize: 14, color: '#333333'
  }
  annotations.value.push(ann)
  selAnn.value = ann.id
  editAnn.value = ann.id
  nextTick(() => {
    const eds = document.querySelectorAll('.ann-edit')
    const last = eds[eds.length - 1]
    if (last) last.focus()
  })
}

// ---- Annotation interaction ----
function onAnnDown(e, ann, pi) {
  curPage.value = pi
  selAnn.value = ann.id
  if (editAnn.value === ann.id) return
  drag.value = { ann, sx: e.clientX, sy: e.clientY, ox: ann.x, oy: ann.y }
  e.preventDefault()
}

function selectAnn(id) { if (!editAnn.value) selAnn.value = id }

function startEdit(ann, pi) {
  editAnn.value = ann.id
  selAnn.value = ann.id
  curPage.value = pi
  nextTick(() => {
    const eds = document.querySelectorAll('.ann-edit')
    for (const ed of eds) { if (ed.textContent === ann.text) { ed.focus(); return } }
  })
}
function onEditInput(e, ann) { ann.text = e.target.textContent }
function finishEdit(ann) {
  editAnn.value = null
  if (!ann.text.trim()) { annotations.value = annotations.value.filter(a => a.id !== ann.id); selAnn.value = null }
}

// Drag
function onMMove(e) {
  if (!drag.value) return
  const d = drag.value
  d.ann.x = d.ox + (e.clientX - d.sx) / zoom.value
  d.ann.y = d.oy + (e.clientY - d.sy) / zoom.value
}
function onMUp() { drag.value = null }

// Properties
function changeSize(d) { const a = selAnnData.value; if (a) a.fontSize = Math.max(8, Math.min(72, a.fontSize + d)) }
function setColor(c) { const a = selAnnData.value; if (a) a.color = c }
function delSel() {
  if (!selAnn.value) return
  pushUndo()
  annotations.value = annotations.value.filter(a => a.id !== selAnn.value)
  selAnn.value = null
}

// Undo/Redo
function pushUndo() {
  undos.value.push(JSON.parse(JSON.stringify(annotations.value)))
  redos.value = []
  if (undos.value.length > 50) undos.value.shift()
}
function undo() {
  if (!canUndo.value) return
  redos.value.push(JSON.parse(JSON.stringify(annotations.value)))
  annotations.value = undos.value.pop()
  selAnn.value = null; editAnn.value = null
}
function redo() {
  if (!canRedo.value) return
  undos.value.push(JSON.parse(JSON.stringify(annotations.value)))
  annotations.value = redos.value.pop()
  selAnn.value = null; editAnn.value = null
}

// Save
async function doSave() {
  if (!pdfBase64.value) return
  saving.value = true
  try {
    const anns = annotations.value.map(a => ({
      pageIndex: a.pageIndex,
      x: a.x / pages.value[a.pageIndex].dispW * pages.value[a.pageIndex].w * 25.4 / 72,  // px → mm
      y: a.y / pages.value[a.pageIndex].dispH * pages.value[a.pageIndex].h * 25.4 / 72,
      html: a.text, fontSize: a.fontSize, color: a.color
    }))
    const blob = await annotatePdf(pdfBase64.value, anns)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = (fileName.value || '文档').replace('.pdf','') + '_已标注.pdf'
    document.body.appendChild(a); a.click(); document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) { alert('保存失败: ' + e.message) }
  finally { saving.value = false }
}

// Keyboard
function onKey(e) {
  if ((e.ctrlKey||e.metaKey) && e.key==='z') { e.preventDefault(); undo(); return }
  if ((e.ctrlKey||e.metaKey) && e.key==='y') { e.preventDefault(); redo(); return }
  if (e.key==='t' && document.activeElement===document.body) tool.value = tool.value==='text'?'select':'text'
  if ((e.key==='Delete'||e.key==='Backspace') && selAnn.value && !editAnn.value) {
    if (document.activeElement===document.body || document.activeElement?.closest('.main')) { e.preventDefault(); delSel() }
  }
  if (e.key==='Escape') { selAnn.value=null; editAnn.value=null }
  if (selAnn.value && !editAnn.value && selAnnData.value) {
    const a = selAnnData.value; const d = e.shiftKey?10:1
    if (e.key==='ArrowLeft'){e.preventDefault();a.x-=d}
    if (e.key==='ArrowRight'){e.preventDefault();a.x+=d}
    if (e.key==='ArrowUp'){e.preventDefault();a.y-=d}
    if (e.key==='ArrowDown'){e.preventDefault();a.y+=d}
  }
}

// Check for PDF from print queue
function checkQueue() {
  try {
    const d = localStorage.getItem('mf-workshop-pdf')
    if (d) {
      const o = JSON.parse(d)
      localStorage.removeItem('mf-workshop-pdf')
      if (o.pdfBase64 && o.title) { fileName.value = o.title; loadBase64(o.pdfBase64) }
    }
  } catch {}
}

onMounted(() => {
  document.addEventListener('mousemove', onMMove)
  document.addEventListener('mouseup', onMUp)
  document.addEventListener('keydown', onKey)
  checkQueue()
})
onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onMMove)
  document.removeEventListener('mouseup', onMUp)
  document.removeEventListener('keydown', onKey)
})
</script>

<style scoped>
.editor { display:flex; flex-direction:column; height:100%; background:#323639; color:#e8eaed; }

/* Toolbar */
.tb { display:flex; align-items:center; gap:8px; padding:6px 12px; background:#35373a; border-bottom:1px solid #202124; flex-shrink:0; min-height:44px; z-index:10; }
.tb-l { display:flex; align-items:center; gap:8px; min-width:140px; }
.tb-c { display:flex; align-items:center; gap:4px; flex:1; justify-content:center; }
.tb-r { display:flex; align-items:center; gap:4px; }
.tb-back { color:#9aa0a6; text-decoration:none; padding:4px; border-radius:4px; display:flex; }
.tb-back:hover { background:rgba(255,255,255,.08); }
.tb-title { font-size:14px; max-width:300px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.tb-title.dim { color:#9aa0a6; }
.tb-btn { display:flex; align-items:center; gap:3px; padding:4px 8px; background:transparent; border:1px solid transparent; border-radius:4px; color:#9aa0a6; font-size:12px; cursor:pointer; transition:.1s; }
.tb-btn:hover { background:rgba(255,255,255,.06); color:#e8eaed; }
.tb-btn.on { background:rgba(138,180,248,.2); color:#8ab4f8; }
.tb-btn:disabled { opacity:.3; cursor:default; }
.tb-pn { font-size:13px; min-width:60px; text-align:center; font-variant-numeric:tabular-nums; }
.tb-zp { font-size:13px; min-width:40px; text-align:center; font-variant-numeric:tabular-nums; }
.tb-div { color:#5f6368; margin:0 2px; font-size:13px; }
.tb-save { padding:5px 16px; border:none; border-radius:4px; background:#8ab4f8; color:#202124; font-size:13px; font-weight:500; cursor:pointer; margin-left:8px; }
.tb-save:hover { background:#aecbfa; }
.tb-save:disabled { opacity:.5; cursor:default; }

/* Main */
.main { flex:1; overflow:auto; display:flex; align-items:flex-start; justify-content:center; }
.status { margin-top:100px; text-align:center; color:#9aa0a6; font-size:14px; }
.spinner { display:inline-block; width:32px; height:32px; border:3px solid #5f6368; border-top-color:#8ab4f8; border-radius:50%; animation:spin .8s linear infinite; margin-bottom:12px; }
@keyframes spin { to { transform:rotate(360deg); } }

.dropzone { margin-top:100px; text-align:center; cursor:pointer; padding:60px 80px; border:2px dashed #5f6368; border-radius:12px; transition:.15s; }
.dropzone:hover { border-color:#8ab4f8; background:rgba(138,180,248,.04); }
.dz-icon { font-size:56px; margin-bottom:16px; }
.dz-title { font-size:16px; margin-bottom:6px; }
.dz-hint { font-size:13px; color:#9aa0a6; line-height:1.6; }
.dz-or { color:#5f6368; margin:16px 0 12px; font-size:13px; }

/* Pages */
.pages { display:flex; flex-direction:column; align-items:center; gap:16px; padding:16px 20px 40px; }
.page { position:relative; background:#fff; box-shadow:0 1px 3px rgba(0,0,0,.3),0 4px 8px rgba(0,0,0,.2); flex-shrink:0; cursor:crosshair; }
.page-img { display:block; width:100%; height:100%; pointer-events:none; user-select:none; }

/* Annotations */
.ann { position:absolute; padding:1px 4px; border:1px solid transparent; cursor:move; user-select:none; white-space:pre-wrap; word-break:break-word; min-width:1em; line-height:1.35; font-family:"SimSun","Microsoft YaHei",sans-serif; }
.ann:hover { border-color:#8ab4f8; }
.ann.sel { border-color:#8ab4f8!important; outline:1px solid rgba(138,180,248,.4); }
.ann.edit { cursor:text; border:1px dashed #8ab4f8!important; background:rgba(255,255,255,.95); z-index:5; min-width:4em; }
.ann-edit { outline:none; min-width:4em; min-height:1.2em; white-space:pre-wrap; word-break:break-word; }

/* Popup */
.apop { position:fixed; z-index:100; display:flex; align-items:center; gap:4px; padding:5px 10px; background:#fff; color:#202124; border-radius:6px; box-shadow:0 2px 12px rgba(0,0,0,.25),0 0 0 1px rgba(0,0,0,.08); font-size:12px; }
.apop button { width:24px; height:24px; border:1px solid #dadce0; border-radius:4px; background:#fff; color:#202124; cursor:pointer; font-size:11px; font-weight:600; display:flex; align-items:center; justify-content:center; }
.apop button:hover { background:#f1f3f4; }
.ap-sz { font-size:12px; font-weight:600; min-width:28px; text-align:center; }
.ap-div { color:#dadce0; margin:0 2px; }
.ap-c { width:16px; height:16px; border-radius:50%; border:2px solid transparent; cursor:pointer; }
.ap-c:hover { border-color:#8ab4f8; }
.ap-c.on { border-color:#1a73e8; }
.ap-pick { width:16px; height:16px; border:none; border-radius:50%; cursor:pointer; padding:0; background:transparent; }
.ap-del { border:none!important; font-size:14px!important; }
.ap-del:hover { background:#fce8e6!important; color:#d93025!important; }
</style>
