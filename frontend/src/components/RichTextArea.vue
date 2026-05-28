<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const editorEl = ref(null)
const fileInput = ref(null)
const selectedImg = ref(null)
const selectedBlank = ref(null)
const handlesStyle = ref({ display: 'none' })
const handlePositions = ref({ n: {}, s: {}, e: {}, w: {}, nw: {}, ne: {}, sw: {}, se: {} })

// ---- Sync ----

function syncContent() {
  if (!editorEl.value) return
  emit('update:modelValue', editorEl.value.innerHTML)
}

// ---- Image selection ----

function onEditorClick(e) {
  if (e.target.closest('.img-blank')) return // handled by mousedown
  deselectBlank()
  const img = e.target.closest('img')
  if (img && editorEl.value?.contains(img)) {
    selectImg(img)
  } else {
    deselectImg()
  }
}

function selectImg(img) {
  deselectBlank()
  selectedImg.value = img
  updateHandlesPosition(img)
}

function deselectImg() {
  selectedImg.value = null
  handlesStyle.value = { display: 'none' }
}

function updateHandlesPosition(img) {
  if (!editorEl.value) return
  const edRect = editorEl.value.getBoundingClientRect()
  const ir = img.getBoundingClientRect()
  const top = ir.top - edRect.top + editorEl.value.scrollTop
  const left = ir.left - edRect.left
  const w = ir.width
  const h = ir.height

  const c = 6
  const e = 6

  handlePositions.value = {
    n:  { top: top - e/2 + 'px', left: left + w/2 - e/2 + 'px', width: e + 'px', height: e + 'px', cursor: 'n-resize' },
    s:  { top: top + h - e/2 + 'px', left: left + w/2 - e/2 + 'px', width: e + 'px', height: e + 'px', cursor: 's-resize' },
    e:  { top: top + h/2 - e/2 + 'px', left: left + w - e/2 + 'px', width: e + 'px', height: e + 'px', cursor: 'e-resize' },
    w:  { top: top + h/2 - e/2 + 'px', left: left - e/2 + 'px', width: e + 'px', height: e + 'px', cursor: 'w-resize' },
    nw: { top: top - c/2 + 'px', left: left - c/2 + 'px', width: c + 'px', height: c + 'px', cursor: 'nw-resize' },
    ne: { top: top - c/2 + 'px', left: left + w - c/2 + 'px', width: c + 'px', height: c + 'px', cursor: 'ne-resize' },
    sw: { top: top + h - c/2 + 'px', left: left - c/2 + 'px', width: c + 'px', height: c + 'px', cursor: 'sw-resize' },
    se: { top: top + h - c/2 + 'px', left: left + w - c/2 + 'px', width: c + 'px', height: c + 'px', cursor: 'se-resize' },
  }
  handlesStyle.value = { display: 'block' }
}

// ---- Resize dragging ----

const dragging = ref(null)
let dragStartX = 0, dragStartY = 0, dragStartW = 0, dragStartH = 0, dragAspect = 1

function onHandleDown(e, handle) {
  if (!selectedImg.value) return
  e.preventDefault()
  e.stopPropagation()

  const ir = selectedImg.value.getBoundingClientRect()
  dragging.value = handle
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragStartW = ir.width
  dragStartH = ir.height
  dragAspect = ir.width / (ir.height || 1)

  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragUp)
}

function onDragMove(e) {
  if (!dragging.value || !selectedImg.value) return
  const dx = e.clientX - dragStartX
  const dy = e.clientY - dragStartY
  const img = selectedImg.value
  let nw, nh

  switch (dragging.value) {
    case 'se': nw = dragStartW + dx; nh = nw / dragAspect; break
    case 'sw': nw = dragStartW - dx; nh = nw / dragAspect; break
    case 'ne': nw = dragStartW + dx; nh = nw / dragAspect; break
    case 'nw': nw = dragStartW - dx; nh = nw / dragAspect; break
    case 'e':  nw = dragStartW + dx; nh = dragStartH; break
    case 'w':  nw = dragStartW - dx; nh = dragStartH; break
    case 's':  nh = dragStartH + dy; nw = dragStartW; break
    case 'n':  nh = dragStartH - dy; nw = dragStartW; break
  }

  nw = Math.max(20, Math.min(nw, editorEl.value.clientWidth))
  nh = Math.max(10, Math.min(nh, 2000))

  img.style.width = nw + 'px'
  img.style.height = nh + 'px'
  img.style.maxWidth = 'none'
  img.removeAttribute('width')
  updateHandlesPosition(img)
}

function onDragUp() {
  dragging.value = null
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragUp)
  if (selectedImg.value) updateHandlesPosition(selectedImg.value)
  syncContent()
}

// ---- Image insert ----

function onPaste(e) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      insertImageFile(item.getAsFile())
      return
    }
  }
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  if (file) insertImageFile(file)
  if (fileInput.value) fileInput.value.value = ''
}

