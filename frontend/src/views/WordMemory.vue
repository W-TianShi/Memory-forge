<script>
export default { name: 'WordMemory' }
</script>
<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'

import { useWords } from '../composables/useWords.js'
import { useVisibility } from '../composables/useVisibility.js'
import { listSheets, saveSheet, getSheet, deleteSheet } from '../api/wordSheets.js'
import { exportPdf as exportPdfApi } from '../api/pdf.js'
import { getUsername } from '../api/auth.js'
import { useToast } from '../composables/useToast.js'
import ToastOverlay from '../components/ToastOverlay.vue'
import Sidebar from '../components/Sidebar.vue'
import PageBar from '../components/PageBar.vue'

const contentRef = ref(null)

const {
  words, currentPage, totalPages, pageWords, columns,
  globalIndex, displayIndex, undoStack, columnCount,
  undo, syncFromDOM,
  onWordKeydown, addWord, removeLastWord,
  addPage, deletePage, prevPage, nextPage,
  batchAddWords
} = useWords(contentRef)

const {
  wordHidden, phoneticHidden, meaningHidden,
  toggleWordHidden, togglePhoneticHidden, toggleMeaningHidden,
  resetAll
} = useVisibility(words)

const timestampVisible = ref(false)
const timestampText = ref('')
const todayStamp = computed(() => {
  const d = new Date()
  return `${d.getMonth() + 1}.${d.getDate()}`
})
function toggleTimestamp() {
  timestampVisible.value = !timestampVisible.value
  if (timestampVisible.value) timestampText.value = todayStamp.value
}

function toggleColumns() {
  syncFromDOM()
  columnCount.value = columnCount.value === 2 ? 3 : 2
  words.value.forEach((w, i) => { w.col = i % columnCount.value })
}

function onWindowKeydown(e) {
  if (e.ctrlKey && e.key === 'z' && !e.shiftKey) {
    const sel = window.getSelection()
    if (sel && sel.anchorNode && contentRef.value?.contains(sel.anchorNode)) {
      if (undoStack.value.length > 0) {
        e.preventDefault()
        undo()
      }
    }
  }
}

const showImport = ref(false)
const importText = ref('')

function batchImport() {
  importText.value = ''
  showImport.value = true
}

function doImport() {
  const raw = importText.value.trim()
  if (!raw) { showImport.value = false; return }
  const wordList = raw
    .split(/[\s,，、;；\n\r]+/)
    .map(w => w.trim())
    .filter(w => w.length > 0)
  batchAddWords(wordList)
  showImport.value = false
}

const leftCollapsed = ref(false)
const searching = ref(false)
const sheetList = ref([])
const currentSheetId = ref(null)

const { visible: toastVisible, message: toastMsg, type: toastType, show: showToast } = useToast()

async function refreshSheetList() {
  if (!getUsername()) return
  sheetList.value = await listSheets()
  // auto-load last sheet
  const lastId = localStorage.getItem('mf_last_sheet')
  if (lastId && sheetList.value.find(s => s.id == lastId) && !currentSheetId.value) {
    pickSheet(Number(lastId))
  }
}

async function newSheet() {
  if (!getUsername()) { alert('请先登录'); return }
  const title = prompt('请输入单词纸名称', '单词纸 ' + new Date().toLocaleDateString())
  if (!title) return
  const sheet = await saveSheet({ title, data: '[]', colCount: columnCount.value })
  currentSheetId.value = sheet.id
  columnCount.value = 2
  words.value = Array.from({ length: 28 }, (_, i) => ({ word: '', phonetic: '', meaning: '', originalIndex: Date.now() + i, col: i % 2 }))
  refreshSheetList()
}

async function doSaveSheet() {
  if (!getUsername()) { alert('请先登录'); return }
  if (!currentSheetId.value) { newSheet(); return }
  syncFromDOM()
  const title = sheetList.value.find(s => s.id === currentSheetId.value)?.title || '单词纸'
  const data = JSON.stringify(words.value.map(w => ({ word: w.word, phonetic: w.phonetic, meaning: w.meaning })))
  await saveSheet({ id: currentSheetId.value, title, data, colCount: columnCount.value })
  localStorage.setItem('mf_last_sheet', currentSheetId.value)
  refreshSheetList()
}

