<template>
  <div class="note-app">
    <ToastOverlay :visible="toastVisible" :message="toastMsg" :type="toastType" />

    <div class="left" :class="{ collapsed: leftCollapsed }">
      <div class="list-header">
        <span class="list-title">笔记列表</span>
        <button class="btn-new" @click="addNote">+ 新建</button>
      </div>
      <div id="noteList">
        <div v-for="note in notes" :key="note.id" class="item"
             :class="{ active: note.id === currentId }"
             @click="switchNote(note)">
          <span class="item-dot" :style="{ background: noteColor(note.id) }"></span>
          <span class="item-title">{{ note.title }}</span>
          <span class="item-more" @click.stop="toggleNoteMenu(note.id)" title="更多">
            <svg v-bind="svg24" v-html="I.ellipsis" width="15" height="15"></svg>
          </span>
          <span class="del" @click.stop="deleteNote(note.id)">×</span>

          <!-- Dropdown menu -->
          <div class="item-menu" v-if="menuNoteId === note.id" @click.stop>
            <div class="item-menu-item" @click="addToQueueFromSidebar(note)">
              <svg width="14" height="14" v-bind="svg24" v-html="I.mergeExport"></svg>
              <span>加入打印队列</span>
            </div>
          </div>
        </div>
      </div>
      <Teleport to="#nav-right">
        <div class="settings-btn" @click.stop="settingsVisible = !settingsVisible" title="设置">
          <svg v-bind="svg24" v-html="I.settings"></svg>
        </div>
        <div class="settings-panel" v-show="settingsVisible" @click.stop>
          <div class="s-title">数据管理</div>
          <div class="s-row"><span class="s-label">数据存储在浏览器中</span></div>
          <div class="s-row s-actions">
            <button class="s-btn" @click="exportNotes">导出备份</button>
          </div>
          <div class="s-row s-actions">
            <button class="s-btn s-btn-dull" @click="importNotes">导入恢复</button>
          </div>
        </div>
      </Teleport>
    </div>

    <div class="toggle-strip" :class="{ collapsed: leftCollapsed }" @click="toggleLeft" :title="leftCollapsed ? '展开列表' : '收起列表'">
      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline v-if="leftCollapsed" points="9,18 15,12 9,6"></polyline>
        <polyline v-else points="15,18 9,12 15,6"></polyline>
      </svg>
    </div>

    <div class="main-area">
      <!-- 颜色选择条 -->
      <div class="sub-color-bar" :class="{ show: activeColorBar === 'blank' }">
        <span>挖空颜色：</span>
        <div v-for="c in blankColors" :key="c" class="color-item"
             :class="{ active: answerColor === c }"
             :style="{ background: c }" @mousedown.prevent="setAnswerColor(c)"></div>
      </div>
      <div class="sub-color-bar" :class="{ show: activeColorBar === 'font' }">
        <span>文字颜色：</span>
        <div v-for="c in fontColors" :key="c" class="color-item"
             :style="{ background: c }" @mousedown.prevent="execFormat('foreColor', c)"></div>
      </div>
      <div class="sub-color-bar" :class="{ show: activeColorBar === 'bg' }">
        <span>背景颜色：</span>
        <div v-for="c in bgColors" :key="c.bg" class="color-item"
             :class="{ 'clear-bg': c.bg === 'transparent' }"
             :style="{ background: c.display }" @mousedown.prevent="execFormat('backColor', c.bg)"></div>
      </div>
      <div class="sub-color-bar" :class="{ show: activeColorBar === 'grid' }">
        <span>网格颜色：</span>
        <div class="color-item" :class="{ active: !gridColor }" style="background:#c8c8c8" @mousedown.prevent="gridColor = null; clearGridColor()" title="默认"></div>
        <span class="color-sep"></span>
        <template v-for="(c, i) in gridColors" :key="c">
          <span v-if="i === 6 || i === 12" class="color-sep"></span>
          <div class="color-item" :class="{ active: gridColor === c }"
               :style="{ background: c }" @mousedown.prevent="gridColor = c; applyGridColor()"></div>
        </template>
      </div>
      <!-- 顶部横排工具栏 -->
      <div class="toolbar-top">
        <div class="icon-btn" title="撤销 (Ctrl+Z)" @click="undo">
          <svg v-bind="svg24" v-html="I.undo"></svg>
        </div>
        <div class="icon-btn" title="重做 (Ctrl+Shift+Z)" @click="redo">
          <svg v-bind="svg24" v-html="I.redo"></svg>
        </div>
        <div class="tb-sep"></div>
        <div class="icon-btn" id="mkBlank" title="选中文字 → 点击挖空 (Alt+Q)" @click="makeBlank">
          <svg v-bind="svg24" v-html="I.shovel"></svg>
        </div>
        <div class="icon-btn" title="显示 / 隐藏答案" @click="toggleAnswer" :class="{ active: showAnswer }">
          <svg v-bind="svg24" v-html="I.eye" v-show="!showAnswer"></svg>
          <svg v-bind="svg24" v-html="I.eyeOff" v-show="showAnswer"></svg>
        </div>
        <div class="icon-btn" title="挖空颜色" @click="toggleColorBar('blank')" :class="{ active: activeColorBar === 'blank' }">
          <svg v-bind="svg24" v-html="I.blankColor"></svg>
        </div>
        <div class="tb-sep"></div>
        <div class="icon-btn" title="标题" @click="makeHeading">
          <svg v-bind="svg24" v-html="I.heading"></svg>
        </div>
        <div class="icon-btn" title="文字颜色" @click="toggleColorBar('font')" :class="{ active: activeColorBar === 'font' }">
          <svg v-bind="svg24" v-html="I.brush"></svg>
        </div>
        <div class="icon-btn" title="文字加粗" @click="toggleFormat('bold')" :class="{ active: isBold }">
          <svg v-bind="svg24" v-html="I.bold"></svg>
        </div>
        <div class="icon-btn" title="文字倾斜" @click="toggleFormat('italic')" :class="{ active: isItalic }">
          <svg v-bind="svg24" v-html="I.italic"></svg>
        </div>
        <div class="icon-btn" title="字体下划线" @click="toggleFormat('underline')" :class="{ active: isUnderline }">
          <svg v-bind="svg24" v-html="I.underline"></svg>
        </div>
        <div class="icon-btn" title="代码块" @click="makeCodeBlock">
          <svg v-bind="svg24" v-html="I.code"></svg>
        </div>
        <div class="icon-btn" title="有序列表" @click="makeOrderedList">
          <svg v-bind="svg24" v-html="I.listOrdered"></svg>
        </div>
        <div class="icon-btn" title="文字背景颜色" @click="toggleColorBar('bg')" :class="{ active: activeColorBar === 'bg' }">
          <svg v-bind="svg24" v-html="I.highlighter"></svg>
        </div>
        <div class="icon-btn" title="清除格式" @click="clearFormat">
          <svg v-bind="svg24" v-html="I.eraser"></svg>
        </div>
        <div class="tb-sep"></div>
        <div class="icon-btn" title="导出 PDF" @click="openPdfDialog">
          <svg v-bind="svg1024" v-html="I.exportPdf"></svg>
        </div>
        <div class="icon-btn" title="导出 Word" @click="exportWord">
          <svg v-bind="svg1024" v-html="I.exportWord"></svg>
        </div>
        <div class="tb-sep"></div>
        <div class="icon-btn" title="打开遮盖板" @click="createCoverBoard">
          <svg v-bind="svg1024" v-html="I.coverBoard"></svg>
        </div>
        <div class="tb-sep"></div>
        <div class="icon-btn" title="自由文字模式" @click="toggleFreeTextMode" :class="{ active: freeTextMode }">
          <svg v-bind="svg24" v-html="I.addText"></svg>
        </div>
        <div class="tb-sep"></div>
        <div class="icon-btn" title="网格纸模式" @click="toggleGrid" :class="{ active: gridMode }">
          <svg v-bind="svg24" v-html="I.gridPaper"></svg>
        </div>
        <div class="icon-btn" title="矩阵点模式" @click="toggleDotGrid" :class="{ active: dotGridMode }">
          <svg v-bind="svg24" v-html="I.dotGrid"></svg>
        </div>
        <div class="icon-btn" title="等轴测网格模式" @click="toggleIsoGrid" :class="{ active: isoGridMode }">
          <svg v-bind="svg24" v-html="I.isoGrid"></svg>
        </div>
        <div class="icon-btn" :title="'工程方格纸 - ' + (!engGridMode ? '点击:实线' : engGridMode === 'solid' ? '当前:实线 | 点击:虚线' : '当前:虚线 | 点击:关闭')" @click="toggleEngGrid" :class="{ active: engGridMode }">
          <svg v-bind="svg1024" v-html="I.engGrid"></svg>
        </div>
        <div class="icon-btn" title="斜点阵纸" @click="toggleHexDotGrid" :class="{ active: hexDotGridMode }">
          <svg v-bind="svg24" v-html="I.hexDots"></svg>
        </div>
        <div class="tb-sep"></div>
        <div class="icon-btn" title="网格颜色" @click="toggleColorBar('grid')" :class="{ active: activeColorBar === 'grid' }" :style="{ color: gridColor || '#888' }">
          <svg v-bind="svg24" v-html="I.palette"></svg>
        </div>
      </div>

      <!-- 编辑器纸板 -->
      <div class="editor-wrap" :class="{ 'ft-active': freeTextMode }"
           @mousedown="onEditorWrapMousedown">
        <div id="editor" ref="editorRef" contenteditable="true"
             @input="onEditorInput" @paste="onPaste" @keydown="onEditorKeydown" @click="onEditorClick" @mousedown="onEditorMousedown" @mouseover="onEditorMouseover" @scroll="onEditorScroll"
             :class="{ 'is-empty': isEditorEmpty, 'show-all': showAnswer, 'grid-paper': gridMode, 'dot-grid': dotGridMode, 'iso-grid': isoGridMode, 'eng-grid-solid': engGridMode === 'solid', 'eng-grid-dashed': engGridMode === 'dashed', 'hex-dots': hexDotGridMode }"
             :style="{ '--answer-color': answerColor }"></div>
        <!-- Free text overlay — outside editor, scroll-synced via transform -->
        <div class="ft-layer" ref="ftLayerRef" v-if="freeTextBlocks.length > 0">
          <div class="ft-inner" :style="{ transform: `translateY(-${ftScrollY}px)` }">
          <div v-for="b in freeTextBlocks" :key="b.id"
               class="ft-blk"
               :class="{ 'ft-sel': ftSel === b.id, 'ft-edit': ftEdit === b.id }"
               :style="{ left: b.x + 'mm', top: b.y + 'mm', fontSize: b.fontSize + 'pt', color: b.color, ...ftEditStyle(b) }"
               @mousedown.left.stop="onFtDown($event, b)"
               @click.stop="onFtClick(b)"
               @dblclick.stop="startFtEdit(b)">
            <span v-if="ftEdit !== b.id && ftSel !== b.id" class="ft-text" :class="{ 'ft-empty': !b.text }" v-html="b.html || b.text"></span>
            <div v-else class="ft-edit-wrap" :style="ftEditStyle(b)">
              <!-- Inline formatting toolbar -->
              <div class="ft-tbar" @mousedown.stop @click.stop>
                <button @click="ftChgSize(-1)" title="缩小字号">A⁻</button>
                <span class="ft-tbar-sz" title="字号">{{ b.fontSize }}pt</span>
                <button @click="ftChgSize(1)" title="增大字号">A⁺</button>
                <span class="ft-tbar-div">|</span>
                <span v-for="c in ftColors" :key="c" class="ft-tbar-c" :class="{ on: b.color === c }"
                      :style="{ background: c }" @click="b.color = c" :title="c"></span>
                <input type="color" class="ft-tbar-pk" :value="b.color" @input="b.color = $event.target.value" title="自定义颜色" />
                <span class="ft-tbar-div">|</span>
                <button class="ft-tbar-del" @click="ftDelSel" title="删除">🗑</button>
              </div>
              <div class="ft-edit-row">
                <div class="ft-handle">
                  <span v-for="i in 6" :key="i" class="ft-dot"></span>
                </div>
                <div class="ft-dash-box">
                  <div v-if="ftEdit === b.id" class="ft-inp" contenteditable="true"
                       @input="e => { b.text = e.target.textContent || ''; b.html = normalizeHtml(e.target.innerHTML || '') }"
                       @paste="onFtPaste"
                       @keydown.escape.stop="onFtEscapeEdit(b)"
                       :ref="el => { if (el && !el.textContent.trim() && b.html) el.innerHTML = b.html }"
                       data-placeholder="在此处开始键入…"></div>
                  <span v-else class="ft-text-inner" v-html="b.html || b.text"></span>
                  <span class="ft-dash-rt"></span>
                  <span class="ft-dash-rb"></span>
                  <span class="ft-circle" @mousedown.left.stop="onFtResizeDown($event, b)"></span>
                  <span class="ft-rsz ft-rsz-tr" @mousedown.left.stop="onFtResizeDown($event, b)"></span>
                  <span class="ft-rsz ft-rsz-br" @mousedown.left.stop="onFtResizeDown($event, b)"></span>
                </div>
              </div>
            </div>
          </div>
          </div> <!-- /ft-inner -->
        </div>
      </div>



    </div>

    <!-- PDF 导出设置对话框 -->
    <div class="pdf-dialog-overlay" v-if="pdfDialogVisible" @click.self="pdfDialogVisible = false">
      <div class="pdf-dialog">
        <div class="pdf-dlg-title">导出 PDF</div>

        <div class="pdf-dlg-group">
          <div class="pdf-dlg-label">钉装边距</div>
          <div class="pdf-dlg-radios">
            <label :class="{ active: bindSide === 'none' }"><input type="radio" v-model="bindSide" value="none" /> 无</label>
            <label :class="{ active: bindSide === 'left' }"><input type="radio" v-model="bindSide" value="left" /> 左侧装订</label>
            <label :class="{ active: bindSide === 'right' }"><input type="radio" v-model="bindSide" value="right" /> 右侧装订</label>
          </div>
          <div class="pdf-dlg-hint" v-if="bindSide === 'left'">奇数页左边多留空，偶数页右边多留空（纸张左侧打孔）</div>
          <div class="pdf-dlg-hint" v-if="bindSide === 'right'">奇数页右边多留空，偶数页左边多留空（纸张右侧打孔）</div>
        </div>

        <div class="pdf-dlg-group" v-if="bindSide !== 'none'">
          <div class="pdf-dlg-label">装订边距宽度</div>
          <select v-model="bindWidth" class="pdf-dlg-select">
            <option :value="15">15mm · 较窄</option>
            <option :value="20">20mm · 标准</option>
            <option :value="25">25mm · 较宽</option>
            <option :value="30">30mm · 活页本</option>
          </select>
        </div>

        <div class="pdf-dlg-group">
          <label class="pdf-dlg-check">
            <input type="checkbox" v-model="autoBlank" />
            <span>奇偶页补白（奇数页时自动补一页带模板的空白页）</span>
          </label>
          <div class="pdf-dlg-hint">双面打印时，最后一页是带网格模板的空白页，拿下来就能继续手写</div>
        </div>

        <div class="pdf-dlg-actions">
          <button class="pdf-dlg-btn pdf-dlg-btn-cancel" @click="pdfDialogVisible = false">取消</button>
          <button class="pdf-dlg-btn pdf-dlg-btn-ok" @click="doExportPdf">导出 PDF</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default { name: 'NoteExport' }
