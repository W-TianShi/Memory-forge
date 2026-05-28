<script setup>
import { ref, computed } from 'vue'
import RichTextArea from '../components/RichTextArea.vue'

defineOptions({ name: 'MistakesBook' })

const layoutMode = ref('two')
const pages = ref([createEmptyPage()])

function createEmptyPage() {
  return {
    id: Date.now() + Math.random(),
    problems: layoutMode.value === 'two' ? [createEmptyProblem(), createEmptyProblem()] : [createEmptyProblem()]
  }
}

function createEmptyProblem() {
  return {
    id: Date.now() + Math.random(),
    question: '',
    thinking: '',
    answer: '',
    aiMindmap: '',
    showAi: false,
    continuation: false
  }
}

function switchMode(mode) {
  layoutMode.value = mode
  pages.value = pages.value.map(p => ({
    ...p,
    problems: mode === 'two' ? ensureN(p.problems, 2) : ensureN(p.problems, 1)
  }))
}

function ensureN(arr, n) {
  while (arr.length < n) arr.push(createEmptyProblem())
  return arr.slice(0, n)
}

function addPage() {
  pages.value.push(createEmptyPage())
}

function removePage(idx) {
  if (pages.value.length > 1) pages.value.splice(idx, 1)
}

function addProblemToPage(pageIdx) {
  const page = pages.value[pageIdx]
  const max = layoutMode.value === 'two' ? 2 : 1
  if (page.problems.length < max) {
    page.problems.push(createEmptyProblem())
  }
}

function removeProblemFromPage(pageIdx, probIdx) {
  const page = pages.value[pageIdx]
  const min = layoutMode.value === 'two' ? 2 : 1
  if (page.problems.length > min) {
    page.problems.splice(probIdx, 1)
  }
}

function printBook() {
  window.print()
}

const printHint = computed(() => {
  const total = pages.value.length
  if (total === 1) return '建议攒够 2 页以上再打印'
  return `共 ${total} 页，每张纸 ${layoutMode.value === 'two' ? '2' : '1'} 道题`
})
</script>

<template>
  <div class="app-wrap">
    <!-- Sidebar -->
    <div class="sidebar">
      <div class="sidebar-title">错题整理本</div>
      <div class="sidebar-hint">{{ printHint }}</div>

      <div class="sidebar-section">
        <span class="sidebar-label">布局</span>
        <button :class="{ active: layoutMode === 'two' }" @click="switchMode('two')">分二</button>
        <button :class="{ active: layoutMode === 'one' }" @click="switchMode('one')">分一</button>
      </div>

      <div class="sidebar-actions">
        <button class="sidebar-btn" @click="addPage">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          加一页
        </button>
        <button class="sidebar-btn primary" @click="printBook">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 12H4a2 2 0 00-2 2v4a2 2 0 002 2h16a2 2 0 002-2v-4a2 2 0 00-2-2h-2"/><rect x="6" y="14" width="12" height="8"/></svg>
          打印
        </button>
      </div>
    </div>

    <!-- Main area -->
    <div class="main-area">
      <div class="pages-container">
        <div
          v-for="(page, pageIdx) in pages"
          :key="page.id"
          class="a4-page"
        >
          <div class="page-header">
            <span class="page-label">第 {{ pageIdx + 1 }} 页</span>
            <button class="btn-remove-page" @click="removePage(pageIdx)" v-if="pages.length > 1">删除此页</button>
          </div>

          <div
            v-for="(prob, probIdx) in page.problems"
            :key="prob.id"
            class="error-section"
            :class="{ 'no-border-bottom': probIdx === page.problems.length - 1 }"
          >
            <div class="binding-area">装<br>订<br>区<br>域</div>
            <div class="content-area">
              <div class="question-area">
                <div class="area-label">题目</div>
                <RichTextArea v-model="prob.question" placeholder="粘贴题目文本或图片..." />
              </div>

              <div class="columns-container">
                <div class="column col-thinking">
                  <div class="area-label">思维提示</div>
                  <textarea
                    v-model="prob.thinking"
                    placeholder="1. 换元&#10;2. 分类讨论&#10;3. ..."
                    class="col-textarea"
                  ></textarea>
                </div>
                <div class="column col-answer">
                  <div class="area-label">答案</div>
                  <RichTextArea v-model="prob.answer" placeholder="答案、解题步骤..." />
                </div>
                <div class="column col-ai">
                  <div class="area-label">
                    AI 思维导图
                    <label class="toggle-ai">
                      <input type="checkbox" v-model="prob.showAi" />
                      <span>启用</span>
                    </label>
                  </div>
                  <textarea
                    v-if="prob.showAi"
                    v-model="prob.aiMindmap"
                    placeholder="AI 思维解析链路..."
                    class="col-textarea"
                  ></textarea>
                  <div v-else class="ai-placeholder">可选，勾选「启用」后填写</div>
                </div>
              </div>
            </div>
          </div>
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