async function doDeleteSheet(id) {
  if (!confirm('确定要删除这张单词纸吗？数据无法恢复。')) return
  await deleteSheet(id)
  if (currentSheetId.value === id) { currentSheetId.value = null; words.value = words.value.map(w => ({...w, word:'', phonetic:'', meaning:''})) }
  refreshSheetList()
}

async function pickSheet(id) {
  syncFromDOM()
  await doSaveSheetSilent()
  const sheet = await getSheet(id)
  columnCount.value = sheet.colCount || 2
  const data = JSON.parse(sheet.data)
  words.value = data.map((w, i) => ({ word: w.word || '', phonetic: w.phonetic || '', meaning: w.meaning || '', originalIndex: Date.now() + i, col: i % columnCount.value }))
  currentSheetId.value = sheet.id
  localStorage.setItem('mf_last_sheet', id)
  currentPage.value = 0
}

async function doSaveSheetSilent() {
  if (!currentSheetId.value || !getUsername()) return
  syncFromDOM()
  const title = sheetList.value.find(s => s.id === currentSheetId.value)?.title || '单词纸'
  const data = JSON.stringify(words.value.map(w => ({ word: w.word, phonetic: w.phonetic, meaning: w.meaning })))
  await saveSheet({ id: currentSheetId.value, title, data, colCount: columnCount.value })
}

async function searchAll() {
  syncFromDOM()
  const pending = []
  for (let i = 0; i < words.value.length; i++) {
    const w = words.value[i].word.trim()
    if (!w) continue
    const hasPhonetic = words.value[i].phonetic && words.value[i].phonetic.trim()
    const hasMeaning = words.value[i].meaning && words.value[i].meaning.trim()
    if (hasPhonetic && hasMeaning) continue
    pending.push({ i, w })
  }

  if (pending.length === 0) return

  searching.value = true
  const wordList = pending.map(p => p.w).join(',')

  try {
    const res = await fetch('/api/word/batch', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ words: wordList }) })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = await res.json()
    data.forEach((item, idx) => {
      const { i } = pending[idx]
      words.value[i].phonetic = item.en_pronunciation || ''
      words.value[i].meaning = item.desc || ''
    })
    nextTick(() => syncDataToDOM())
  } catch (e) {
    alert('查询失败：' + e.message)
  } finally {
    searching.value = false
  }
}