function wrapImage(img) {
  if (img.parentElement?.classList.contains('img-wrapper')) return
  const wrapper = document.createElement('span')
  wrapper.classList.add('img-wrapper')
  wrapper.setAttribute('contenteditable', 'false')
  Object.assign(wrapper.style, {
    position: 'relative',
    display: 'inline-block',
    lineHeight: '0',
    verticalAlign: 'top'
  })
  img.parentElement.insertBefore(wrapper, img)
  wrapper.appendChild(img)
}

function insertImageFile(file) {
  if (!file || !file.type.startsWith('image/')) return
  const reader = new FileReader()
  reader.onload = () => {
    deselectImg()
    const img = document.createElement('img')
    img.src = reader.result
    img.style.width = '50%'
    img.style.display = 'block'
    img.style.margin = '6px 0'
    img.style.cursor = 'pointer'
    placeCaretAndInsert(img)
    syncContent()
    nextTick(() => selectImg(img))
  }
  reader.readAsDataURL(file)
}

function placeCaretAndInsert(node) {
  editorEl.value?.focus()
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) {
    editorEl.value?.appendChild(node)
    return
  }
  const range = sel.getRangeAt(0)
  if (!editorEl.value?.contains(range.commonAncestorContainer)) {
    editorEl.value?.appendChild(node)
    return
  }
  range.deleteContents()
  range.insertNode(node)
  range.setStartAfter(node)
  range.collapse(true)
  sel.removeAllRanges()
  sel.addRange(range)
}

function triggerUpload() {
  fileInput.value?.click()
}

// ---- Blank management ----

let blankDragData = null

function onEditorMouseDown(e) {
  if (e.target.classList.contains('blank-resize-h')) {
    const blank = e.target.closest('.img-blank')
    if (blank) { selectBlank(blank); startBlankResize(e, blank) }
    return
  }
  const blank = e.target.closest('.img-blank')
  if (blank) {
    selectBlank(blank)
    startBlankDrag(e, blank)
    return
  }
}

function addBlank() {
  if (!selectedImg.value) return
  deselectBlank()
  const img = selectedImg.value
  wrapImage(img)
  const wrapper = img.parentElement

  const blank = document.createElement('span')
  blank.classList.add('img-blank')
  blank.setAttribute('contenteditable', 'false')
  Object.assign(blank.style, {
    position: 'absolute',
    left: '25%',
    top: '25%',
    width: '50%',
    height: '50%',
    background: '#fff'
  })

  const rh = document.createElement('span')
  rh.classList.add('blank-resize-h')
  rh.setAttribute('contenteditable', 'false')
  blank.appendChild(rh)

  wrapper.appendChild(blank)
  nextTick(() => selectBlank(blank))
  syncContent()
}

function selectBlank(el) {
  deselectImg()
  if (selectedBlank.value && selectedBlank.value !== el) {
    selectedBlank.value.classList.remove('blank-selected')
  }
  selectedBlank.value = el
  el.classList.add('blank-selected')
}

function deselectBlank() {
  if (selectedBlank.value) {
    selectedBlank.value.classList.remove('blank-selected')
    selectedBlank.value = null
  }
}

function removeSelectedBlank() {
  if (!selectedBlank.value) return
  const blank = selectedBlank.value
  deselectBlank()
  blank.remove()
  syncContent()
}

function startBlankDrag(e, blank) {
  e.preventDefault()
  const wrapper = blank.parentElement
  const wr = wrapper.getBoundingClientRect()

  blankDragData = {
    el: blank,
    startX: e.clientX,
    startY: e.clientY,
    startLeft: parseFloat(blank.style.left) || 25,
    startTop: parseFloat(blank.style.top) || 25,
    ww: wr.width,
    wh: wr.height
  }

  document.addEventListener('mousemove', onBlankDragMove)
  document.addEventListener('mouseup', onBlankDragUp)
}

function startBlankResize(e, blank) {
  e.preventDefault()
  e.stopPropagation()
  const wrapper = blank.parentElement
  const wr = wrapper.getBoundingClientRect()

  blankDragData = {
    el: blank,
    startX: e.clientX,
    startY: e.clientY,
    startLeft: parseFloat(blank.style.left) || 25,
    startTop: parseFloat(blank.style.top) || 25,
    startW: parseFloat(blank.style.width) || 50,
    startH: parseFloat(blank.style.height) || 50,
    ww: wr.width,
    wh: wr.height,
    resizing: true
  }

  document.addEventListener('mousemove', onBlankDragMove)
  document.addEventListener('mouseup', onBlankDragUp)
}

