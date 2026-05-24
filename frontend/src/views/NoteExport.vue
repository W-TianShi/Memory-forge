<template>
  <div class="note-app">
    <div class="toast" :class="{ show: toastVisible }">{{ toastMsg }}</div>

    <div class="left" :class="{ collapsed: leftCollapsed }">
      <h3>笔记列表</h3>
      <button class="btn" @click="addNote">+ 新建笔记</button>
      <div id="noteList">
        <div v-for="note in notes" :key="note.id" class="item"
             :class="{ active: note.id === currentId }"
             @click="switchNote(note)">
          {{ note.title }}
          <span class="del" @click.stop="deleteNote(note.id)">×</span>
        </div>
    </div>
  </div>

    <div class="toggle-bar" @click="toggleLeft" :title="leftCollapsed ? '展开列表' : '收起列表'">
      <span class="toggle-arrow">{{ leftCollapsed ? '▶' : '◀' }}</span>
    </div>

    <div class="right">
      <div class="bar">
        <!-- 撤销 / 重做 -->
        <div class="icon-btn" title="撤销 (Ctrl+Z)" @click="undo">
          <svg v-bind="svg24" v-html="I.undo"></svg>
        </div>
        <div class="icon-btn" title="重做 (Ctrl+Shift+Z)" @click="redo">
          <svg v-bind="svg24" v-html="I.redo"></svg>
        </div>
        <div class="bar-sep"></div>
        <!-- 挖空 -->
        <div class="icon-btn" id="mkBlank" title="选中文字 → 点击挖空" @click="makeBlank">
          <svg v-bind="svg24" v-html="I.shovel"></svg>
        </div>
        <!-- 显示/隐藏答案 -->
        <div class="icon-btn" title="显示 / 隐藏答案" @click="toggleAnswer" :class="{ active: showAnswer }">
          <svg v-bind="svg24" v-html="I.eye" v-show="!showAnswer"></svg>
          <svg v-bind="svg24" v-html="I.eyeOff" v-show="showAnswer"></svg>
        </div>
        <!-- 挖空颜色 -->
        <div class="icon-btn" title="挖空颜色" @click="toggleColorBar('blank')" :class="{ active: activeColorBar === 'blank' }">
          <svg v-bind="svg24" v-html="I.blankColor"></svg>
        </div>
        <div class="bar-sep"></div>
        <!-- 文字颜色 -->
        <div class="icon-btn" title="文字颜色" @click="toggleColorBar('font')" :class="{ active: activeColorBar === 'font' }">
          <svg v-bind="svg24" v-html="I.brush"></svg>
        </div>
        <!-- 加粗 / 倾斜 / 下划线 -->
        <div class="icon-btn" title="文字加粗" @click="toggleFormat('bold')" :class="{ active: isBold }">
          <svg v-bind="svg24" v-html="I.bold"></svg>
        </div>
        <div class="icon-btn" title="文字倾斜" @click="toggleFormat('italic')" :class="{ active: isItalic }">
          <svg v-bind="svg24" v-html="I.italic"></svg>
        </div>
        <div class="icon-btn" title="字体下划线" @click="toggleFormat('underline')" :class="{ active: isUnderline }">
          <svg v-bind="svg24" v-html="I.underline"></svg>
        </div>
        <!-- 背景颜色 -->
        <div class="icon-btn" title="文字背景颜色" @click="toggleColorBar('bg')" :class="{ active: activeColorBar === 'bg' }">
          <svg v-bind="svg24" v-html="I.highlighter"></svg>
        </div>
        <!-- 清除格式 -->
        <div class="icon-btn" title="清除格式" @click="clearFormat">
          <svg v-bind="svg24" v-html="I.eraser"></svg>
        </div>
        <div class="bar-sep"></div>
        <!-- 导出 PDF -->
        <div class="icon-btn" title="导出 PDF" @click="exportPdf">
          <svg v-bind="svg1024" v-html="I.exportPdf"></svg>
        </div>
        <!-- 导出 Word -->
        <div class="icon-btn" title="导出 Word" @click="exportWord">
          <svg v-bind="svg1024" v-html="I.exportWord"></svg>
        </div>
        <div class="bar-sep"></div>
        <!-- 遮盖板 -->
        <div class="icon-btn" title="打开遮盖板" @click="createCoverBoard">
          <svg v-bind="svg1024" v-html="I.coverBoard"></svg>
        </div>
      </div>

      <!-- 颜色选择条 -->
      <div class="sub-color-bar" :class="{ show: activeColorBar === 'blank' }">
        <span style="font-size:13px;color:#888;">挖空颜色：</span>
        <div v-for="c in blankColors" :key="c" class="color-item"
             :class="{ active: answerColor === c }"
             :style="{ background: c }" @mousedown.prevent="setAnswerColor(c)"></div>
      </div>
      <div class="sub-color-bar" :class="{ show: activeColorBar === 'font' }">
        <span style="font-size:13px;color:#888;">文字颜色：</span>
        <div v-for="c in fontColors" :key="c" class="color-item"
             :style="{ background: c }" @mousedown.prevent="execFormat('foreColor', c)"></div>
      </div>
      <div class="sub-color-bar" :class="{ show: activeColorBar === 'bg' }">
        <span style="font-size:13px;color:#888;">背景颜色：</span>
        <div v-for="c in bgColors" :key="c.bg" class="color-item"
             :class="{ 'clear-bg': c.bg === 'transparent' }"
             :style="{ background: c.display }" @mousedown.prevent="execFormat('backColor', c.bg)"></div>
      </div>

      <!-- 编辑器 -->
      <div id="editor" ref="editorRef" contenteditable="true"
           @input="onEditorInput" @paste="onPaste" @keydown="onEditorKeydown" @click="onEditorClick"
           :class="{ 'is-empty': isEditorEmpty, 'show-all': showAnswer }"
           :style="{ '--answer-color': answerColor }"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'