</script>
<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'

import { marked } from 'marked'
import katex from 'katex'
import { I } from '../icons.js'
import { exportPdf as exportPdfApi } from '../api/pdf.js'
import { useToast } from '../composables/useToast.js'
import { usePrintQueue } from '../composables/usePrintQueue.js'
import ToastOverlay from '../components/ToastOverlay.vue'
import 'katex/dist/katex.min.css'

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
const { visible: toastVisible, message: toastMsg, type: toastType, show: showToast } = useToast()
const { add: addToQueue } = usePrintQueue()

// ---- Notes ----
const notes = ref([])
const currentId = ref(null)
const editorRef = ref(null)
const isEditorEmpty = ref(true)
const leftCollapsed = ref(false)
const settingsVisible = ref(false)
const gridMode = ref(false)
const dotGridMode = ref(false)
const isoGridMode = ref(false)
const engGridMode = ref(false)
function syncGridBg() {
  const anyOn = gridMode.value || dotGridMode.value || isoGridMode.value || engGridMode.value || hexDotGridMode.value
  if (!anyOn) { clearGridColor(); return }
  if (gridColor.value) applyGridColor()
  else clearGridColor()
}
function toggleGrid() {
  gridMode.value = !gridMode.value
  if (gridMode.value) { dotGridMode.value = false; isoGridMode.value = false; engGridMode.value = false; hexDotGridMode.value = false }
  syncGridBg()
}
function toggleDotGrid() {
  dotGridMode.value = !dotGridMode.value
  if (dotGridMode.value) { gridMode.value = false; isoGridMode.value = false; engGridMode.value = false; hexDotGridMode.value = false }
  syncGridBg()
}
function toggleIsoGrid() {
  isoGridMode.value = !isoGridMode.value
  if (isoGridMode.value) { gridMode.value = false; dotGridMode.value = false; engGridMode.value = false; hexDotGridMode.value = false }
  syncGridBg()
}
function toggleEngGrid() {
  if (!engGridMode.value) engGridMode.value = 'solid'
  else if (engGridMode.value === 'solid') engGridMode.value = 'dashed'
  else engGridMode.value = false
  if (engGridMode.value) { gridMode.value = false; dotGridMode.value = false; isoGridMode.value = false; hexDotGridMode.value = false }
  syncGridBg()
}
const hexDotGridMode = ref(false)
function toggleHexDotGrid() {
  hexDotGridMode.value = !hexDotGridMode.value
  if (hexDotGridMode.value) { gridMode.value = false; dotGridMode.value = false; isoGridMode.value = false; engGridMode.value = false }
  syncGridBg()
}

// ── Free text mode (Edge-style "Add Text" on paper) ──
const freeTextMode = ref(false)
const freeTextBlocks = ref([])  // [{ id, x(mm), y(mm), text, fontSize(pt), color }]
const ftLayerRef = ref(null)
const ftScrollY = ref(0)

function onEditorScroll() {
  if (editorRef.value) {
    ftScrollY.value = editorRef.value.scrollTop
  }
}
const ftSel = ref(null)
const ftEdit = ref(null)
const ftDrag = ref(null)
const ftResize = ref(null)
const ftJustCreated = ref(false)
const ftColors = ['#333333', '#000000', '#e74c3c', '#e67e22', '#2ecc71', '#3498db', '#9b59b6', '#1abc9c']

const ftSelData = computed(() => freeTextBlocks.value.find(b => b.id === ftSel.value) || null)


function toggleFreeTextMode() {
  freeTextMode.value = !freeTextMode.value
  ftSel.value = null; ftEdit.value = null
}

function createFtBlock(xMm, yMm) {
  const b = {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 5),
    x: Math.max(0, Math.min(xMm, 210 - 20)),
    y: Math.max(0, Math.min(yMm, 297 - 10)),
    text: '', html: '', fontSize: 12, color: '#333333', width: null, height: null
  }
  freeTextBlocks.value.push(b)
  ftSel.value = b.id; ftEdit.value = b.id
  freeTextMode.value = false
  ftJustCreated.value = true
  nextTick(() => {
    const inps = document.querySelectorAll('.ft-inp')
    const last = inps[inps.length - 1]
    if (last) last.focus()
    setTimeout(() => { ftJustCreated.value = false }, 300)
  })
}

function onFtDown(e, b) {
  ftSel.value = b.id
  // Allow drag from handle even while editing; clicks in the input pass through for text selection
  if (ftEdit.value === b.id && !e.target.closest('.ft-handle')) return
  e.preventDefault()
  ftDrag.value = { b, sx: e.clientX, sy: e.clientY, ox: b.x, oy: b.y }
}

function onFtClick(b) {
  if (ftSel.value === b.id) {
    // Second click on same block → enter edit mode
    startFtEdit(b)
  } else {
    ftSel.value = b.id
  }
}

function startFtEdit(b) {
  ftEdit.value = b.id; ftSel.value = b.id
  nextTick(() => {
    const inps = document.querySelectorAll('.ft-inp')
    for (const inp of inps) { if (inp.textContent === b.text) { inp.focus(); return } }
  })
}

function finishFtEdit(b) {
  ftEdit.value = null
  ftSel.value = null
  if (!b.text.trim()) {
    freeTextBlocks.value = freeTextBlocks.value.filter(x => x.id !== b.id)
  }
  saveFreeBlocks()
}