function onBlankDragMove(e) {
  if (!blankDragData) return
  const d = blankDragData
  const dx = e.clientX - d.startX
  const dy = e.clientY - d.startY

  if (d.resizing) {
    const nw = Math.max(5, Math.min(100 - d.startLeft, d.startW + (dx / d.ww) * 100))
    const nh = Math.max(5, Math.min(100 - d.startTop, d.startH + (dy / d.wh) * 100))
    d.el.style.width = nw + '%'
    d.el.style.height = nh + '%'
  } else {
    const cw = parseFloat(d.el.style.width) || 50
    const ch = parseFloat(d.el.style.height) || 50
    const nl = Math.max(0, Math.min(100 - cw, d.startLeft + (dx / d.ww) * 100))
    const nt = Math.max(0, Math.min(100 - ch, d.startTop + (dy / d.wh) * 100))
    d.el.style.left = nl + '%'
    d.el.style.top = nt + '%'
  }
}

function onBlankDragUp() {
  blankDragData = null
  document.removeEventListener('mousemove', onBlankDragMove)
  document.removeEventListener('mouseup', onBlankDragUp)
  syncContent()
}

function onKeydown(e) {
  if ((e.key === 'Delete' || e.key === 'Backspace') && selectedBlank.value) {
    e.preventDefault()
    removeSelectedBlank()
  }
}

// ---- Scroll / resize ----

function onScrollOrResize() {
  if (selectedImg.value) updateHandlesPosition(selectedImg.value)
}

// ---- Global events ----

function onGlobalClick(e) {
  if (editorEl.value && !editorEl.value.contains(e.target)) {
    deselectImg()
    deselectBlank()
  }
}

onMounted(() => {
  document.addEventListener('click', onGlobalClick)
  window.addEventListener('resize', onScrollOrResize)
})

onUnmounted(() => {
  document.removeEventListener('click', onGlobalClick)
  window.removeEventListener('resize', onScrollOrResize)
})

watch(() => props.modelValue, (val) => {
  if (editorEl.value && editorEl.value.innerHTML !== val) {
    editorEl.value.innerHTML = val
    deselectImg()
    deselectBlank()
  }
}, { immediate: true })
</script>

<template>
  <div class="rich-editor">
    <div class="resize-handles" :style="handlesStyle">
      <div
        v-for="h in ['n','s','e','w','nw','ne','sw','se']"
        :key="h"
        class="resize-handle"
        :style="handlePositions[h]"
        @mousedown="onHandleDown($event, h)"
      ></div>
    </div>

    <div class="editor-float-bar">
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        style="display:none"
        @change="onFileChange"
      />
      <button class="float-btn" @click="triggerUpload" title="插入图片">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
      </button>
      <button v-if="selectedImg" class="float-btn" @click="addBlank" title="添加挖空">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="12" x2="21" y2="12"/></svg>
      </button>
    </div>

    <div
      ref="editorEl"
      class="editor-body"
      contenteditable="true"
      :data-placeholder="placeholder"
      @input="syncContent"
      @paste="onPaste"
      @click="onEditorClick"
      @mousedown="onEditorMouseDown"
      @keydown="onKeydown"
      @scroll="onScrollOrResize"
    ></div>
  </div>
</template>

<style scoped>
.rich-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  position: relative;
}

.editor-body {
  flex: 1;
  outline: none;
  font-size: 14px;
  line-height: 1.7;
  color: #333;
  min-height: 40px;
  overflow-y: auto;
  word-break: break-word;
  position: relative;
}

.editor-body:empty::before {
  content: attr(data-placeholder);
  color: #bbb;
}

.editor-body :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

/* ---- Resize handles ---- */
.resize-handles {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 10;
}

.resize-handle {
  position: absolute;
  width: 6px;
  height: 6px;
  background: #fff;
  border: 1px solid #7b7b7b;
  pointer-events: auto;
}

.resize-handle:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

/* ---- Image wrapper & blanks ---- */
.editor-body :deep(.img-wrapper) {
  position: relative;
  display: inline-block;
  line-height: 0;
  vertical-align: top;
}

.editor-body :deep(.img-blank) {
  position: absolute;
  background: #fff;
  border: 1px dashed #ccc;
  box-sizing: border-box;
}

.editor-body :deep(.img-blank.blank-selected) {
  border-color: #409eff;
  border-style: solid;
  z-index: 1;
}

.editor-body :deep(.blank-resize-h) {
  position: absolute;
  right: -3px;
  bottom: -3px;
  width: 7px;
  height: 7px;
  background: #fff;
  border: 1px solid #7b7b7b;
  cursor: se-resize;
  display: none;
}

.editor-body :deep(.img-blank.blank-selected .blank-resize-h) {
  display: block;
}

/* ---- Floating toolbar ---- */
.editor-float-bar {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  z-index: 5;
  padding: 2px;
}

.float-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e0e0e0;
  border-radius: 3px;
  background: #fafafa;
  color: #888;
  cursor: pointer;
  transition: all 0.15s;
}

.float-btn:hover {
  border-color: #409eff;
  color: #409eff;
  background: #f0f7ff;
}

</style>
