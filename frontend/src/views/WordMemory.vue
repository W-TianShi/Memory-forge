<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

defineOptions({ name: 'WordMemory' })

import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { useWords } from '../composables/useWords.js'
import { useVisibility } from '../composables/useVisibility.js'
import Sidebar from '../components/Sidebar.vue'
import PageBar from '../components/PageBar.vue'

const contentRef = ref(null)
const indexOffset = ref(0)

const {
  words, currentPage, totalPages, pageWords, columns,
  globalIndex, displayIndex,
  undo, syncFromDOM,
  onWordKeydown, addWord, removeLastWord,
  addPage, deletePage, prevPage, nextPage
} = useWords(contentRef, indexOffset)

const {
  wordHidden, phoneticHidden, meaningHidden,
  toggleWordHidden, togglePhoneticHidden, toggleMeaningHidden,
  resetAll
} = useVisibility(words)

function onFirstIndexBlur(e) {
  const raw = e.target.textContent.trim()
  const n = parseInt(raw, 10)
  if (isNaN(n) || n < 1) {
    e.target.textContent = String(1 + indexOffset.value).padStart(2, '0')
    return
  }
  indexOffset.value = n - 1
}

function onWindowKeydown(e) {
  if (e.ctrlKey && e.key === 'z' && !e.shiftKey) {
    const sel = window.getSelection()
    if (sel && sel.anchorNode && contentRef.value?.contains(sel.anchorNode)) {
      e.preventDefault()
      undo()
    }
  }
}

async function searchAll() {
  syncFromDOM()
  for (let i = 0; i < words.value.length; i++) {
    const w = words.value[i].word.trim()
    if (!w) continue
    const hasPhonetic = words.value[i].phonetic && words.value[i].phonetic.trim()
    const hasMeaning = words.value[i].meaning && words.value[i].meaning.trim()
    if (hasPhonetic && hasMeaning) continue
    try {
      const pRes = await fetch(`/api/word/phonetic?word=${encodeURIComponent(w)}`)
      words.value[i].phonetic = (await pRes.text()) || ''
    } catch (e) { words.value[i].phonetic = '' }
    try {
      const mRes = await fetch(`/api/word/search?word=${encodeURIComponent(w)}`)
      const data = await mRes.json()
      words.value[i].meaning = data?.desc || ''
    } catch (e) { words.value[i].meaning = '' }
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

onMounted(() => {
  window.addEventListener('keydown', onWindowKeydown)
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
      @searchAll="searchAll"
      @toggleWordHidden="toggleWordHidden"
      @togglePhoneticHidden="togglePhoneticHidden"
      @toggleMeaningHidden="toggleMeaningHidden"
      @resetAll="resetAll"
      @addWord="addWord"
      @removeLastWord="removeLastWord"
      @exportPdf="exportPdf"
    />

    <div class="main-area">
      <div class="content-area" ref="contentRef">
        <div class="grid-container">
          <div class="table-column" v-for="(colData, colIdx) in columns" :key="colIdx">
            <div class="table-header">
              <div>序号</div>
              <div>单词 / 音标</div>
              <div>释义</div>
            </div>
            <div class="table-row" v-for="item in colData" :key="item.originalIndex">
              <div
                v-if="globalIndex(item) === 0"
                class="index index-edit"
                contenteditable="true"
                @blur="onFirstIndexBlur"
                @keydown.enter.prevent="e => { onFirstIndexBlur($event); e.target.blur() }"
                @focus="e => { e.target.textContent = String(1 + indexOffset).padStart(2, '0') }"
              >{{ String(1 + indexOffset).padStart(2, '0') }}</div>
              <div v-else class="index">{{ displayIndex(item) }}</div>
              <div class="word-section">
                <div class="word" :data-oid="item.originalIndex" contenteditable="true" @input="e => item.word = ($event.target).textContent" @keydown="e => onWordKeydown($event, item)" v-text="item.word"></div>
                <div class="phonetic" :data-oid="item.originalIndex" contenteditable="true" @input="e => item.phonetic = ($event.target).textContent" v-text="item.phonetic"></div>
              </div>
              <div
                class="meaning-text"
                :data-oid="item.originalIndex"
                contenteditable="true"
                @input="e => item.meaning = ($event.target).textContent"
                v-text="item.meaning"
                :title="item.meaning"
              ></div>
            </div>
          </div>
        </div>
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

.content-area {
  width: 210mm;
  min-height: 297mm;
  padding: 12mm;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,.15);
}

.grid-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8mm;
}

.table-column {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.table-header {
  display: grid;
  grid-template-columns: 10mm 35mm 1fr;
  padding: 2px 5px;
  background: #f0f5f9;
  border: 1px solid #ddd;
  font-size: 8pt;
  font-weight: bold;
  color: #555;
}

.table-row {
  display: grid;
  grid-template-columns: 10mm 35mm 1fr;
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

.index-edit {
  outline: none;
  border-radius: 2px;
  cursor: text;
  transition: background 0.15s;
}
.index-edit:hover { background: #e8f0fe; }
.index-edit:focus { background: #d0e0fc; color: #333; }

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
  padding: 1px 3px;
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

@media print {
  .content-area { box-shadow: none; }
  .meaning-text { -webkit-line-clamp: unset; line-clamp: unset; display: block; overflow: visible; }
}
</style>
