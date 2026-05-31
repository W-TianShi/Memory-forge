<script>
export default { name: 'WordMemory' }
</script>
<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'

import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { useWords } from '../composables/useWords.js'
import { useVisibility } from '../composables/useVisibility.js'
import Sidebar from '../components/Sidebar.vue'
import PageBar from '../components/PageBar.vue'

const contentRef = ref(null)

const {
  words, currentPage, totalPages, pageWords, columns,
  globalIndex, displayIndex, undoStack,
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

const searching = ref(false)

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
    const res = await fetch(`/api/word/batch?words=${encodeURIComponent(wordList)}`)
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

async function exportPdf() {
  syncFromDOM()
  const el = contentRef.value
  if (!el) return
  const savedPage = currentPage.value

  try {
    const pdf = new jsPDF('p', 'mm', 'a4')
    const pdfW = pdf.internal.pageSize.getWidth()
    const pdfH = pdf.internal.pageSize.getHeight()
    const margin = 10

    for (let p = 0; p < totalPages.value; p++) {
      currentPage.value = p
      await nextTick()
      await new Promise(r => requestAnimationFrame(r))

      if (p > 0) pdf.addPage()
      const canvas = await html2canvas(el, { scale: 2, useCORS: true, background: '#fff' })
      const w = pdfW - margin * 2
      const h = pdfH - margin * 2
      const scale = Math.min(w / canvas.width, h / canvas.height)
      pdf.addImage(canvas.toDataURL('image/jpeg'), 'JPEG', margin, margin, canvas.width * scale, canvas.height * scale)
    }

    const d = new Date()
    const ts = `${d.getFullYear()}${String(d.getMonth()+1).padStart(2,'0')}${String(d.getDate()).padStart(2,'0')}-${String(d.getHours()).padStart(2,'0')}${String(d.getMinutes()).padStart(2,'0')}`
    pdf.save(`单词记忆-${ts}.pdf`)
  } catch (e) {
    console.error('PDF导出错误:', e)
  } finally {
    currentPage.value = savedPage
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
})

onUnmounted(() => {
  window.removeEventListener('keydown', onWindowKeydown)
})
</script>

<template>
  <div class="app-wrap">
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
    />

    <div class="main-area">
      <div v-if="searching" class="progress-wrap">
        <div class="progress-track">
          <div class="progress-fill progress-indeterminate"></div>
        </div>
        <span class="progress-label">查询中...</span>
      </div>
      <div class="content-area" ref="contentRef">
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
  min-height: 100vh;
  background: #e8e8e8;
  display: flex;
  flex-direction: row;
  justify-content: center;
  padding: 10px 10px 20px;
  gap: 10px;
}

.main-area {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.progress-wrap {
  width: 210mm;
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
  width: 210mm;
  min-height: 297mm;
  padding: 12mm 14mm;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,.15);
}

.grid-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2mm;
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
  padding: 2px 5px;
  background: #f0f5f9;
  border: 1px solid #ddd;
  font-size: 8pt;
  font-weight: bold;
  color: #555;
}

.table-row {
  display: grid;
  grid-template-columns: 8mm 32mm 1fr;
  align-items: start;
  gap: 4px;
  padding: 2px 5px;
  border: 1px solid #ddd;
  border-top: none;
  height: 10.5mm;
  overflow: hidden;
}

.index {
  font-size: 8pt;
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
  font-size: 9pt; font-weight: 600; color: #222;
  border: none; background: transparent;
  padding: 0 2px; outline: none; width: 100%;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.phonetic {
  font-size: 6.5pt; color: #666;
  border: none; background: transparent;
  padding: 0 2px; outline: none; width: 100%;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.meaning-text {
  border: none; background: transparent;
  font-size: 7.5pt;
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