function onFtPaste(e) {
  e.preventDefault()
  const html = (e.clipboardData || window.clipboardData).getData('text/html')
  const text = (e.clipboardData || window.clipboardData).getData('text/plain')
  const inp = e.target
  if (html) {
    document.execCommand('insertHTML', false, normalizeHtml(html))
    // Clean up contenteditable's extra wrapping after paste
    nextTick(() => {
      inp.innerHTML = normalizeHtml(inp.innerHTML)
      const b = freeTextBlocks.value.find(x => x.id === ftEdit.value)
      if (b) { b.text = inp.textContent || ''; b.html = inp.innerHTML || '' }
    })
  } else if (text) {
    document.execCommand('insertText', false, text)
  }
}

function onFtEscapeEdit(b) {
  ftEdit.value = null
  // Keep ftSel — stays selected with chrome visible
}

function ftEditStyle(b) {
  const s = {}
  if (b.width) s.width = b.width + 'mm'
  if (b.height) s.minHeight = b.height + 'mm'
  return s
}
function ftChgSize(d) { if (ftSelData.value) ftSelData.value.fontSize = Math.max(8, Math.min(72, ftSelData.value.fontSize + d)) }
function ftDelSel() {
  if (!ftSel.value) return
  freeTextBlocks.value = freeTextBlocks.value.filter(b => b.id !== ftSel.value)
  ftSel.value = null
  saveFreeBlocks()
}

// Load/save freeTextBlocks with note
function loadFreeBlocks() {
  // Re-normalize old blocks that may have legacy HTML (e.g. <li> tags)
  freeTextBlocks.value = (currentNote.value?.freeBlocks || []).map(b => {
    if (b.html) b.html = normalizeHtml(b.html)
    return b
  })
}
function saveFreeBlocks() {
  const note = currentNote.value
  if (note) note.freeBlocks = JSON.parse(JSON.stringify(freeTextBlocks.value))
  saveNotes()
}

function onFtResizeDown(e, b) {
  e.preventDefault()
  const pxPerMm = 96 / 25.4
  const editWrap = e.target.closest('.ft-edit-wrap')
  const ow = b.width ? b.width * pxPerMm : (editWrap ? editWrap.offsetWidth : 80)
  const oh = b.height ? b.height * pxPerMm : (editWrap ? editWrap.offsetHeight : 30)
  const el = e.target.closest('[class*="ft-rsz-"]')
  const corner = el ? (el.classList.contains('ft-rsz-tr') ? 'tr' : 'br') : 'r'
  ftResize.value = { b, sx: e.clientX, sy: e.clientY, ow, oh, corner }
}

// Global drag/resize handler for free text blocks
function onFtMove(e) {
  const pxPerMm = 96 / 25.4
  if (ftResize.value) {
    const { b, sx, sy, ow, oh, corner } = ftResize.value
    const dx = e.clientX - sx
    const dy = e.clientY - sy
    if (corner === 'r') {
      b.width = Math.max(15, (ow + dx) / pxPerMm)
    } else if (corner === 'tr') {
      b.width = Math.max(15, (ow + Math.max(dx, -dy)) / pxPerMm)
      b.height = Math.max(12, (oh - dy) / pxPerMm)
    } else if (corner === 'br') {
      b.width = Math.max(15, (ow + Math.max(dx, dy)) / pxPerMm)
      b.height = Math.max(12, (oh + dy) / pxPerMm)
    }
    return
  }
  if (!ftDrag.value) return
  const { b, sx, sy, ox, oy } = ftDrag.value
  b.x = Math.max(0, Math.min(210 - 20, ox + (e.clientX - sx) / pxPerMm))
  b.y = Math.max(0, Math.min(297 - 10, oy + (e.clientY - sy) / pxPerMm))
}

// ── Export: inject free text blocks ──
// Editor padding: 20mm L, 18mm T. PDF @page default margin: 15mm.
// Offset blocks to compensate the 5mm/3mm coordinate system difference.
function buildFreeTextHtml() {
  if (freeTextBlocks.value.length === 0) return ''
  const dx = 5, dy = 3
  let h = ''
  for (const b of freeTextBlocks.value) {
    const w = b.width ? `width:${b.width}mm;` : ''
    const mh = b.height ? `min-height:${b.height}mm;` : ''
    h += `<div class="ft-pdf" style="position:absolute;left:${b.x + dx}mm;top:${b.y + dy}mm;font-size:${b.fontSize}pt;color:${b.color};font-family:'SimSun','Microsoft YaHei',sans-serif;line-height:1.4;padding:2px 6px;white-space:pre-wrap;word-break:normal;overflow-wrap:break-word;z-index:10;${w}${mh}">${b.html || escHtmlFn(b.text)}</div>`
  }
  return h
}

