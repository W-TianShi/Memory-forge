<template>
  <Teleport to="body">
    <!-- Panel overlay -->
    <div class="pq-backdrop" v-if="panelVisible" @click="panelVisible = false"></div>

    <div class="pq-sheet" :class="{ show: panelVisible }" @click.stop>
      <div class="pq-sheet-handle"><span></span></div>

      <div class="pq-sheet-header">
        <span class="pq-sheet-title">打印队列</span>
        <button class="pq-sheet-close" @click="panelVisible = false">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>

      <div class="pq-sheet-body" v-if="items.length === 0">
        <div class="pq-empty">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ddd" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" v-html="I.mergeExport"></svg>
          <p>队列为空</p>
          <p class="pq-empty-hint">在单词纸或笔记页面点击「加入打印队列」</p>
        </div>
      </div>

      <div class="pq-sheet-body pq-list" v-else>
        <label class="pq-global-toggle">
          <input type="checkbox" v-model="allNewSheet" @change="setAllNewSheet">
          <span>双面打印 — 全部开启</span>
        </label>
        <div class="pq-global-hint" v-if="allNewSheet">开启后，每篇文档页数为奇数时自动补一页空白页，避免双面打印时两篇文档正反面混在一起。</div>

        <div class="pq-items">
          <div v-for="(item, idx) in items" :key="item.id"
               :class="[
                 'pq-item',
                 { 'is-blank': item.blank, 'drop-before': dragOverIdx === idx && !dragInsertAfter, 'drop-after': dragOverIdx === idx && dragInsertAfter }
               ]"
               :draggable="true"
               @dragstart="onDragStart($event, idx)"
               @dragover.prevent="onDragOver($event, idx)"
               @dragleave="onDragLeave(idx)"
               @drop="onDrop($event, idx)"
               @dragend="onDragEnd">
            <span class="pq-drag" v-if="!item.blank">⋮⋮</span>
            <span class="pq-drag pq-drag-invis" v-else></span>

            <div class="pq-item-body">
              <div class="pq-item-top">
                <span class="pq-item-tag" :class="item.source">{{ sourceLabel(item.source) }}</span>
                <span class="pq-item-title">{{ item.title }}</span>
              </div>
              <div class="pq-item-meta">
                <span>{{ item.landscape ? '横版' : '竖版' }}</span>
                <span v-if="item.gridType">· {{ gridLabel(item.gridType) }}</span>
              </div>
            </div>

            <div class="pq-item-actions" v-if="!item.blank">
              <label class="pq-newsheet" title="此篇页数为奇数时，末尾补一页同款模板空白页">
                <input type="checkbox" v-model="item.newSheet" @change="save">
                <span>双面</span>
              </label>
              <button class="pq-del" @click="remove(item.id)" title="从队列移除">×</button>
            </div>
            <button class="pq-del" v-else @click="remove(item.id)" title="移除空白页">×</button>
          </div>
        </div>

        <div class="pq-insert-blank-wrap" v-if="items.length > 0">
          <button class="pq-insert-blank" @click="insertBlank(items[items.length - 1].id)">
            ＋ 在末尾插入空白页
          </button>
        </div>
      </div>

      <div class="pq-sheet-footer" v-if="items.length > 0">
        <span class="pq-estimate">共 {{ items.length }} 篇</span>
        <button class="pq-btn pq-btn-clear" @click="clearAll">清空队列</button>
        <button class="pq-btn pq-btn-merge" @click="doMerge" :disabled="merging">
          {{ merging ? '合并中...' : '合并下载' }}
        </button>
        <button v-if="mergedPdfBase64" class="pq-btn pq-btn-workshop" @click="sendToWorkshop">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M20 7h-6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z"/><path d="M16 3v4"/><path d="M16 21v-2"/><path d="M6 17V7a1 1 0 0 1 1-1h1"/><path d="M6 21v-4"/><rect x="4" y="3" width="4" height="4" rx="1"/><circle cx="6" cy="12" r="1.5"/></svg>
          送出稿工坊
        </button>
      </div>
    </div>

    <!-- Toast -->
    <div class="pq-toast" :class="{ show: toastVisible }">
      <span v-if="toastType === 'loading'" class="pq-toast-spinner"></span>
      <svg v-else-if="toastType === 'success'" class="pq-toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
      <svg v-else-if="toastType === 'error'" class="pq-toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      <span>{{ toastMsg }}</span>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePrintQueue } from '../composables/usePrintQueue.js'