function escapeHtml(s) {
  if (!s) return ''
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

function buildExportHtml() {
  const cols = columnCount.value
  const allWords = words.value
  const ROWS_PER_PAGE = 25
  const wordsPerPage = ROWS_PER_PAGE * cols

  const pages = []
  for (let i = 0; i < allWords.length; i += wordsPerPage) {
    pages.push(allWords.slice(i, i + wordsPerPage))
  }

  if (pages.length === 0) {
    pages.push([])
  }

  const colsClass = cols === 3 ? 'cols-3' : ''
  let pagesHtml = ''

  pages.forEach((pageWords, pageIdx) => {
    const colWords = []
    for (let c = 0; c < cols; c++) {
      colWords.push(pageWords.filter(w => w.col === c))
    }

    let tableHtml = '<div class="grid-container">'
    for (let c = 0; c < cols; c++) {
      tableHtml += '<div class="table-column">'
      tableHtml += '<div class="table-header"><div>序号</div><div>单词 / 音标</div><div>释义</div></div>'
      colWords[c].forEach((w) => {
        const globalIdx = pageWords.findIndex(pw => pw.originalIndex === w.originalIndex)
        const idx = String(globalIdx + 1).padStart(2, '0')
        tableHtml += '<div class="table-row">'
        tableHtml += `<div class="index">${idx}</div>`
        tableHtml += '<div class="word-section">'
        tableHtml += `<div class="word">${escapeHtml(w.word)}</div>`
        tableHtml += `<div class="phonetic">${escapeHtml(w.phonetic)}</div>`
        tableHtml += '</div>'
        tableHtml += `<div class="meaning-text">${escapeHtml(w.meaning)}</div>`
        tableHtml += '</div>'
      })
      tableHtml += '</div>'
    }
    tableHtml += '</div>'

    const isLast = pageIdx === pages.length - 1
    let tsHtml = ''
    if (isLast && timestampVisible.value && timestampText.value) {
      tsHtml = `<div class="timestamp-stamp">${escapeHtml(timestampText.value)}</div>`
    }

    const breakClass = isLast ? '' : ' page-break'
    pagesHtml += `<div class="content-area ${colsClass}${breakClass}">\n${tableHtml}\n${tsHtml}\n</div>\n`
  })

  return '<!DOCTYPE html>\n<html>\n<head>\n<meta charset="utf-8">\n<style>\n'
    + '@page { size: A4; margin: 0; }\n'
    + '*, *::before, *::after { box-sizing: border-box; }\n'
    + 'html, body { margin: 0; padding: 0; font-family: "SimSun", "Arial", sans-serif; color: #000; background: #fff; }\n'
    + '.content-area { width: 210mm; height: 297mm; padding: 12mm 14mm; background: #fff; position: relative; overflow: hidden; }\n'
    + '.page-break { page-break-after: always; }\n'
    + '.grid-container { display: grid; grid-template-columns: 1fr 1fr; gap: 2mm; }\n'
    + '.cols-3 .grid-container { grid-template-columns: 1fr 1fr 1fr; gap: 1.5mm; }\n'
    + '.table-column { display: flex; flex-direction: column; }\n'
    + '.cols-3 .table-column { min-width: 0; }\n'
    + '.table-header { display: grid; grid-template-columns: 8mm 32mm 1fr; gap: 4px; padding: 3px 4px; background: #f0f5f9; border: 1px solid #ddd; font-size: 7.5pt; font-weight: 600; color: #555; white-space: nowrap; }\n'
    + '.cols-3 .table-header { grid-template-columns: 5mm 32mm 1fr; gap: 2px; padding: 1px 2px; font-size: 6pt; line-height: 1.2; white-space: nowrap; }\n'
    + '.table-row { display: grid; grid-template-columns: 8mm 32mm 1fr; align-items: start; gap: 4px; padding: 3px 6px; border: 1px solid #ddd; border-top: none; height: 10.5mm; overflow: hidden; page-break-inside: avoid; }\n'
    + '.cols-3 .table-row { grid-template-columns: 5mm 32mm 1fr; gap: 2px; padding: 2px 3px; }\n'
    + '.cols-3 .word-section { font-size: 9.5pt; }\n'
    + '.index { font-size: 7.5pt; color: #666; text-align: center; padding-top: 1px; }\n'
    + '.word-section { display: flex; flex-direction: column; gap: 0; overflow: hidden; }\n'
    + '.word { font-family: "Arial", "Helvetica", "SimHei", "黑体", sans-serif; font-size: 9pt; font-weight: bold; color: #000; border: none; background: transparent; padding: 0 2px; outline: none; width: 100%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n'
    + '.phonetic { font-size: 7.5pt; color: #888; border: none; background: transparent; padding: 0 2px; outline: none; width: 100%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n'
    + '.meaning-text { border: none; background: transparent; font-size: 7.5pt; color: #555; padding: 0; width: 100%; line-height: 1.35; outline: none; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; word-break: break-all; }\n'
    + '.timestamp-stamp { position: absolute; bottom: 10mm; right: 14mm; text-align: right; font-size: 9pt; color: #aaa; font-family: Consolas, "Courier New", monospace; }\n'
    + '</style>\n</head>\n<body>\n'
    + pagesHtml
    + '</body>\n</html>'
}

const exportingPdf = ref(false)

async function exportPdf() {
  syncFromDOM()
  exportingPdf.value = true
  showToast('PDF 导出中...', 'loading')
  try {
    const html = buildExportHtml()
    const blob = await exportPdfApi(html, false, null, null)
    const d = new Date()
    const ts = `${d.getFullYear()}${String(d.getMonth()+1).padStart(2,'0')}${String(d.getDate()).padStart(2,'0')}-${String(d.getHours()).padStart(2,'0')}${String(d.getMinutes()).padStart(2,'0')}`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `单词记忆-${ts}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    showToast('PDF 导出成功', 'success')
  } catch (e) {
    console.error('PDF导出错误:', e)
    showToast('导出失败：' + e.message, 'error')
  } finally {
    exportingPdf.value = false
  }
}

function syncDataToDOM() {
  if (!contentRef.value) return
  contentRef.value.querySelectorAll('.word').forEach(el => {
    const oid = Number(el.dataset.oid)
    const w = words.value.find(w => w.originalIndex === oid)
    if (w && el.textContent !== w.word) el.textContent = w.word
  })
  contentRef.value.querySelectorAll('.phonetic').forEach(el => {
    const oid = Number(el.dataset.oid)
    const w = words.value.find(w => w.originalIndex === oid)
    if (w && el.textContent !== w.phonetic) el.textContent = w.phonetic
  })
  contentRef.value.querySelectorAll('.meaning-text').forEach(el => {
    const oid = Number(el.dataset.oid)
    const w = words.value.find(w => w.originalIndex === oid)
    if (w && el.textContent !== w.meaning) el.textContent = w.meaning
  })
}

watch(pageWords, () => nextTick(() => syncDataToDOM()), { deep: true })

onMounted(() => {
  window.addEventListener('keydown', onWindowKeydown)
  nextTick(() => syncDataToDOM())
  refreshSheetList()
})

onUnmounted(() => {
  window.removeEventListener('keydown', onWindowKeydown)
})
</script>

<template>
  <div class="app-wrap">
    <div class="left-panel" :class="{ collapsed: leftCollapsed }">
      <div class="left-panel-head">
        <button class="lph-btn" @click="newSheet">+ 新建</button>
        <button class="lph-btn-save" @click="doSaveSheet">保存</button>
      </div>
      <div class="left-panel-list">
        <div v-for="s in sheetList" :key="s.id" class="left-panel-item"
             :class="{ active: s.id === currentSheetId }"
             @click="pickSheet(s.id)">
          <span class="lpi-title">{{ s.title }}</span>
          <span class="lpi-del" @click.stop="doDeleteSheet(s.id)">×</span>
        </div>
        <div v-if="sheetList.length===0" class="lpi-empty">点击新建创建单词纸</div>
      </div>
    </div>

    <div class="toggle-strip" :class="{ collapsed: leftCollapsed }" @click="leftCollapsed = !leftCollapsed" :title="leftCollapsed ? '展开列表' : '收起列表'">
      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <polyline v-if="leftCollapsed" points="9,18 15,12 9,6"></polyline>
        <polyline v-else points="15,18 9,12 15,6"></polyline>
      </svg>
    </div>

    <div class="main-area">
      <div v-if="searching" class="progress-wrap">
        <div class="progress-track">
          <div class="progress-fill progress-indeterminate"></div>
        </div>
        <span class="progress-label">查询中...</span>
      </div>
      <div v-if="exportingPdf" class="progress-wrap">
        <div class="progress-track">
          <div class="progress-fill progress-indeterminate"></div>
        </div>
        <span class="progress-label">PDF 导出中，请稍候...</span>
      </div>
      <div class="content-area" ref="contentRef" :class="{ 'cols-3': columnCount === 3 }">
        <div class="grid-container">
          <div class="table-column" v-for="(colData, colIdx) in columns" :key="colIdx">
            <div class="table-header">
              <div>序号</div>
              <div>单词 / 音标</div>
              <div>释义</div>
            </div>
            <div class="table-row" v-for="item in colData" :key="item.originalIndex">
              <div class="index">{{ displayIndex(item) }}</div>
              <div class="word-section">
                <div class="word" :data-oid="item.originalIndex" contenteditable="true" @input="e => item.word = e.target.textContent" @keydown="e => onWordKeydown($event, item)"></div>
                <div class="phonetic" :data-oid="item.originalIndex" contenteditable="true" @input="e => item.phonetic = e.target.textContent"></div>
              </div>
              <div
                class="meaning-text"
                :data-oid="item.originalIndex"
                contenteditable="true"
                @input="e => item.meaning = e.target.textContent"
                :title="item.meaning"
              ></div>
            </div>
          </div>
        </div>
        <div v-if="timestampVisible" class="timestamp-stamp" contenteditable="true" @input="e => timestampText = e.target.textContent">{{ timestampText }}</div>
      </div>

      <PageBar
        :currentPage="currentPage"
        :totalPages="totalPages"
        @prevPage="prevPage"
        @nextPage="nextPage"
        @addPage="addPage"
        @deletePage="deletePage"
      />
    </div>

    <div v-if="showImport" class="modal-overlay" @click.self="showImport = false">
      <div class="modal-box">
        <div class="modal-title">批量导入单词</div>
        <div class="modal-hint">粘贴单词列表，支持空格、逗号、顿号、分号、换行等分隔</div>
        <textarea
          v-model="importText"
          class="modal-textarea"
          placeholder="例如：abandon ability access account achieve"
          rows="8"
          @keydown.escape="showImport = false"
        ></textarea>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showImport = false">取消</button>
          <button class="btn-confirm" @click="doImport">导入</button>
        </div>
      </div>
    </div>

    <Sidebar
      :wordHidden="wordHidden"
      :phoneticHidden="phoneticHidden"
      :meaningHidden="meaningHidden"
      :timestampVisible="timestampVisible"
      @searchAll="searchAll"
      @toggleWordHidden="toggleWordHidden"
      @togglePhoneticHidden="togglePhoneticHidden"
      @toggleMeaningHidden="toggleMeaningHidden"
      @resetAll="resetAll"
      @addWord="addWord"
      @removeLastWord="removeLastWord"
      @batchImport="batchImport"
      @exportPdf="exportPdf"
      @toggleTimestamp="toggleTimestamp"
      @toggleColumns="toggleColumns"
    />

    <ToastOverlay :visible="toastVisible" :message="toastMsg" :type="toastType" />
  </div>
</template>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: "SimSun", "Microsoft YaHei", sans-serif;
}
.app-wrap {
  height: 100%;
  background: #f0f2f5;
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: stretch;
  padding: 10px;
  gap: 0;
  overflow: hidden;
  scrollbar-width: none;
}
.app-wrap::-webkit-scrollbar { width: 0; height: 0; }

.main-area {
  width: calc(210mm - 2px);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: none;
  padding-bottom: 40px;
  background: #fff;
  box-shadow: 0 4px 24px rgba(0,0,0,.1), 0 1px 4px rgba(0,0,0,.06);
}
.main-area::-webkit-scrollbar { width: 0; height: 0; }

.progress-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.progress-track {
  flex: 1;
  height: 4px;
  background: #e0e0e0;
  border-radius: 2px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: #409eff;
  border-radius: 2px;
}
.progress-indeterminate {
  width: 30%;
  animation: progress-scan 1.2s ease-in-out infinite;
}
@keyframes progress-scan {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(430%); }
}
.progress-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.content-area {
  width: 100%;
  min-height: 297mm;
  padding: 12mm 14mm;
}

.grid-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2mm;
}
.cols-3 .grid-container {
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1.5mm;
}

.table-column {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.table-header {
  display: grid;
  grid-template-columns: 8mm 32mm 1fr;
  gap: 4px;
  padding: 3px 4px;
  background: #f0f5f9;
  border: 1px solid #ddd;
  font-size: 7.5pt;
  font-weight: 600;
  color: #555;
  white-space: nowrap;
}

.table-row {
  display: grid;
  grid-template-columns: 8mm 32mm 1fr;
  align-items: start;
  gap: 4px;
  padding: 3px 6px;
  border: 1px solid #ddd;
  border-top: none;
  height: 10.5mm;
  overflow: hidden;
}

.cols-3 .table-column { min-width: 0; }
.cols-3 .table-header { grid-template-columns: 5mm 32mm 1fr; gap: 2px; padding: 1px 2px; font-size: 6pt; line-height: 1.2; white-space: nowrap; }
.cols-3 .table-row { grid-template-columns: 5mm 32mm 1fr; gap: 2px; padding: 2px 3px; }
.cols-3 .word-section { font-size: 9.5pt; }

.index {
  font-size: 7.5pt;
  color: #666;
  text-align: center;
  padding-top: 1px;
}

.word-section {
  display: flex;
  flex-direction: column;
  gap: 0;
  overflow: hidden;
}

.word {
  font-family: "Arial", "Helvetica", "SimHei", "黑体", sans-serif;
  font-size: 9pt; font-weight: bold; color: #000;
  border: none; background: transparent;
  padding: 0 2px; outline: none; width: 100%;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.phonetic {
  font-size: 7.5pt; color: #888;
  border: none; background: transparent;
  padding: 0 2px; outline: none; width: 100%;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.meaning-text {
  border: none; background: transparent;
  font-size: 7.5pt;
  color: #555;
  padding: 0;
  width: 100%;
  line-height: 1.35;
  outline: none;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-all;
}
.meaning-text:focus {
  -webkit-line-clamp: unset;
  line-clamp: unset;
  display: block;
  overflow: auto;
  background: #fff;
  z-index: 1;
  position: relative;
}

.modal-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,.4);
  display: flex; align-items: center; justify-content: center;
}
.modal-box {
  background: #fff; border-radius: 12px; padding: 24px;
  width: 480px; max-width: 90vw;
  box-shadow: 0 8px 32px rgba(0,0,0,.2);
}
.modal-title { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.modal-hint { font-size: 12px; color: #999; margin-bottom: 12px; }
.modal-textarea {
  width: 100%; padding: 10px 12px;
  border: 1px solid #dcdfe6; border-radius: 8px;
  font-size: 13px; line-height: 1.6; resize: vertical;
  outline: none;
}
.modal-textarea:focus { border-color: #409eff; }
.modal-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 12px; }
.btn-cancel, .btn-confirm {
  padding: 8px 20px; border-radius: 6px; font-size: 13px;
  border: none; cursor: pointer;
}
.btn-cancel { background: #f0f0f0; color: #666; }
.btn-cancel:hover { background: #e0e0e0; }
.btn-confirm { background: #409eff; color: #fff; }
.btn-confirm:hover { background: #337ecc; }

.left-panel {
  width: 230px;
  flex-shrink: 0;
  padding: 14px 10px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
  transition: width 0.2s, padding 0.2s, min-width 0.2s;
  overflow: hidden;
  position: relative;
}
.left-panel.collapsed { width: 0; min-width: 0; padding: 14px 0; box-shadow: none; border: none; border-radius: 0; overflow: hidden; }
.left-panel.collapsed .left-panel-head,
.left-panel.collapsed .left-panel-list { display: none; }
.left-panel-head {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 10px; padding: 0 4px;
  flex-shrink: 0;
  white-space: nowrap;
}
.lph-btn {
  padding: 4px 12px; font-size: 12px; font-weight: 500;
  border: none; border-radius: 6px;
  background: #409eff; color: #fff; cursor: pointer;
  transition: all 0.15s;
}
.lph-btn:hover { background: #337ecc; box-shadow: 0 2px 8px rgba(64,158,255,.3); }
.lph-btn-save {
  padding: 4px 12px; font-size: 12px; border: 1px solid #409eff; border-radius: 6px;
  background: #fff; color: #409eff; cursor: pointer;
  transition: all 0.15s;
}
.lph-btn-save:hover { background: #ecf5ff; }
.left-panel-list { flex: 1; overflow-y: auto; }
.left-panel-list::-webkit-scrollbar { width: 4px; }
.left-panel-list::-webkit-scrollbar-thumb { background: #d0d0d0; border-radius: 2px; }
.left-panel-item {
  padding: 7px 8px; margin-bottom: 2px; border-radius: 6px; cursor: pointer;
  font-size: 13px; color: #555; display: flex; align-items: center;
  transition: all 0.15s;
}
.left-panel-item:hover { background: #f5f7fa; }
.left-panel-item.active { background: #e8f4ff; color: #409eff; font-weight: 600; }
.lpi-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.lpi-date { color: #ccc; font-size: 10px; margin-left: 4px; flex-shrink: 0; }
.lpi-del { color: #ccc; font-size: 15px; flex-shrink: 0; margin-left: 4px; opacity: 0; transition: all .15s; cursor: pointer; }
.left-panel-item:hover .lpi-del { opacity: 1; }
.lpi-del:hover { color: #f56c6c; }
.lpi-empty { padding: 24px 0; text-align: center; color: #bbb; font-size: 13px; }

.toggle-strip {
  width: 10px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  color: #bbb;
  transition: width 0.2s;
}
.toggle-strip.collapsed { width: 5px; }
.toggle-strip:hover { color: #409eff; }
.toggle-strip svg { display: block; }

.timestamp-stamp {
  text-align: right;
  font-size: 12px;
  color: #aaa;
  font-family: 'Consolas', 'Courier New', monospace;
  cursor: text;
  user-select: none;
  padding: 8px 4px 4px;
  outline: none;
}
.timestamp-stamp:hover { background: #f0f0f0; }
.timestamp-stamp:focus { background: #fff; color: #333; }

@media print {
  .content-area { box-shadow: none; }
  .meaning-text { -webkit-line-clamp: unset; line-clamp: unset; display: block; overflow: visible; }
}

</style>