/* ---- Sidebar ---- */
.sidebar {
  width: 120px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 8px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  height: fit-content;
  position: sticky;
  top: 10px;
  flex-shrink: 0;
}

.sidebar-title {
  font-size: 13px;
  font-weight: 700;
  color: #1a1a1a;
  text-align: center;
}

.sidebar-hint {
  font-size: 11px;
  color: #999;
  text-align: center;
  line-height: 1.4;
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-label {
  font-size: 11px;
  color: #888;
  font-weight: 600;
}

.sidebar-section button {
  padding: 5px 0;
  border: 1px solid #ddd;
  border-radius: 3px;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.sidebar-section button.active {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 5px 0;
  border: 1px solid #ddd;
  border-radius: 3px;
  background: #fff;
  font-size: 12px;
  color: #555;
  cursor: pointer;
  transition: all 0.15s;
}

.sidebar-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.sidebar-btn.primary {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.sidebar-btn.primary:hover {
  background: #337ecc;
}

/* ---- Main area ---- */
.main-area {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.pages-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ---- A4 Page ---- */
.a4-page {
  width: 210mm;
  min-height: 297mm;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  padding-bottom: 5mm;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4mm 8mm;
  border-bottom: 1px dashed #ddd;
}

.page-label {
  font-size: 13px;
  color: #888;
  font-weight: 600;
}

.btn-remove-page {
  padding: 2px 10px;
  border: 1px solid #eee;
  border-radius: 3px;
  background: #fff;
  font-size: 11px;
  color: #f56c6c;
  cursor: pointer;
}

.btn-remove-page:hover { background: #fef0f0; }

/* ---- Error Section ---- */
.error-section {
  flex: 1;
  display: flex;
  border-bottom: 1px dashed #ccc;
}

.error-section.no-border-bottom { border-bottom: none; }

/* ---- Binding Area ---- */
.binding-area {
  width: 15mm;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #bbb;
  border-right: 1px solid #e8e8e8;
  line-height: 1.6;
  text-align: center;
  padding: 4px 2px;
}

/* ---- Content Area ---- */
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* ---- Question Area ---- */
.question-area {
  padding: 5mm 5mm 3mm;
  border-bottom: 1px solid #eee;
  display: flex;
  flex-direction: column;
  min-height: 80px;
}

.area-label {
  font-size: 12px;
  font-weight: 600;
  color: #888;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* ---- Three Columns ---- */
.columns-container {
  flex: 1;
  display: flex;
}

.column {
  padding: 4mm 4mm;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.column:last-child { border-right: none; }

.col-thinking { width: 20%; }
.col-answer { width: 50%; }
.col-ai { width: 30%; }

.col-textarea {
  flex: 1;
  width: 100%;
  border: none;
  outline: none;
  font-size: 13px;
  line-height: 1.6;
  resize: none;
  font-family: inherit;
  color: #333;
  background: transparent;
  min-height: 0;
}

.toggle-ai {
  font-size: 11px;
  font-weight: 400;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.toggle-ai input { cursor: pointer; }

.ai-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #ccc;
}

/* ---- Print ---- */
@media print {
  body {
    background: #fff;
    padding: 0;
  }

  .sidebar {
    display: none !important;
  }

  .app-wrap {
    background: #fff;
    padding: 0;
  }

  .a4-page {
    box-shadow: none;
    page-break-after: always;
    min-height: auto;
    padding-bottom: 0;
  }

  .a4-page:last-child {
    page-break-after: auto;
  }

  .page-header {
    display: none !important;
  }

  .btn-remove-page {
    display: none !important;
  }

  .col-textarea {
    color: #000;
  }
}
</style>