import { mergePdfs } from '../api/pdf.js'
import { I, svg24Attrs } from '../icons.js'

const router = useRouter()
const svg24 = svg24Attrs

const mergedPdfBase64 = ref(null)

const { items, count, panelVisible, remove, move, insertBlank, clearAll } = usePrintQueue()

// Drag state
const dragIdx = ref(-1)
const dragOverIdx = ref(-1)
const dragInsertAfter = ref(false)

function onDragStart(e, idx) {
  dragIdx.value = idx
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', idx.toString())
}

function onDragOver(e, idx) {
  if (idx === dragIdx.value) return
  e.dataTransfer.dropEffect = 'move'
  const rect = e.currentTarget.getBoundingClientRect()
  dragInsertAfter.value = e.clientY > rect.top + rect.height / 2
  dragOverIdx.value = idx
}

function onDragLeave(idx) {
  if (dragOverIdx.value === idx) dragOverIdx.value = -1
}

function onDrop(e, idx) {
  e.preventDefault()
  const from = dragIdx.value
  dragOverIdx.value = -1
  if (from < 0 || from === idx) return
  const after = dragInsertAfter.value
  const to = after
    ? (from < idx ? idx : idx + 1)
    : (from < idx ? idx - 1 : idx)
  if (from === to) return
  move(from, to)
}

function onDragEnd() {
  dragIdx.value = -1
  dragOverIdx.value = -1
  dragInsertAfter.value = false
}

function save() {
  localStorage.setItem('mf-print-queue', JSON.stringify(items.value))
}

const allNewSheet = ref(false)
function setAllNewSheet() {
  const val = allNewSheet.value
  items.value.forEach(i => { if (!i.blank) i.newSheet = val })
  save()
}

const merging = ref(false)
const toastVisible = ref(false)
const toastMsg = ref('')
const toastType = ref('info')

function showToast(msg, type, duration = 2000) {
  toastMsg.value = msg
  toastType.value = type
  toastVisible.value = true
  if (type !== 'loading') {
    setTimeout(() => { toastVisible.value = false }, duration)
  }
}