defineOptions({ name: 'NoteExport' })

import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import { I, SVG24, SVG1024 } from '../icons.js'

const svg24 = {
  xmlns: 'http://www.w3.org/2000/svg',
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  'stroke-width': '2',
  'stroke-linecap': 'round',
  'stroke-linejoin': 'round'
}
const svg1024 = {
  xmlns: 'http://www.w3.org/2000/svg',
  viewBox: '0 0 1024 1024',
  fill: 'currentColor'
}

// ---- Toast ----
const toastVisible = ref(false)
const toastMsg = ref('')
let toastTimer = null
function showToast(msg) {
  toastMsg.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => toastVisible.value = false, 1800)
}

// ---- Notes ----
const notes = ref([])
const currentId = ref(null)
const editorRef = ref(null)
const isEditorEmpty = ref(true)
const leftCollapsed = ref(false)
const currentNote = computed(() => notes.value.find(n => n.id === currentId.value))
const currentTitle = computed(() => currentNote.value?.title || '未命名笔记')

function saveNotes() { localStorage.setItem('notes', JSON.stringify(notes.value)) }
function syncEditorToNote() {
  const note = notes.value.find(n => n.id === currentId.value)
  if (note && editorRef.value) note.content = editorRef.value.innerHTML
}
function updateIsEmpty() {
  if (editorRef.value) isEditorEmpty.value = !editorRef.value.textContent?.trim()
}
function onEditorInput() {
  syncEditorToNote()
  saveNotes()
  updateIsEmpty()
  // 编辑器为空时清除残留格式标签，避免新文字继承旧颜色
  if (isEditorEmpty.value && editorRef.value.innerHTML.trim()) {
    editorRef.value.innerHTML = ''
  }
}
function onPaste(e) {
  e.preventDefault()
  const text = (e.clipboardData || window.clipboardData).getData('text/plain')
  const urlRegex = /https?:\/\/[^\s<>"]+/g
  if (urlRegex.test(text)) {
    urlRegex.lastIndex = 0
    const html = text.replace(urlRegex, url => `<a href="${url}" target="_blank" rel="noopener" title="Ctrl+点击打开链接">${url}</a>`)
    document.execCommand('insertHTML', false, html)
  } else {
    document.execCommand('insertText', false, text)
  }
}

function loadNoteContent(note) {
  nextTick(() => {
    if (editorRef.value) { editorRef.value.innerHTML = note?.content || ''; updateIsEmpty() }
  })
}

function addNote() {
  const t = prompt('笔记名')
  if (!t) return
  const id = Date.now()
  notes.value.push({ id, title: t, content: '' })
  currentId.value = id
  loadNoteContent(null)
  saveNotes()
}

function deleteNote(id) {
  notes.value = notes.value.filter(n => n.id !== id)
  if (currentId.value === id) {
    if (notes.value.length > 0) {
      currentId.value = notes.value[0].id
      loadNoteContent(notes.value[0])
    } else {
      currentId.value = Date.now()
      notes.value.push({ id: currentId.value, title: '默认笔记', content: '' })
      loadNoteContent(null)
    }
  }
  saveNotes()
  showToast('已删除')
}

function switchNote(note) {
  if (currentId.value === note.id) return
  syncEditorToNote()
  saveNotes()
  currentId.value = note.id
  loadNoteContent(note)
  showToast('切换：' + note.title)
}

function toggleLeft() {
  leftCollapsed.value = !leftCollapsed.value
}

// ---- 挖空 ----
function makeBlank() {
  const sel = window.getSelection()
  const text = sel.toString()
  if (!text) { showToast('请先选中要挖空的文字'); return }
  if (/[\n\r]/.test(text)) { showToast('请勿跨行挖空，仅支持单行文字'); return }
  const range = sel.getRangeAt(0)
  const blankSpan = document.createElement('span')
  blankSpan.className = 'blank'
  blankSpan.textContent = text
  range.deleteContents()
  range.insertNode(blankSpan)
  sel.removeAllRanges()
  syncEditorToNote()
  saveNotes()
  showToast('挖空成功')
}

// ---- 显示/隐藏答案 ----
const showAnswer = ref(false)
function toggleAnswer() {
  showAnswer.value = !showAnswer.value
  showToast(showAnswer.value ? '已显示答案' : '已隐藏答案')
}

// ---- 颜色栏（合并为一个状态）----
const answerColor = ref('#d93025')
const activeColorBar = ref(null)
function toggleColorBar(name) {
  activeColorBar.value = activeColorBar.value === name ? null : name
}

const blankColors = ['#d93025', '#1976d2', '#388e3c', '#f57c00', '#7b1fa2', '#424242']
const fontColors = ['#000000', '#333333', '#d93025', '#1976d2', '#388e3c', '#f57c00', '#7b1fa2']
const bgColors = [
  { bg: '#ffff00', display: '#ffff00' }, { bg: '#90ee90', display: '#90ee90' },
  { bg: '#00ffff', display: '#00ffff' }, { bg: '#ffb6c1', display: '#ffb6c1' },
  { bg: '#ffa500', display: '#ffa500' }, { bg: '#d7ccc8', display: '#d7ccc8' },
  { bg: 'transparent', display: '#fff' }
]

function setAnswerColor(color) {
  answerColor.value = color
  if (coverBoard) coverBoard.style.background = color
}

// ---- 格式命令 ----
function execFormat(cmd, val) {
  const sel = window.getSelection()
  let savedRange = sel.rangeCount > 0 ? sel.getRangeAt(0).cloneRange() : null
  editorRef.value.focus()
  if (savedRange) { sel.removeAllRanges(); sel.addRange(savedRange) }
  document.execCommand(cmd, false, val)
}

const isBold = ref(false)
const isItalic = ref(false)
const isUnderline = ref(false)
function toggleFormat(cmd) {
  editorRef.value.focus()
  document.execCommand(cmd, false, null)
  updateFormatStates()
}
function updateFormatStates() {
  const sel = window.getSelection()
  if (!sel.anchorNode || !editorRef.value?.contains(sel.anchorNode)) return
  isBold.value = document.queryCommandState('bold')
  isItalic.value = document.queryCommandState('italic')
  isUnderline.value = document.queryCommandState('underline')
}

function clearFormat() {
  const sel = window.getSelection()
  if (!sel.toString()) { showToast('请先选中文字'); return }
  editorRef.value.focus()
  document.execCommand('removeFormat', false, null)
  syncEditorToNote()
  saveNotes()
  showToast('已清除格式')
}

function undo() {
  editorRef.value.focus()
  document.execCommand('undo', false, null)
}

function redo() {
  editorRef.value.focus()
  document.execCommand('redo', false, null)
}

function onEditorKeydown(e) {
  if (e.ctrlKey && e.key === 'z' && !e.shiftKey) {
    e.preventDefault()
    undo()
  } else if (e.ctrlKey && (e.key === 'Z' || (e.key === 'z' && e.shiftKey))) {
    e.preventDefault()
    redo()
  }
}

function onEditorClick(e) {
  if (e.ctrlKey && e.target.tagName === 'A') {
    e.preventDefault()
    window.open(e.target.href, '_blank')
  }
}

// ---- 导出 ----
const exportCounter = new Map()
function getTimeStr() {
  const d = new Date()
  return `${String(d.getDate()).padStart(2,'0')}-${String(d.getHours()).padStart(2,'0')}${String(d.getMinutes()).padStart(2,'0')}`
}
function getFileName(ext) {
  const base = currentTitle.value.replace(/[\\/:*?"<>|]/g, '_')
  const key = `${base}-${getTimeStr()}`
  let c = exportCounter.get(key) || 0
  c++
  exportCounter.set(key, c)
  return `${key}${c > 1 ? `(${c - 1})` : ''}.${ext}`
}

async function exportPdf() {
  const fn = getFileName('pdf')
  showToast('生成PDF中...')
  const el = editorRef.value
  const bak = { o: el.style.overflow, h: el.style.height, b: el.style.border, bg: el.style.backgroundColor }
  Object.assign(el.style, { overflow: 'visible', height: 'auto', border: 'none', backgroundColor: '#fff' })
  try {
    const canvas = await html2canvas(el, { scale: 2, useCORS: true, background: '#fff' })
    const pdf = new jsPDF('p', 'mm', 'a4')
    const w = pdf.internal.pageSize.getWidth() - 20
    const h = pdf.internal.pageSize.getHeight() - 20
    const ph = w / (canvas.width / canvas.height)
    let y = 0, page = 1
    while (y < canvas.height) {
      if (page > 1) pdf.addPage()
      const slice = document.createElement('canvas')
      slice.width = canvas.width
      slice.height = Math.min(canvas.height - y, h * canvas.width / w)
      slice.getContext('2d').drawImage(canvas, 0, y, canvas.width, slice.height, 0, 0, canvas.width, slice.height)
      pdf.addImage(slice.toDataURL('image/jpeg'), 'JPEG', 10, 10, w, ph * slice.height / canvas.height)
      y += slice.height; page++
    }
    pdf.save(fn)
    showToast(`PDF导出成功：${fn}`)
  } catch (e) {
    console.error('PDF导出错误:', e)
    showToast('导出失败')
  } finally {
    Object.assign(el.style, { overflow: bak.o, height: bak.h, border: bak.b, backgroundColor: bak.bg })
  }
}

function exportWord() {
  syncEditorToNote()
  const fn = getFileName('doc')
  showToast('正在导出Word…')
  const styles = `<style>body{font-size:16px;line-height:1.8;margin:20px;font-family:微软雅黑}.blank{display:inline;border-bottom:2px solid #333;color:${answerColor.value}}br{line-height:1.8}</style>`
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8">${styles}</head><body>${editorRef.value.innerHTML}</body></html>`
  const blob = new Blob([html], { type: 'application/msword' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = fn; a.click()
  URL.revokeObjectURL(url)
  setTimeout(() => showToast(`Word导出成功：${fn}`), 300)
}

// ---- 遮盖板 ----
let coverBoard = null, isDragging = false, dsx, dsy, csx, csy

function createCoverBoard() {
  if (coverBoard) { coverBoard.remove(); coverBoard = null; return }
  coverBoard = document.createElement('div')
  coverBoard.className = 'cover-board'
  coverBoard.style.background = answerColor.value
  coverBoard.innerHTML = `<div class="cover-header"><span>遮盖板</span><span class="close-btn">×</span></div><div class="opacity-control"><span>透明度</span><input type="range" min="0" max="1" step="0.1" value="0.8" class="opacity-slider"></div>`
  document.body.appendChild(coverBoard)
  coverBoard.querySelector('.close-btn').onclick = () => { coverBoard.remove(); coverBoard = null }
  coverBoard.querySelector('.opacity-slider').oninput = e => coverBoard.style.opacity = e.target.value
  coverBoard.querySelector('.cover-header').onmousedown = e => {
    isDragging = true; dsx = e.clientX; dsy = e.clientY; csx = coverBoard.offsetLeft; csy = coverBoard.offsetTop; e.preventDefault()
  }
  document.onmousemove = e => { if (!isDragging) return; coverBoard.style.left = `${csx + e.clientX - dsx}px`; coverBoard.style.top = `${csy + e.clientY - dsy}px` }
  document.onmouseup = () => { isDragging = false }
  showToast('遮盖板已打开，可拖拽移动、调整大小和透明度')
}

// ---- Init ----
onMounted(() => {
  const saved = localStorage.getItem('notes')
  if (saved) { try { notes.value = JSON.parse(saved) } catch (e) { notes.value = [] } }
  if (notes.value.length > 0) {
    currentId.value = notes.value[0].id
    loadNoteContent(notes.value[0])
  } else {
    currentId.value = Date.now()
    notes.value = [{ id: currentId.value, title: '默认笔记', content: '' }]
    saveNotes()
  }
  document.addEventListener('selectionchange', updateFormatStates)
  nextTick(() => updateIsEmpty())
})

onUnmounted(() => {
  document.removeEventListener('selectionchange', updateFormatStates)
})
</script>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

.note-app { display: flex; height: calc(100vh - 42px); overflow: hidden; }

.left { width: 260px; flex-shrink: 0; padding: 15px; display: flex; flex-direction: column; border-right: 1px solid #ccc; position: relative; transition: width 0.2s, padding 0.2s; overflow: hidden; }
.left.collapsed { width: 0; padding: 0; border-right: none; }
.left h3 { margin-bottom: 15px; text-align: center; }

.toggle-bar { width: 14px; background: transparent; flex-shrink: 0; display: flex; align-items: center; justify-content: center; cursor: pointer; user-select: none; }
.toggle-arrow { font-size: 9px; color: #bbb; background: #e8e8e8; width: 14px; height: 36px; display: flex; align-items: center; justify-content: center; border-radius: 4px; transition: all 0.15s; }
.toggle-bar:hover .toggle-arrow { color: #409eff; background: #d0d0d0; }
.btn { padding: 8px 16px; background: #409eff; color: white; border: none; border-radius: 6px; cursor: pointer; margin-bottom: 10px; user-select: none; }

#noteList { flex: 1; overflow-y: auto; margin-top: 10px; }
.item { padding: 8px 10px; border: 1px solid #eee; margin-bottom: 6px; border-radius: 4px; display: flex; justify-content: space-between; cursor: pointer; user-select: none; transition: all 0.2s; }
.item:hover { background: #f5f7fa; }
.item.active { background: #e8f4ff; border-color: #409eff; font-weight: bold; }
.del { color: red; cursor: pointer; }

.right { flex: 1; padding: 20px; display: flex; flex-direction: column; overflow: hidden; }
.bar { margin-bottom: 15px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

.icon-btn { width: 38px; height: 38px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 8px; cursor: pointer; user-select: none; transition: all 0.2s; color: #606266; position: relative; }
.icon-btn:hover { background: #ecf5ff; color: #409eff; transform: scale(1.08); }
.icon-btn.active { background: #409eff; color: #fff; }
.icon-btn:active { transform: scale(0.95); }
.icon-btn svg { width: 22px; height: 22px; display: block; }
#mkBlank svg { width: 26px; height: 26px; }

.bar-sep { width: 1px; height: 24px; background: #d0d0d0; margin: 0 2px; align-self: center; flex-shrink: 0; }

#editor.is-empty::before { content: '在此编辑笔记内容…'; color: #bbb; font-style: italic; }

.sub-color-bar { display: none; margin-bottom: 10px; gap: 8px; align-items: center; user-select: none; }
.sub-color-bar.show { display: flex; }
.sub-color-bar .color-item { width: 24px; height: 24px; border-radius: 50%; cursor: pointer; border: 2px solid transparent; transition: all 0.15s; }
.sub-color-bar .color-item:hover { border-color: #999; transform: scale(1.15); }
.sub-color-bar .color-item.active { border-color: #333; transform: scale(1.15); }
.sub-color-bar .color-item.clear-bg { border: 2px dashed #ccc; position: relative; }
.sub-color-bar .color-item.clear-bg::after { content: '×'; position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 14px; color: #999; font-weight: bold; }

#editor { flex: 1; border: 1px solid #ccc; padding: 15px; font-size: 16px; line-height: 1.8; overflow-y: auto; overflow-x: auto; white-space: pre-wrap; word-wrap: break-word; word-break: break-all; outline: none; max-width: 120ch; font-family: system-ui, -apple-system, sans-serif; }
#editor :deep(a) { color: #409eff; text-decoration: underline; cursor: pointer; }
#editor :deep(a):hover { color: #337ecc; }

.toast { position: fixed; top: 50%; left: 50%; transform: translate(-50%,-50%); background: rgba(0,0,0,0.7); color: #fff; padding: 10px 20px; border-radius: 6px; font-size: 14px; z-index: 9999; opacity: 0; transition: opacity 0.3s; pointer-events: none; }
.toast.show { opacity: 1; }
</style>

<style>
.blank { display: inline; border-bottom: 2px solid #333; color: transparent; padding: 0 2px; }
.show-all .blank { color: var(--answer-color); border-bottom: none; }

.cover-board { position: fixed; top: 200px; left: 200px; width: 200px; height: 100px; background: #d93025; opacity: 0.8; border: 1px solid #ccc; border-radius: 4px; cursor: move; z-index: 999; box-shadow: 0 2px 10px rgba(0,0,0,0.2); resize: both; overflow: hidden; }
.cover-board .cover-header { height: 30px; background: #f0f0f0; display: flex; justify-content: space-between; align-items: center; padding: 0 10px; cursor: move; border-bottom: 1px solid #ccc; }
.cover-board .close-btn { color: red; cursor: pointer; font-weight: bold; }
.cover-board .opacity-control { position: absolute; bottom: 10px; left: 10px; right: 10px; display: flex; align-items: center; gap: 5px; font-size: 12px; color: #fff; background: rgba(0,0,0,0.3); padding: 3px; border-radius: 3px; }
.cover-board .opacity-slider { flex: 1; height: 8px; cursor: pointer; }
</style>