function escHtmlFn(s) { return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') }

// Normalize contenteditable innerHTML: flatten all block elements to <br>-separated inline text.
// Strategy (same as commit 575d1e0): eliminate inter-tag whitespace that causes blank lines,
// then convert every block-boundary to <br> and strip all block tags.
function normalizeHtml(html) {
  const BLOCK = 'div|p|li|ul|ol|h[1-6]|blockquote'
  return html
    // Fix from commit 575d1e0: \n between tags renders as unwanted blank lines
    .replace(/>\n+</g, '><')
    // Any closing block tag followed by an opening block tag → single <br> between
    .replace(new RegExp(`<\\/(?:${BLOCK})>\\s*<(?:${BLOCK})[^>]*>`, 'gi'), '<br>')
    // Strip ALL remaining block-level tags (text content is preserved)
    .replace(new RegExp(`<\\/?(?:${BLOCK})[^>]*>`, 'gi'), '')
    // Collapse 3+ consecutive <br> to at most 2
    .replace(/(<br\s*\/?\s*>\s*){3,}/gi, '<br><br>')
    // Strip leading / trailing
    .replace(/^(<br\s*\/?\s*>|\s)+/, '')
    .replace(/(<br\s*\/?\s*>|\s)+$/, '')
}

const NOTE_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#5470c6', '#91cc75', '#fc8452', '#ee6666', '#73c0de']
function noteColor(id) { return NOTE_COLORS[id % NOTE_COLORS.length] }
const currentNote = computed(() => notes.value.find(n => n.id === currentId.value))
const currentTitle = computed(() => currentNote.value?.title || '未命名笔记')

function saveNotes() {
  localStorage.setItem('notes', JSON.stringify(notes.value))
}
function syncEditorToNote() {
  const note = notes.value.find(n => n.id === currentId.value)
  if (note && editorRef.value) { note.content = editorRef.value.innerHTML; saveFreeBlocks() }
}
function updateIsEmpty() {
  if (editorRef.value) isEditorEmpty.value = !editorRef.value.textContent?.trim()
}
function onEditorInput() {
  syncEditorToNote()
  saveNotes()
  updateIsEmpty()
  if (isEditorEmpty.value && editorRef.value.innerHTML.trim()) {
    editorRef.value.innerHTML = ''
  }
}
const mdPattern = /(^#{1,6}\s)|(\*\*|__)|(^[\-\*\+]\s)|(^\d+\.\s)|(```)|(\[.*?\]\(.*?\))|(`[^`]+`)|(^\|)|(^>\s)/m
const mathPattern = /(?<!\$)\$(?!\$)[^$\n]+?\$(?!\$)|(?<!\$)\$\$[\s\S]*?\$\$|\\\([\s\S]*?\\\)|\\\[[\s\S]*?\\\]|\\frac|\\dfrac|\\sqrt|\\int|\\sum|\\prod|\\lim|\\infty|\\mathbb|\\mathbf|\\boldsymbol|\\mathcal|\\Rightarrow|\\Leftrightarrow|\\forall|\\exists|\\partial|\\nabla|\\sin|\\cos|\\tan|\\ln|\\log|\\cdot|\\times|\\div|\\pm|\\leq|\\geq|\\neq|\\approx|\\equiv|\\in|\\notin|\\subset|\\subseteq|\\angle|\\triangle|\\perp|\\mid|\\langle|\\rangle|\\ldots|\\cdots|\\vdots|\\ddots|\\begin\{|\\end\{|\\left|\\right|\\big|\\Big|\\overline|\\underline|\\hat|\\tilde|\\vec|\\dot|\\ddot/

function looksLikeMarkdown(text) {
  return mdPattern.test(text)
}

function hasMath(text) {
  return mathPattern.test(text)
}

function renderMath(html) {
  let result = html
  result = result.replace(/\$\$([\s\S]*?)\$\$/g, (_, tex) => {
    try { return '<span contenteditable="false">' + katex.renderToString(tex.trim(), { displayMode: true, throwOnError: false }) + '</span>' } catch { return _ }
  })
  result = result.replace(/(?<!\$)\$(?!\$)([^$\n]+?)\$(?!\$)/g, (_, tex) => {
    try { return '<span contenteditable="false">' + katex.renderToString(tex.trim(), { displayMode: false, throwOnError: false }) + '</span>' } catch { return _ }
  })
  result = result.replace(/\\\(([\s\S]*?)\\\)/g, (_, tex) => {
    try { return '<span contenteditable="false">' + katex.renderToString(tex.trim(), { displayMode: false, throwOnError: false }) + '</span>' } catch { return _ }
  })
  result = result.replace(/\\\[([\s\S]*?)\\\]/g, (_, tex) => {
    try { return '<span contenteditable="false">' + katex.renderToString(tex.trim(), { displayMode: true, throwOnError: false }) + '</span>' } catch { return _ }
  })
  return result
}

function onPaste(e) {
  const items = (e.clipboardData || window.clipboardData)?.items
  if (items) {
    for (const item of items) {
      if (item.type.startsWith('image/')) {
        e.preventDefault()
        const file = item.getAsFile()
        const reader = new FileReader()
        reader.onload = () => {
          const img = new Image()
          img.onload = () => {
            const w = Math.min(img.naturalWidth, 700)
            const el = document.createElement('img')
            el.src = reader.result

            const wrap = document.createElement('div')
            wrap.className = 'img-wrap'
            wrap.setAttribute('contenteditable', 'false')
            wrap.style.width = w + 'px'
            wrap.appendChild(el)

            editorRef.value.focus()
            const sel = window.getSelection()
            if (sel.rangeCount > 0) {
              sel.getRangeAt(0).insertNode(wrap)
              const after = document.createRange()
              after.setStartAfter(wrap)
              after.collapse(true)
              sel.removeAllRanges()
              sel.addRange(after)
            }
            syncEditorToNote()
            saveNotes()
            showToast('图片已插入 · 拖拽右下角缩放')
          }
          img.src = reader.result
        }
        reader.readAsDataURL(file)
        return
      }
    }
  }
  e.preventDefault()
  let text = (e.clipboardData || window.clipboardData).getData('text/plain')
  text = text.replace(/&amp;/g, '&').replace(/&#39;/g, "'").replace(/&gt;/g, '>').replace(/&lt;/g, '<')
  text = text.replace(/\[([^\]]*?\\(?:frac|sqrt|int|sum|prod|lim|infty|sin|cos|tan|ln|log|mathbb|mathbf|boldsymbol|mathcal|Rightarrow|forall|exists|partial|nabla|cases|begin|end)[^\]]*?)\]/g, (_, m) => '\\[' + m + '\\]')
  if (looksLikeMarkdown(text) || hasMath(text)) {
    try {
      let html = marked.parse(text.trimEnd()).replace(/>\n+</g, '><').replace(/&#39;/g, "'").replace(/&gt;/g, '>').replace(/&lt;/g, '<').trim()
      if (hasMath(text)) html = renderMath(html)
      const sel = window.getSelection()
      if (sel.rangeCount > 0) {
        const range = sel.getRangeAt(0)
        range.deleteContents()
        const frag = range.createContextualFragment(html)
        range.insertNode(frag)
        range.collapse(false)
        sel.removeAllRanges()
        sel.addRange(range)
      }
      updateIsEmpty()
      syncEditorToNote()
      saveNotes()
    } catch (err) {
      document.execCommand('insertText', false, text)
    }
  } else {
    const urlRegex = /https?:\/\/[^\s<>"]+/g
    if (urlRegex.test(text)) {
      urlRegex.lastIndex = 0
      const html = text.replace(urlRegex, url => `<a href="${url}" target="_blank" rel="noopener" title="Ctrl+点击打开链接">${url}</a>`)
      document.execCommand('insertHTML', false, html)
    } else {
      document.execCommand('insertText', false, text)
    }
  }
}

function loadNoteContent(note) {
  nextTick(() => {
    if (editorRef.value) { editorRef.value.innerHTML = note?.content || ''; updateIsEmpty(); loadFreeBlocks() }
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

// ---- 导入导出 ----
function exportNotes() {
  syncEditorToNote()
  const blob = new Blob([JSON.stringify(notes.value, null, 2)], { type: 'application/json' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `memory-forge-backup-${new Date().toISOString().slice(0,10)}.json`
  a.click()
  URL.revokeObjectURL(a.href)
  showToast('备份已下载')
  settingsVisible.value = false
}

function importNotes() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.json'
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    try {
      const data = JSON.parse(await file.text())
      if (Array.isArray(data)) {
        notes.value = data
        localStorage.setItem('notes', JSON.stringify(data))
        if (data.length > 0) {
          currentId.value = data[0].id
          loadNoteContent(data[0])
        }
        showToast(`已恢复 ${data.length} 条笔记`)
      } else {
        showToast('文件格式不对')
      }
    } catch { showToast('文件损坏，无法读取') }
    settingsVisible.value = false
  }
  input.click()
}

function toggleLeft() {
  leftCollapsed.value = !leftCollapsed.value
}

// ---- 挖空 ----
const blankUndoStack = []
const blankRedoStack = []
const MAX_BLANK_UNDO = 50

function findTextRange(root, searchText) {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
  let node
  while (node = walker.nextNode()) {
    const idx = node.textContent.indexOf(searchText)
    if (idx >= 0) {
      const range = document.createRange()
      range.setStart(node, idx)
      range.setEnd(node, idx + searchText.length)
      return range
    }
  }
  return null
}

function makeBlank() {
  const sel = window.getSelection()
  const text = sel.toString().trim()
  if (!text) { showToast('请先选中要挖空的文字'); return }
  if (/[\n\r]/.test(text)) { showToast('请勿跨行挖空，仅支持单行文字'); return }

  blankRedoStack.length = 0
  blankUndoStack.push({ html: editorRef.value.innerHTML, text })
  if (blankUndoStack.length > MAX_BLANK_UNDO) blankUndoStack.shift()

  editorRef.value.focus()
  const range = sel.getRangeAt(0)
  range.deleteContents()
  const blankSpan = document.createElement('span')
  blankSpan.className = 'blank'
  blankSpan.textContent = text
  range.insertNode(blankSpan)
  blankSpan.querySelectorAll('u').forEach(u => u.replaceWith(...u.childNodes))

  sel.removeAllRanges()
  syncEditorToNote()
  saveNotes()
  showToast('挖空成功')
}

let hoveredBlank = null

function onEditorMouseover(e) {
  const blank = e.target.closest('.blank')
  if (blank && editorRef.value?.contains(blank)) {
    if (hoveredBlank && hoveredBlank !== blank) hoveredBlank.style.outline = ''
    hoveredBlank = blank
    blank.style.outline = '1px dashed #409eff'
  } else if (hoveredBlank) {
    hoveredBlank.style.outline = ''
    hoveredBlank = null
  }
}

function unblank() {
  let blank = hoveredBlank
  if (!blank) {
    const sel = window.getSelection()
    if (!sel.rangeCount) return
    let node = sel.getRangeAt(0).startContainer
    if (node.nodeType === Node.TEXT_NODE) node = node.parentElement
    blank = node?.closest('.blank')
  }
  if (!blank || !editorRef.value?.contains(blank)) { showToast('请先将鼠标悬停在挖空文字上'); return }
  blank.style.outline = ''
  hoveredBlank = null
  blank.replaceWith(...blank.childNodes)
  syncEditorToNote()
  saveNotes()
  showToast('已恢复原样')
}

// ---- 显示/隐藏答案 ----
const showAnswer = ref(false)
function toggleAnswer() {
  showAnswer.value = !showAnswer.value
  showToast(showAnswer.value ? '已显示答案' : '已隐藏答案')
}

// ---- 颜色栏 ----
const answerColor = ref('#d93025')
const activeColorBar = ref(null)
function toggleColorBar(name) {
  activeColorBar.value = activeColorBar.value === name ? null : name
}

const gridColor = ref(null)
const gridColors = ['#e8e8e8', '#e2e0e8', '#e0e4e8', '#e8e4e0', '#e4e8e0', '#f0e8e8', '#c8c8c8', '#b0a0c0', '#a0b0c0', '#c0b0a0', '#a0c0a0', '#d0a0a0', '#999999', '#8877aa', '#7799aa', '#aa8877', '#77aa77', '#aa7777']

const pdfDialogVisible = ref(false)
const bindSide = ref('none')
const bindWidth = ref(20)
const autoBlank = ref(true)

function openPdfDialog() {
  pdfDialogVisible.value = true
}

function doExportPdf() {
  pdfDialogVisible.value = false
  nextTick(() => exportPdf())
}

function applyGridColor() {
  const c = gridColor.value
  const el = editorRef.value
  if (!el || !c) return
  if (!gridMode.value && !dotGridMode.value && !isoGridMode.value && !engGridMode.value && !hexDotGridMode.value) return
  el.style.removeProperty('background-size')
  el.style.removeProperty('background-position')
  if (gridMode.value) {
    el.style.backgroundImage = `linear-gradient(to right, ${c} 1px, transparent 1px), linear-gradient(to bottom, ${c} 1px, transparent 1px)`
    el.style.backgroundSize = '5mm 5mm'
  } else if (dotGridMode.value) {
    el.style.backgroundImage = `radial-gradient(circle, ${c} 1px, transparent 1px)`
    el.style.backgroundSize = '5mm 5mm'
  } else if (hexDotGridMode.value) {
    el.style.backgroundImage = `radial-gradient(circle, ${c} 1px, transparent 1px), radial-gradient(circle, ${c} 1px, transparent 1px)`
    el.style.backgroundSize = '20px 34.64px'
    el.style.backgroundPosition = '0 0, 10px 17.32px'
    return
  } else if (isoGridMode.value) {
    el.style.backgroundImage = `url("data:image/svg+xml,${encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="40" height="23"><g stroke="${c}" stroke-width="0.5"><line x1="0" y1="0" x2="0" y2="23"/><line x1="20" y1="0" x2="20" y2="23"/><line x1="0" y1="0" x2="40" y2="23"/><line x1="40" y1="0" x2="0" y2="23"/></g></svg>`)}")`
  } else if (engGridMode.value) {
    const d = engGridMode.value === 'dashed' ? ' stroke-dasharray="4 3"' : ''
    const r = parseInt(c.slice(1,3), 16), g = parseInt(c.slice(3,5), 16), b = parseInt(c.slice(5,7), 16)
    const major = '#' + [r,g,b].map(v => Math.max(0, v - 50).toString(16).padStart(2,'0')).join('')
    el.style.backgroundImage = `url("data:image/svg+xml,${encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40"><g><path d="M4,0 L4,40 M8,0 L8,40 M12,0 L12,40 M16,0 L16,40 M20,0 L20,40 M24,0 L24,40 M28,0 L28,40 M32,0 L32,40 M36,0 L36,40 M0,4 L40,4 M0,8 L40,8 M0,12 L40,12 M0,16 L40,16 M0,20 L40,20 M0,24 L40,24 M0,28 L40,28 M0,32 L40,32 M0,36 L40,36" stroke="${c}" stroke-width="0.5"/><path d="M0,0 L0,40 M0,0 L40,0" stroke="${major}" stroke-width="1"${d}/></g></svg>`)}")`
  }
}

function clearGridColor() {
  const el = editorRef.value
  if (el) {
    el.style.removeProperty('background-image')
    el.style.removeProperty('background-size')
    el.style.removeProperty('background-position')
  }
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
  if (sel.rangeCount === 0) return
  const savedRange = sel.getRangeAt(0).cloneRange()

  editorRef.value.focus()

  // Find contenteditable="false" elements (KaTeX formulas, etc.) that
  // intersect the selection.  execCommand skips them, so we temporarily
  // unlock them, apply the command, then lock them again.
  const affected = []
  if (editorRef.value) {
    for (const el of editorRef.value.querySelectorAll('[contenteditable="false"]')) {
      if (sel.containsNode(el, true)) {
        el.contentEditable = 'true'
        affected.push(el)
      }
    }
  }

  sel.removeAllRanges()
  sel.addRange(savedRange)
  document.execCommand(cmd, false, val)

  // Restore non-editable state
  affected.forEach(el => el.contentEditable = 'false')

  syncEditorToNote()
  saveNotes()
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

const headingLevels = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6']
function makeHeading() {
  editorRef.value.focus()
  const sel = window.getSelection()
  let current = null
  if (sel.rangeCount > 0) {
    let node = sel.getRangeAt(0).startContainer
    while (node && node !== editorRef.value) {
      if (node.nodeType === 1 && /^H[1-6]$/.test(node.tagName)) {
        current = node.tagName.toLowerCase()
        break
      }
      node = node.parentNode
    }
  }
  if (current) {
    const idx = headingLevels.indexOf(current)
    if (idx < headingLevels.length - 1) {
      document.execCommand('formatBlock', false, headingLevels[idx + 1])
      showToast('已设为 ' + headingLevels[idx + 1].toUpperCase())
    } else {
      document.execCommand('formatBlock', false, 'p')
      showToast('已还原为正文')
    }
  } else {
    document.execCommand('formatBlock', false, 'h1')
    showToast('已设为 H1 标题')
  }
  syncEditorToNote()
  saveNotes()
}

function makeCodeBlock() {
  const sel = window.getSelection()
  const text = sel.toString().trim()
  editorRef.value.focus()
  if (text) {
    const range = sel.getRangeAt(0)
    const pre = document.createElement('pre')
    const code = document.createElement('code')
    code.textContent = text
    pre.appendChild(code)
    range.deleteContents()
    range.insertNode(pre)
    sel.removeAllRanges()
  } else {
    const pre = document.createElement('pre')
    pre.innerHTML = '<code> </code>'
    const range = sel.getRangeAt(0)
    range.insertNode(pre)
    pre.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
  syncEditorToNote()
  saveNotes()
  showToast('已插入代码块')
}

function makeOrderedList() {
  editorRef.value.focus()
  document.execCommand('insertOrderedList', false, null)
  syncEditorToNote()
  saveNotes()
  showToast('已切换有序列表')
}

function onEditorWrapMousedown(e) {
  if (!freeTextMode.value) return
  if (e.button !== 0) return
  // Don't intercept clicks on existing blocks
  if (e.target.closest('.ft-blk') || e.target.closest('.ft-inp')) return
  e.preventDefault()
  const ed = editorRef.value
  if (!ed) return
  const rect = ed.getBoundingClientRect()
  const pxPerMm = 96 / 25.4
  const xMm = (e.clientX - rect.left) / pxPerMm - 20
  const yMm = (e.clientY - rect.top) / pxPerMm - 18
  createFtBlock(xMm, yMm)
}

function onEditorMousedown(e) {
  if (!editorRef.value?.textContent?.trim() && e.target === editorRef.value) {
    e.preventDefault()
    editorRef.value.focus()
    const sel = window.getSelection()
    const range = document.createRange()
    range.setStart(editorRef.value, 0)
    range.collapse(true)
    sel.removeAllRanges()
    sel.addRange(range)
  }
}

function onEditorClick(e) {
  if (e.ctrlKey && e.target.tagName === 'A') {
    e.preventDefault()
    window.open(e.target.href, '_blank')
  }
  // click on image: toggle selected, deselect others
  const wrap = e.target.closest('.img-wrap')
  editorRef.value?.querySelectorAll('.img-wrap.img-selected').forEach(el => {
    if (el !== wrap) el.classList.remove('img-selected')
  })
  if (wrap) { wrap.classList.toggle('img-selected'); e.preventDefault() }
  else { editorRef.value?.querySelectorAll('.img-wrap.img-selected').forEach(el => el.classList.remove('img-selected')) }
}

function scrollCursorIntoView() {
  const sel = window.getSelection()
  if (!sel.rangeCount || !editorRef.value) return
  const range = sel.getRangeAt(0)
  let node = range.startContainer
  if (node.nodeType === Node.TEXT_NODE) node = node.parentElement
  if (node && node !== editorRef.value && editorRef.value.contains(node)) {
    const editorRect = editorRef.value.getBoundingClientRect()
    const nodeRect = node.getBoundingClientRect()
    if (nodeRect.top < editorRect.top + 40 || nodeRect.bottom > editorRect.bottom - 40) {
      node.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }
}

function undo() {
  if (blankUndoStack.length > 0) {
    const state = blankUndoStack.pop()
    blankRedoStack.push({ html: editorRef.value.innerHTML, text: state.text })
    editorRef.value.innerHTML = state.html
    syncEditorToNote()
    saveNotes()
    nextTick(() => {
      const r = findTextRange(editorRef.value, state.text)
      if (r) {
        const sel = window.getSelection()
        sel.removeAllRanges()
        sel.addRange(r)
        scrollCursorIntoView()
      }
    })
    return
  }
  editorRef.value.focus()
  document.execCommand('undo', false, null)
  scrollCursorIntoView()
}

function redo() {
  if (blankRedoStack.length > 0) {
    const state = blankRedoStack.pop()
    blankUndoStack.push({ html: editorRef.value.innerHTML, text: state.text })
    editorRef.value.innerHTML = state.html
    syncEditorToNote()
    saveNotes()
    nextTick(() => {
      const r = findTextRange(editorRef.value, state.text)
      if (r) {
        const sel = window.getSelection()
        sel.removeAllRanges()
        sel.addRange(r)
        scrollCursorIntoView()
      }
    })
    return
  }
  editorRef.value.focus()
  document.execCommand('redo', false, null)
  scrollCursorIntoView()
}

function onEditorKeydown(e) {
  if (e.key === 'Tab' && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    if (e.shiftKey) {
      document.execCommand('outdent', false, null)
    } else {
      const sel = window.getSelection()
      if (sel.rangeCount > 0) {
        const range = sel.getRangeAt(0)
        range.deleteContents()
        const tabNode = document.createTextNode('\t')
        range.insertNode(tabNode)
        range.setStartAfter(tabNode)
        range.collapse(true)
        sel.removeAllRanges()
        sel.addRange(range)
      }
    }
    syncEditorToNote()
    saveNotes()
    return
  }
  if ((e.key === 'Backspace' || e.key === 'Delete') && !e.ctrlKey) {
    const sel = editorRef.value?.querySelector('.img-wrap.img-selected')
    if (sel) { e.preventDefault(); sel.remove(); syncEditorToNote(); saveNotes(); return }
  }
  if (e.altKey && e.key === 'ArrowUp') {
    e.preventDefault()
    moveBlock('up')
    return
  }
  if (e.altKey && e.key === 'ArrowDown') {
    e.preventDefault()
    moveBlock('down')
    return
  }
  if (e.altKey && e.key.toLowerCase() === 'q') {
    e.preventDefault()
    makeBlank()
    return
  }
  if (e.altKey && e.key.toLowerCase() === 'w') {
    e.preventDefault()
    unblank()
    return
  }
  if (e.key === 'Backspace') {
    const sel = window.getSelection()
    if (sel.rangeCount > 0 && sel.isCollapsed) {
      const range = sel.getRangeAt(0)
      let node = range.startContainer
      if (node.nodeType === Node.TEXT_NODE) {
        if (range.startOffset > 0) return
        node = node.parentElement
      }
      const li = node?.closest('li')
      if (li && editorRef.value?.contains(li)) {
        const textBefore = li.textContent.slice(0, range.startOffset)
        if (!textBefore.trim()) {
          e.preventDefault()
          document.execCommand('outdent', false, null)
          syncEditorToNote()
          saveNotes()
          return
        }
      }
    }
  }
  if (e.ctrlKey && e.key === 'z' && !e.shiftKey) {
    e.preventDefault()
    undo()
  } else if (e.ctrlKey && (e.key === 'Z' || (e.key === 'z' && e.shiftKey))) {
    e.preventDefault()
    redo()
  }
}

function moveBlock(dir) {
  const sel = window.getSelection()
  if (!sel.rangeCount || !editorRef.value) return
  let node = sel.getRangeAt(0).startContainer
  if (node.nodeType === Node.TEXT_NODE) node = node.parentElement

  const block = node.closest('#editor > *')
  if (!block) return

  const editor = editorRef.value
  if (dir === 'up') {
    const prev = block.previousElementSibling
    if (!prev) return
    editor.insertBefore(block, prev)
  } else {
    const next = block.nextElementSibling
    if (!next) return
    editor.insertBefore(block, next.nextElementSibling)
  }

  const range = document.createRange()
  const anchor = block.firstChild || block
  range.setStart(anchor, 0)
  range.collapse(true)
  sel.removeAllRanges()
  sel.addRange(range)

  syncEditorToNote()
  saveNotes()
  scrollCursorIntoView()
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

function collectKatexCss() {
  // Search ALL stylesheets for KaTeX rules — Vite merges CSS in production
  // so the katex file no longer has its own href.
  let css = ''
  try {
    for (const sheet of document.styleSheets) {
      try {
        for (const rule of sheet.cssRules || []) {
          if (rule.cssText && /katex|@font-face.*KaTeX/i.test(rule.cssText)) {
            css += rule.cssText + '\n'
          }
        }
      } catch (_) {}
    }
  } catch (_) {}
  return css
}

function getGridParams() {
  // Return grid type & color for the backend PDFBox overlay, or null if no grid.
  if (gridMode.value)       return { type: 'grid', color: gridColor.value }
  if (dotGridMode.value)    return { type: 'dot', color: gridColor.value }
  if (isoGridMode.value)    return { type: 'iso', color: gridColor.value }
  if (engGridMode.value)    return { type: engGridMode.value === 'dashed' ? 'eng-dashed' : 'eng-solid', color: gridColor.value }
  if (hexDotGridMode.value) return { type: 'hex', color: gridColor.value }
  return null
}

function buildExportHtml() {
  const el = editorRef.value

  const clone = el.cloneNode(true)

  // Strip default white backgrounds so the PDFBox grid shows through
  clone.querySelectorAll('div,p,span,h1,h2,h3,h4,h5,h6,td,th,li,blockquote').forEach(el => {
    const bg = (el.style.background || el.style.backgroundColor || '').toLowerCase()
    if (!bg || bg === 'white' || bg === '#fff' || bg === '#ffffff' || bg === 'transparent') {
      el.style.backgroundColor = 'transparent'
      el.style.background = 'transparent'
    }
  })
  clone.querySelectorAll('ol, ul').forEach(el => {
    el.style.listStylePosition = 'inside'
    el.style.paddingLeft = '0'
  })
  clone.querySelectorAll('pre').forEach(el => {
    el.style.background = 'transparent'
    el.style.border = 'none'
    el.style.borderLeft = '3px solid #999'
    el.style.borderRadius = '0'
  })
  clone.querySelectorAll('code').forEach(el => {
    el.style.background = 'transparent'
  })
  clone.querySelectorAll('.blank').forEach(el => {
    el.style.color = showAnswer.value ? answerColor.value : 'transparent'
    el.style.borderBottom = '0.3mm solid #999'
    el.style.display = 'inline'
  })

  // @page rules
  const margin = 15
  const bindMargin = bindSide.value !== 'none' ? bindWidth.value : 0
  let pageCss = ''
  if (bindSide.value === 'left') {
    pageCss = `
      @page :right { margin-left: ${margin + bindMargin}mm; margin-right: ${margin}mm; }
      @page :left  { margin-left: ${margin}mm; margin-right: ${margin + bindMargin}mm; }
    `
  } else if (bindSide.value === 'right') {
    pageCss = `
      @page :right { margin-left: ${margin}mm; margin-right: ${margin + bindMargin}mm; }
      @page :left  { margin-left: ${margin + bindMargin}mm; margin-right: ${margin}mm; }
    `
  } else {
    pageCss = `@page { margin: ${margin}mm; }`
  }

  const blankColor = showAnswer.value ? answerColor.value : 'transparent'

  return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.17.0/dist/katex.min.css" crossorigin="anonymous">
<style>
  @page { size: A4; margin: ${margin}mm; }
  ${pageCss}
  html, body {
    margin: 0; padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
    font-size: 15px; line-height: 1.9;
    font-weight: normal;
    color: #000;
    white-space: pre-wrap; word-wrap: break-word; word-break: break-all;
    background: transparent;
  }
  /* heading sizes — match the editor exactly */
  h1 { font-size: 24px; font-weight: 700; margin: 16px 0 8px; color: #1a1a1a; }
  h2 { font-size: 21px; font-weight: 700; margin: 14px 0 6px; color: #1a1a1a; }
  h3 { font-size: 18px; font-weight: 700; margin: 12px 0 6px; color: #1a1a1a; }
  h4 { font-size: 16px; font-weight: 700; margin: 10px 0 4px; color: #333; }
  h5 { font-size: 15px; font-weight: 600; margin: 8px 0 4px; color: #444; }
  h6 { font-size: 14px; font-weight: 600; margin: 6px 0 2px; color: #555; }
  p { margin: 0; }
  .blank { display: inline; border-bottom: 0.3mm solid #999; color: ${blankColor}; }
  pre {
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 14px; line-height: 1.6;
    background: transparent;
    border: none;
    border-left: 3px solid #999;
    border-radius: 0;
    padding: 12px 16px;
    margin: 8px 0;
    white-space: pre-wrap;
    word-wrap: break-word;
  }
  code {
    font-family: 'Consolas', 'Courier New', monospace;
    font-size: 0.9em;
    background: transparent;
    padding: 0;
    border-radius: 0;
  }
  pre code { font-size: 14px; background: transparent; }
  ol, ul { padding-left: 1.5em; margin: 4px 0; list-style-position: inside; }
  li { margin-bottom: 2px; }
  table { border-collapse: collapse; margin: 8px 0; font-size: 14px; }
  th { border: 1px solid #ccc; padding: 6px 10px; background: transparent; text-align: left; font-weight: 600; }
  td { border: 1px solid #ccc; padding: 6px 10px; }
	/* Free text blocks — isolate from global rich-text rules.
	   In the editor these blocks are outside #editor so :deep() styles don't apply.
	   In the PDF the global h1-h6/p/li/etc rules would leak in — reset them to inherit. */
	.ft-pdf, .ft-pdf h1, .ft-pdf h2, .ft-pdf h3, .ft-pdf h4, .ft-pdf h5, .ft-pdf h6,
	.ft-pdf p, .ft-pdf pre, .ft-pdf code, .ft-pdf blockquote,
	.ft-pdf table, .ft-pdf th, .ft-pdf td {
	  color: inherit; font-family: inherit; font-size: inherit;
	  font-weight: inherit; line-height: inherit;
	  margin: 0; padding: 0;
	  background: transparent; border: none;
	  white-space: inherit; word-break: inherit;
	}
	/* surviving <li>/<ul>/<ol> — hide markers, keep inline */
	.ft-pdf ul, .ft-pdf ol, .ft-pdf li {
	  list-style: none; margin: 0; padding: 0; display: inline;
	}

</style>
</head>
<body><div style="position:relative">${buildFreeTextHtml()}<div style="padding:3mm 0 0 5mm;width:170mm;box-sizing:content-box">${clone.innerHTML}</div></div></body>
</html>`
}

async function exportPdf() {
  const fn = getFileName('pdf')
  showToast('生成 PDF 中...', 'loading')

  try {
    const html = buildExportHtml()
    const grid = getGridParams()
    console.log('[exportPdf] grid params:', JSON.stringify(grid), 'autoBlank:', autoBlank.value)
    const blob = await exportPdfApi(html, false,
      grid ? grid.type : null,
      grid ? grid.color : null,
      autoBlank.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fn
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    showToast(`PDF 导出成功：${fn}`, 'success')
  } catch (e) {
    console.error('PDF导出错误:', e)
    showToast('导出失败: ' + (e.message || '未知错误'), 'error')
  }
}

function addToPrintQueueHandler() {
  syncEditorToNote()
  const html = buildExportHtml()
  const grid = getGridParams()
  console.log('[addToQueue] grid params:', JSON.stringify(grid))
  addToQueue({
    source: 'note',
    title: currentTitle.value,
    html,
    landscape: false,
    gridType: grid ? grid.type : null,
    gridColor: grid ? grid.color : null
  })
  showToast('已加入打印队列', 'success')
}

// ── Sidebar ⋮ menu ──
const menuNoteId = ref(null)

function toggleNoteMenu(id) {
  menuNoteId.value = menuNoteId.value === id ? null : id
}

async function addToQueueFromSidebar(note) {
  menuNoteId.value = null
  if (note.id !== currentId.value) {
    switchNote(note)
    await nextTick()
  }
  addToPrintQueueHandler()
}

function exportWord() {
  syncEditorToNote()
  const fn = getFileName('doc')
  showToast('正在导出 Word...', 'loading')
  const styles = `<style>body{font-size:16px;line-height:1.8;margin:20px;font-family:微软雅黑}p{margin:0}ol,ul{padding-left:1.5em}.blank{display:inline;border-bottom:2px solid #999;color:${showAnswer.value ? answerColor.value : 'transparent'}}.img-wrap{display:inline-block;max-width:100%}.img-wrap img{width:100%;display:block}br{line-height:1.8}</style>`
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8">${styles}</head><body>${editorRef.value.innerHTML}</body></html>`
  const blob = new Blob([html], { type: 'application/msword' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = fn; a.click()
  URL.revokeObjectURL(url)
  setTimeout(() => showToast(`Word 导出成功：${fn}`, 'success'), 200)
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
  gridMode.value = true
  document.addEventListener('selectionchange', updateFormatStates)
  document.addEventListener('click', closeNoteMenu)
  document.addEventListener('mousemove', onFtMove)
  document.addEventListener('mouseup', () => {
    if (ftDrag.value || ftResize.value) saveFreeBlocks()
    ftDrag.value = null; ftResize.value = null
  })
  document.addEventListener('keydown', onFtKey)
  document.addEventListener('click', onDocClick)
  nextTick(() => updateIsEmpty())
})

onUnmounted(() => {
  document.removeEventListener('selectionchange', updateFormatStates)
  document.removeEventListener('click', closeNoteMenu)
  document.removeEventListener('mousemove', onFtMove)
  document.removeEventListener('mouseup', () => {
    if (ftDrag.value || ftResize.value) saveFreeBlocks()
    ftDrag.value = null; ftResize.value = null
  })
  document.removeEventListener('keydown', onFtKey)
  document.removeEventListener('click', onDocClick)
})

function onFtKey(e) {
  if (e.key === 'Escape') {
    if (ftEdit.value) return // handled by @keydown.escape on input
    ftSel.value = null
    return
  }
  // Delete/Backspace on selected block (only when no input is focused)
  if (!ftSel.value || ftEdit.value) return
  if (e.key === 'Delete' || e.key === 'Backspace') {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.isContentEditable) return
    e.preventDefault()
    ftDelSel()
  }
}

function onDocClick(e) {
  if (ftJustCreated.value) return
  // If editing: only exit when clicking outside the currently editing block
  if (ftEdit.value) {
    const editEl = document.querySelector('.ft-blk.ft-edit')
    if (editEl && editEl.contains(e.target)) return // click inside same block, stay
    // Click outside — finish (b.text/b.html already up to date from @input)
    const b = freeTextBlocks.value.find(x => x.id === ftEdit.value)
    if (b) finishFtEdit(b)
    return
  }
  // Not editing, but selected — deselect if clicking outside any block
  if (ftSel.value && !e.target.closest('.ft-blk')) {
    ftSel.value = null
  }
}

function closeNoteMenu() {
  menuNoteId.value = null
}
</script>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

.note-app {
  display: flex;
  height: 100%;
  overflow: hidden;
  background: #f0f2f5;
  padding: 10px;
  gap: 0;
}

/* ---- Left: note list ---- */
.left {
  width: 230px;
  flex-shrink: 0;
  padding: 16px 10px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
  transition: width 0.2s, padding 0.2s, min-width 0.2s;
  overflow: hidden;
  position: relative;
}
.left.collapsed {
  width: 0;
  min-width: 0;
  padding: 14px 0;
  box-shadow: none;
  border: none;
  border-radius: 0;
}

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

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 0 4px;
}
.list-title {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
}
.btn-new {
  padding: 4px 12px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  user-select: none;
  transition: all 0.15s;
  white-space: nowrap;
}
.btn-new:hover { background: #337ecc; box-shadow: 0 2px 8px rgba(64,158,255,.3); }

.settings-btn {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 5px; cursor: pointer; user-select: none;
  color: #999; transition: all 0.15s;
}
.settings-btn:hover { background: #f0f0f0; color: #555; }
.settings-btn svg { width: 18px; height: 18px; }

.settings-panel {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 4px;
  width: 220px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,.12);
  z-index: 200;
}
.s-title { font-size: 13px; font-weight: 700; color: #1a1a1a; margin-bottom: 8px; }
.s-row { margin-bottom: 6px; }
.s-label { font-size: 11px; color: #888; }
.s-actions { margin-top: 8px; }
.s-btn {
  width: 100%; padding: 6px 0;
  background: #409eff; color: #fff;
  border: none; border-radius: 4px;
  cursor: pointer; font-size: 12px;
  transition: background 0.15s;
}
.s-btn:hover { background: #337ecc; }
.s-btn-dull { background: #909399; }
.s-btn-dull:hover { background: #787b80; }
#noteList { flex: 1; overflow-y: auto; }
#noteList::-webkit-scrollbar { width: 4px; }
#noteList::-webkit-scrollbar-thumb { background: #d0d0d0; border-radius: 2px; }

.item {
  position: relative;
  padding: 8px 10px;
  margin-bottom: 2px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  font-size: 13px;
  color: #555;
  transition: all 0.15s;
}
.item:hover { background: #f5f7fa; }
.item.active { background: #e8f4ff; color: #409eff; font-weight: 600; }
.item-dot {
  width: 8px; height: 8px;
  border-radius: 3px;
  flex-shrink: 0;
}
.item-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.del { color: #ccc; cursor: pointer; font-size: 14px; flex-shrink: 0; opacity: 0; transition: all 0.15s; }
.item:hover .del { opacity: 1; }
.del:hover { color: #f56c6c; }

.item-more {
  color: #ccc; cursor: pointer; flex-shrink: 0;
  opacity: 0; transition: opacity 0.15s, color 0.15s;
  display: flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; border-radius: 4px;
}
.item:hover .item-more { opacity: 1; }
.item-more:hover { color: #409eff; }

.item-menu {
  position: absolute;
  top: 100%; right: 28px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,.12);
  border: 1px solid #eee;
  z-index: 50;
  min-width: 140px;
  padding: 4px 0;
  animation: menu-in .15s ease;
}
@keyframes menu-in { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: translateY(0); } }

.item-menu-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 16px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  white-space: nowrap;
  transition: background .1s;
}
.item-menu-item:hover { color: #409eff; }

/* ---- Main area ---- */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
  gap: 0;
}

/* ---- Color bars ---- */
.sub-color-bar {
  display: none;
  padding: 5px 10px;
  gap: 6px;
  align-items: center;
  user-select: none;
  font-size: 12px;
  color: #888;
  background: #fff;
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
  flex-shrink: 0;
  margin-bottom: 6px;
}
.sub-color-bar.show { display: flex; }
.sub-color-bar .color-item {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.15s;
}
.sub-color-bar .color-item:hover { border-color: #999; transform: scale(1.15); }
.sub-color-bar .color-item.active { border-color: #333; transform: scale(1.15); }
.sub-color-bar .color-item.clear-bg { border: 2px dashed #ccc; position: relative; }
.sub-color-bar .color-item.clear-bg::after {
  content: '×';
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
  font-weight: bold;
}

/* ---- Top toolbar ---- */
.color-sep { width: 1px; height: 18px; background: #d0d0d0; flex-shrink: 0; margin: 0 2px; }

.toolbar-top {
  display: flex;
  gap: 3px;
  align-items: center;
  padding: 7px 12px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
  flex-shrink: 0;
  flex-wrap: wrap;
}

.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: all 0.15s;
  color: #666;
  flex-shrink: 0;
}
.icon-btn:hover { background: #f0f3f8; color: #409eff; }
.icon-btn:active { transform: scale(0.95); }
.icon-btn.active { background: #409eff; color: #fff; }
.icon-btn svg { width: 18px; height: 18px; display: block; }
#mkBlank svg { width: 20px; height: 20px; }

.tb-sep {
  width: 1px;
  height: 18px;
  background: #e8eaed;
  flex-shrink: 0;
  margin: 0 3px;
}

/* ---- Editor ---- */
.editor-wrap {
  flex: 1;
  display: flex;
  overflow: hidden;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 16px rgba(0,0,0,.08);
}

#editor {
  position: relative; /* needed for absolute-positioned free text blocks */
  flex: 1;
  border: none;
  padding: 18mm 20mm;
  font-size: 15px;
  line-height: 1.9;
  overflow-y: auto;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-all;
  outline: none;
  background: #fff;
  border-radius: 10px;
  color: #000;
}
#editor::-webkit-scrollbar { width: 0; height: 0; }
#editor { scrollbar-width: none; }

#editor.is-empty::before { content: '在此编辑笔记内容…'; color: #bbb; font-style: italic; }

#editor.grid-paper {
  background-color: #fdfdfd;
  background-image:
    linear-gradient(to right, #e5e5e5 1px, transparent 1px),
    linear-gradient(to bottom, #e5e5e5 1px, transparent 1px);
  background-size: 5mm 5mm;
}

#editor.dot-grid {
  background-color: #fdfdfd;
  background-image: radial-gradient(circle, #e5e5e5 1px, transparent 1px);
  background-size: 5mm 5mm;
}

#editor.iso-grid {
  background-color: #fdfdfd;
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20width='40'%20height='23'%3E%3Cline%20x1='0'%20y1='0'%20x2='0'%20y2='23'%20stroke='%23c8c8c8'%20stroke-width='0.5'/%3E%3Cline%20x1='20'%20y1='0'%20x2='20'%20y2='23'%20stroke='%23c8c8c8'%20stroke-width='0.5'/%3E%3Cline%20x1='0'%20y1='0'%20x2='40'%20y2='23'%20stroke='%23c8c8c8'%20stroke-width='0.5'/%3E%3Cline%20x1='40'%20y1='0'%20x2='0'%20y2='23'%20stroke='%23c8c8c8'%20stroke-width='0.5'/%3E%3C/svg%3E");
  background-size: 40px 23px;
}

#editor.eng-grid-solid {
  background-color: #fff;
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20width='40'%20height='40'%20viewBox='0%200%2040%2040'%3E%3Cpath%20d='M4,0%20L4,40%20M8,0%20L8,40%20M12,0%20L12,40%20M16,0%20L16,40%20M20,0%20L20,40%20M24,0%20L24,40%20M28,0%20L28,40%20M32,0%20L32,40%20M36,0%20L36,40%20M0,4%20L40,4%20M0,8%20L40,8%20M0,12%20L40,12%20M0,16%20L40,16%20M0,20%20L40,20%20M0,24%20L40,24%20M0,28%20L40,28%20M0,32%20L40,32%20M0,36%20L40,36'%20stroke='%23e0e0e0'%20stroke-width='0.5'/%3E%3Cpath%20d='M0,0%20L0,40%20M0,0%20L40,0'%20stroke='%23999'%20stroke-width='1'/%3E%3C/svg%3E");
  background-size: 10mm 10mm;
  background-position: 0 0;
}

#editor.eng-grid-dashed {
  background-color: #fff;
  background-image: url("data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20width='40'%20height='40'%20viewBox='0%200%2040%2040'%3E%3Cpath%20d='M4,0%20L4,40%20M8,0%20L8,40%20M12,0%20L12,40%20M16,0%20L16,40%20M20,0%20L20,40%20M24,0%20L24,40%20M28,0%20L28,40%20M32,0%20L32,40%20M36,0%20L36,40%20M0,4%20L40,4%20M0,8%20L40,8%20M0,12%20L40,12%20M0,16%20L40,16%20M0,20%20L40,20%20M0,24%20L40,24%20M0,28%20L40,28%20M0,32%20L40,32%20M0,36%20L40,36'%20stroke='%23e0e0e0'%20stroke-width='0.5'/%3E%3Cpath%20d='M0,0%20L0,40%20M0,0%20L40,0'%20stroke='%23999'%20stroke-width='1'%20stroke-dasharray='4%203'/%3E%3C/svg%3E");
  background-size: 10mm 10mm;
  background-position: 0 0;
}

#editor.hex-dots {
  background-color: #fdfdfd;
  background-image:
    radial-gradient(circle, #e5e5e5 1px, transparent 1px),
    radial-gradient(circle, #e5e5e5 1px, transparent 1px);
  background-size: 20px 34.64px;
  background-position: 0 0, 10px 17.32px;
}

#editor :deep(a) { color: #409eff; text-decoration: underline; cursor: pointer; }
#editor :deep(a):hover { color: #337ecc; }
#editor :deep(.img-wrap) {
  display: inline-block;
  resize: horizontal;
  overflow: hidden;
  max-width: 100%;
  min-width: 80px;
  border-radius: 4px;
  vertical-align: bottom;
  font-size: 0; line-height: 0;
}
#editor :deep(.img-wrap.img-selected) {
  outline: 2px solid #409eff;
  outline-offset: 2px;
  border-radius: 2px;
}
#editor :deep(.img-wrap img) {
  display: block;
  width: 100%;
  height: auto;
  pointer-events: none;
  border-radius: 4px;
}
#editor :deep(pre) {
  background: #fafafa;
  border: none;
  border-left: 3px solid #c0c4cc;
  border-radius: 4px;
  padding: 12px 16px;
  margin: 8px 0;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  overflow-wrap: break-word;
}
#editor :deep(code) {
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 0.9em;
  background: #f5f5f5;
  padding: 1px 4px;
  border-radius: 3px;
}
#editor :deep(pre code) {
  background: transparent;
  padding: 0;
  border-radius: 0;
  font-size: 14px;
}
#editor :deep(p) { margin: 0; }
#editor :deep(h1) { font-size: 24px; font-weight: 700; margin: 16px 0 8px; color: #1a1a1a; }
#editor :deep(h2) { font-size: 21px; font-weight: 700; margin: 14px 0 6px; color: #1a1a1a; }
#editor :deep(h3) { font-size: 18px; font-weight: 700; margin: 12px 0 6px; color: #1a1a1a; }
#editor :deep(h4) { font-size: 16px; font-weight: 700; margin: 10px 0 4px; color: #333; }
#editor :deep(h5) { font-size: 15px; font-weight: 600; margin: 8px 0 4px; color: #444; }
#editor :deep(h6) { font-size: 14px; font-weight: 600; margin: 6px 0 2px; color: #555; }
#editor :deep(ol), #editor :deep(ul) { padding-left: 1.5em; margin: 4px 0; }
#editor :deep(li) { margin-bottom: 2px; }
#editor :deep(table) { border-collapse: collapse; margin: 8px 0; font-size: 14px; }
#editor :deep(th) { border: 1px solid #ccc; padding: 6px 10px; background: #f5f7fa; text-align: left; font-weight: 600; min-width: 60px; }
#editor :deep(td) { border: 1px solid #ccc; padding: 6px 10px; min-width: 60px; }

@media print {
  #editor {
    box-shadow: none;
    border-radius: 0;
  }
  #editor :deep(pre) {
    background: transparent;
    border: none;
    border-left: 3px solid #999;
    border-radius: 0;
  }
  #editor :deep(code) { background: transparent; padding: 0; }
  #editor :deep(pre code) { background: transparent; }
}
</style>

<style>
.blank { display: inline; border-bottom: 2px solid #999; color: transparent; text-decoration: none; }
.blank:hover { color: var(--answer-color); border-bottom: none; cursor: help; }
.show-all .blank { color: var(--answer-color); border-bottom: none; }

.cover-board { position: fixed; top: 200px; left: 200px; width: 200px; height: 100px; background: #d93025; opacity: 0.8; border: 1px solid #ccc; border-radius: 4px; cursor: move; z-index: 999; box-shadow: 0 2px 10px rgba(0,0,0,0.2); resize: both; overflow: hidden; }
.cover-board .cover-header { height: 30px; background: #f0f0f0; display: flex; justify-content: space-between; align-items: center; padding: 0 10px; cursor: move; border-bottom: 1px solid #ccc; }
.cover-board .close-btn { color: red; cursor: pointer; font-weight: bold; }
.cover-board .opacity-control { position: absolute; bottom: 10px; left: 10px; right: 10px; display: flex; align-items: center; gap: 5px; font-size: 12px; color: #fff; background: rgba(0,0,0,0.3); padding: 3px; border-radius: 3px; }
.cover-board .opacity-slider { flex: 1; height: 8px; cursor: pointer; }

/* ---- PDF dialog ---- */
.pdf-dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 400; display: flex; align-items: center; justify-content: center; }
.pdf-dialog { width: 420px; background: #fff; border-radius: 12px; padding: 28px 24px 20px; box-shadow: 0 12px 40px rgba(0,0,0,.2); }
.pdf-dlg-title { font-size: 18px; font-weight: 700; color: #1a1a1a; margin-bottom: 20px; }
.pdf-dlg-group { margin-bottom: 16px; }
.pdf-dlg-label { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 8px; }
.pdf-dlg-radios { display: flex; flex-direction: column; gap: 6px; }
.pdf-dlg-radios label { font-size: 13px; color: #555; cursor: pointer; padding: 8px 12px; border: 1px solid #e0e0e0; border-radius: 6px; transition: all .15s; display: flex; align-items: center; gap: 6px; }
.pdf-dlg-radios label:hover { border-color: #409eff; }
.pdf-dlg-radios label.active { border-color: #409eff; background: #ecf5ff; color: #409eff; }
.pdf-dlg-radios input { display: none; }
.pdf-dlg-hint { font-size: 11px; color: #999; margin-top: 6px; line-height: 1.5; }
.pdf-dlg-select { width: 100%; padding: 8px 10px; font-size: 13px; border: 1px solid #e0e0e0; border-radius: 6px; outline: none; background: #fff; }
.pdf-dlg-check { display: flex; align-items: flex-start; gap: 8px; font-size: 13px; color: #333; cursor: pointer; }
.pdf-dlg-check input { margin-top: 2px; }
.pdf-dlg-actions { display: flex; gap: 10px; margin-top: 20px; }
.pdf-dlg-btn { flex: 1; padding: 10px 0; font-size: 14px; font-weight: 600; border: none; border-radius: 8px; cursor: pointer; transition: all .15s; }
.pdf-dlg-btn-cancel { background: #f5f5f5; color: #666; }
.pdf-dlg-btn-cancel:hover { background: #e8e8e8; }
.pdf-dlg-btn-ok { background: #409eff; color: #fff; }
.pdf-dlg-btn-ok:hover { background: #337ecc; }

/* ── Free Text Mode ── */
.editor-wrap { position: relative; }
.editor-wrap.ft-active { cursor: crosshair; }
/* Free text layer — overlays editor, scroll-synced via transform */
.ft-layer {
  position: absolute; inset: 0; z-index: 5;
  overflow: hidden;
  padding: 18mm 20mm;
  pointer-events: none;
}
.ft-inner {
  position: relative;
  width: 100%; height: 100%;
  pointer-events: none; /* pass through empty areas */
}

/* Free text blocks */
.ft-blk {
  position: absolute; z-index: 5; pointer-events: auto;
  padding: 2px 6px;
  border: 1px solid transparent; border-radius: 2px;
  user-select: none;
  white-space: pre-wrap; word-break: break-word;
  min-width: 1em; line-height: 1.4;
  font-family: "SimSun","Microsoft YaHei",sans-serif;
  cursor: move;
}
.ft-blk:hover {}
.ft-blk.ft-sel {}
.ft-blk.ft-edit {
  padding: 0; border: none; border-radius: 4px;
  background: transparent; cursor: default;
  z-index: 10; min-width: auto;
  box-shadow: 0 2px 12px rgba(0,0,0,.15);
  font-family: "SimSun","Microsoft YaHei",sans-serif;
}

/* ── Edge-style edit wrapper ── */
.ft-edit-wrap {
  display: flex; flex-direction: column;
  border-radius: 4px;
  overflow: hidden;
}
.ft-edit-row {
  display: flex; align-items: stretch;
  min-height: 1.6em;
}

/* Inline formatting toolbar */
.ft-tbar {
  display: flex; align-items: center; gap: 3px;
  padding: 4px 8px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  font-size: 11px;
  flex-shrink: 0;
}
.ft-tbar button {
  width: 22px; height: 22px;
  border: 1px solid #dadce0; border-radius: 4px;
  background: #fff; color: #202124;
  cursor: pointer; font-size: 10px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  padding: 0;
}
.ft-tbar button:hover { background: #f1f3f4; }
.ft-tbar-sz {
  font-size: 11px; font-weight: 600;
  min-width: 26px; text-align: center;
  color: #202124;
}
.ft-tbar-div { color: #dadce0; margin: 0 1px; }
.ft-tbar-c {
  width: 14px; height: 14px; border-radius: 50%;
  border: 2px solid transparent; cursor: pointer;
  flex-shrink: 0;
}
.ft-tbar-c:hover { border-color: #409eff; }
.ft-tbar-c.on { border-color: #1a73e8; }
.ft-tbar-pk {
  width: 14px; height: 14px; border: none; border-radius: 50%;
  cursor: pointer; padding: 0; background: transparent;
  flex-shrink: 0;
}
.ft-tbar-del {
  border: none !important; font-size: 12px !important;
  width: 22px !important; height: 22px !important;
}
.ft-tbar-del:hover { background: #fef0f0 !important; color: #f56c6c !important; }

/* Blue left handle */
.ft-handle {
  width: 1em;
  min-width: 15px;
  background: #0078e6;
  border-radius: 4px 0 0 4px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-content: center;
  gap: 2px;
  padding: 3px 3px;
  cursor: move;
  flex-shrink: 0;
}
.ft-dot {
  width: 2.5px; height: 2.5px;
  background: #fff;
  border-radius: 50%;
}

/* Dashed-border text area */
.ft-dash-box {
  position: relative;
  flex: 1;
  min-width: 4em;
  border-top: 2px dashed #0078e6;
  border-bottom: 2px dashed #0078e6;
  border-left: 2px dashed #0078e6;
  padding: 2px 8px;
  display: flex; align-items: center;
  background: rgba(255,255,255,.97);
}

/* Right-edge dashed segments (top & bottom, circle in middle) */
.ft-dash-rt {
  position: absolute; right: 0; top: 0;
  width: 2px; height: calc(50% - 10px);
  background: repeating-linear-gradient(to bottom, #0078e6 0 4px, transparent 4px 8px);
}
.ft-dash-rb {
  position: absolute; right: 0; bottom: 0;
  width: 2px; height: calc(50% - 10px);
  background: repeating-linear-gradient(to bottom, #0078e6 0 4px, transparent 4px 8px);
}

/* Right-edge diamond (also resize handle) */
.ft-circle {
  position: absolute;
  right: -10px;
  top: 50%;
  transform: translateY(-50%) rotate(45deg);
  width: 12px; height: 12px;
  border: 2.5px solid #0078e6;
  border-radius: 2px;
  background: #fff;
  cursor: ew-resize;
}
/* Corner resize handles */
.ft-rsz {
  position: absolute;
  width: 10px; height: 10px;
  border: 2px solid #0078e6;
  border-radius: 2px;
  background: #fff;
}
.ft-rsz-tr {
  top: -5px; right: -5px;
  cursor: ne-resize;
}
.ft-rsz-br {
  bottom: -5px; right: -5px;
  cursor: se-resize;
}

/* Text input */
.ft-inp {
  outline: none; min-width: 4em; min-height: 1.2em;
  white-space: pre-wrap; word-break: break-word;
  flex: 1;
}
/* Placeholder for empty input */
.ft-inp[data-placeholder]:empty::before {
  content: attr(data-placeholder);
  color: #999;
  pointer-events: none;
  white-space: nowrap;
}

/* Empty text display */
.ft-text.ft-empty {
  display: inline-block;
  min-width: 3em; min-height: 1em;
  opacity: 0.5;
}

/* Selected text (static, inside dash-box) */
.ft-text-inner {
  white-space: pre-wrap; word-break: break-word;
  flex: 1;
}

/* Reset block-element margins from contenteditable HTML inside free text */
.ft-text :deep(div), .ft-text :deep(p),
.ft-text-inner :deep(div), .ft-text-inner :deep(p) {
  margin: 0;
}
/* Surviving list tags: hide markers, keep inline to avoid unwanted breaks */
.ft-text :deep(ul), .ft-text :deep(ol), .ft-text :deep(li),
.ft-text-inner :deep(ul), .ft-text-inner :deep(ol), .ft-text-inner :deep(li) {
  list-style: none; margin: 0; padding: 0; display: inline;
}

@media print { .ft-overlay, .ft-tbar { display: none; } }
</style>