async function doMerge() {
  if (merging.value) return
  merging.value = true
  showToast('正在生成合并 PDF...', 'loading')

  try {
    const reqItems = items.value.map(i => {
      if (i.blank) return { blank: true }
      return {
        html: i.html,
        landscape: i.landscape || false,
        gridType: i.gridType || null,
        gridColor: i.gridColor || null,
        blankHtml: i.blankHtml || null,
        newSheet: i.newSheet !== false
      }
    })

    const blob = await mergePdfs(reqItems)
    const d = new Date()
    const ts = `${d.getFullYear()}${String(d.getMonth()+1).padStart(2,'0')}${String(d.getDate()).padStart(2,'0')}-${String(d.getHours()).padStart(2,'0')}${String(d.getMinutes()).padStart(2,'0')}`

    // Download the merged PDF
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `打印合集-${ts}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)

    // Also save as base64 for "Send to Workshop"
    const reader = new FileReader()
    reader.onload = () => {
      mergedPdfBase64.value = reader.result.split(',')[1]
    }
    reader.readAsDataURL(blob)

    showToast('合并 PDF 下载成功', 'success')
  } catch (e) {
    console.error('Merge error:', e)
    showToast('合并失败：' + (e.message || '未知错误'), 'error', 3000)
  } finally {
    merging.value = false
  }
}

function sendToWorkshop() {
  if (!mergedPdfBase64.value) return
  const d = new Date()
  const ts = `${d.getFullYear()}${String(d.getMonth()+1).padStart(2,'0')}${String(d.getDate()).padStart(2,'0')}-${String(d.getHours()).padStart(2,'0')}${String(d.getMinutes()).padStart(2,'0')}`
  localStorage.setItem('mf-workshop-pdf', JSON.stringify({
    title: `打印合集-${ts}`,
    pdfBase64: mergedPdfBase64.value
  }))
  mergedPdfBase64.value = null
  panelVisible.value = false
  router.push('/workshop')
}

function sourceLabel(s) {
  const map = { word: '单词纸', note: '笔记', blank: '空白页' }
  return map[s] || s
}
function gridLabel(g) {
  const map = { grid: '方格', dot: '点阵', iso: '等轴测', 'eng-solid': '工程实线', 'eng-dashed': '工程虚线', hex: '斜点阵' }
  return map[g] || g
}
</script>

<style scoped>
/* ── Backdrop ── */
.pq-backdrop {
  position: fixed; inset: 0;
  background: rgba(0,0,0,.3);
  backdrop-filter: blur(2px);
  z-index: 3000;
  animation: pq-fade-in .25s ease;
}
@keyframes pq-fade-in { from { opacity: 0; } to { opacity: 1; } }

/* ── Bottom sheet ── */
.pq-sheet {
  position: fixed;
  left: 50%; bottom: 0;
  transform: translateX(-50%) translateY(100%);
  width: 540px; max-width: 100vw;
  max-height: 70vh;
  background: #fff;
  border-radius: 16px 16px 0 0;
  box-shadow: 0 -8px 40px rgba(0,0,0,.12);
  z-index: 3001;
  display: flex; flex-direction: column;
  transition: transform .3s cubic-bezier(.4,0,.2,1);
  overflow: hidden;
}
.pq-sheet.show { transform: translateX(-50%) translateY(0); }

.pq-sheet-handle {
  display: flex; justify-content: center;
  padding: 8px 0 4px; flex-shrink: 0;
}
.pq-sheet-handle span {
  width: 36px; height: 4px;
  border-radius: 2px; background: #ddd;
}

.pq-sheet-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 20px 12px; flex-shrink: 0;
}
.pq-sheet-title { font-size: 16px; font-weight: 700; color: #1a1a1a; }
.pq-sheet-close {
  width: 28px; height: 28px;
  border: none; background: #f5f5f5;
  border-radius: 50%; color: #999;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all .15s;
}
.pq-sheet-close:hover { background: #f56c6c; color: #fff; }

.pq-sheet-body { flex: 1; overflow-y: auto; min-height: 0; }

.pq-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; color: #bbb; gap: 8px; }
.pq-empty p { font-size: 14px; }
.pq-empty-hint { font-size: 12px !important; color: #ddd; }

.pq-global-toggle {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 20px; font-size: 13px; color: #606266; cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}
.pq-global-toggle input { accent-color: #409eff; }
.pq-global-hint {
  padding: 2px 20px 8px; font-size: 11px; color: #bbb; line-height: 1.5;
  border-bottom: 1px solid #f0f0f0;
}

.pq-items { padding: 6px 0; }

.pq-item {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px; margin: 2px 8px; border-radius: 8px;
  transition: border-color .15s, background .15s;
  border: 1px solid transparent;
  border-top: 2px solid transparent; border-bottom: 2px solid transparent;
}
.pq-item:hover { background: #f8f9fb; }
.pq-item.is-blank { opacity: .55; }
.pq-item.is-blank:hover { opacity: .75; background: #fafafa; }

.pq-item.drop-before {
  border-top: 2px solid #409eff;
  border-top-left-radius: 2px; border-top-right-radius: 2px;
}
.pq-item.drop-after {
  border-bottom: 2px solid #409eff;
  border-bottom-left-radius: 2px; border-bottom-right-radius: 2px;
}

.pq-drag {
  color: #ccc; font-size: 14px; cursor: grab;
  letter-spacing: -2px; user-select: none; flex-shrink: 0;
}
.pq-drag:active { cursor: grabbing; color: #409eff; }
.pq-drag-invis { visibility: hidden; }

.pq-item-body { flex: 1; min-width: 0; }
.pq-item-top { display: flex; align-items: center; gap: 8px; }
.pq-item-tag {
  font-size: 10px; padding: 1px 6px; border-radius: 3px;
  font-weight: 600; flex-shrink: 0; line-height: 1.5;
}
.pq-item-tag.word { background: #ecf5ff; color: #409eff; }
.pq-item-tag.note { background: #f0f9eb; color: #67c23a; }
.pq-item-tag.blank { background: #f5f5f5; color: #bbb; }
.pq-item-title {
  font-size: 13px; color: #303133;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.pq-item-meta { font-size: 11px; color: #bbb; margin-top: 2px; }

.pq-item-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.pq-newsheet {
  display: flex; align-items: center; gap: 2px;
  font-size: 11px; color: #909399; cursor: pointer;
}
.pq-newsheet input { accent-color: #409eff; width: 13px; height: 13px; }

.pq-del {
  width: 24px; height: 24px;
  border: none; background: transparent;
  border-radius: 50%; font-size: 16px; color: #bbb;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all .15s; line-height: 1;
}
.pq-del:hover { background: #fef0f0; color: #f56c6c; }

.pq-insert-blank-wrap { padding: 4px 20px 8px; }
.pq-insert-blank {
  width: 100%; padding: 8px 0; font-size: 12px;
  border: 1px dashed #ddd; border-radius: 6px;
  background: transparent; color: #bbb; cursor: pointer;
  transition: all .15s;
}
.pq-insert-blank:hover { border-color: #409eff; color: #409eff; background: #f8fbff; }

.pq-sheet-footer {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 20px; border-top: 1px solid #eee; flex-shrink: 0;
}
.pq-estimate { font-size: 12px; color: #bbb; flex: 1; }
.pq-btn {
  padding: 8px 18px; font-size: 13px; border-radius: 8px;
  border: none; cursor: pointer; font-weight: 500; transition: all .15s;
}
.pq-btn-clear { background: #f5f5f5; color: #999; }
.pq-btn-clear:hover { background: #fef0f0; color: #f56c6c; }
.pq-btn-merge {
  background: #409eff; color: #fff;
  box-shadow: 0 2px 8px rgba(64,158,255,.3);
}
.pq-btn-merge:hover { background: #337ecc; transform: translateY(-1px); box-shadow: 0 4px 14px rgba(64,158,255,.4); }
.pq-btn-merge:disabled { opacity: .6; transform: none; cursor: not-allowed; }
.pq-btn-workshop {
  display: flex; align-items: center; gap: 4px;
  padding: 8px 14px; border: 1px solid #409eff; border-radius: 8px;
  background: #ecf5ff; color: #409eff; font-size: 13px; cursor: pointer; transition: .15s;
}
.pq-btn-workshop:hover { background: #409eff; color: #fff; transform: translateY(-1px); }

/* ── Toast ── */
.pq-toast {
  position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);
  display: flex; align-items: center; gap: 10px;
  padding: 12px 24px;
  background: rgba(30,30,30,.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: #fff; border-radius: 10px; font-size: 14px;
  box-shadow: 0 8px 32px rgba(0,0,0,.2);
  z-index: 4000; opacity: 0; pointer-events: none;
  transition: opacity .25s;
}
.pq-toast.show { opacity: 1; }
.pq-toast-spinner {
  width: 18px; height: 18px;
  border: 2px solid rgba(255,255,255,.25);
  border-top-color: #fff; border-radius: 50%;
  animation: pq-spin .6s linear infinite;
}
@keyframes pq-spin { to { transform: rotate(360deg); } }
.pq-toast-icon { width: 18px; height: 18px; stroke: #fff; flex-shrink: 0; }
</style>
