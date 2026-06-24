<script setup>
import { ref, computed, nextTick, onUnmounted } from 'vue'
import { marked } from 'marked'
import katex from 'katex'
import { apiFetch } from '../api/apiClient.js'

const props = defineProps({
  selectedProblem: Object
})

let abortController = null

onUnmounted(() => {
  if (abortController) abortController.abort()
})

const mode = ref('full')
const userInput = ref('')
const messages = ref([])
const streaming = ref(false)
const rawStream = ref('')

const chatEl = ref(null)

function scrollToBottom() {
  nextTick(() => {
    if (chatEl.value) chatEl.value.scrollTop = chatEl.value.scrollHeight
  })
}

// Real-time rendered HTML during streaming
const streamingHtml = computed(() => {
  if (!rawStream.value) return ''
  return renderFinal(rawStream.value)
})

function addMessage(role, content) {
  messages.value.push({ role, content, html: renderFinal(content), time: Date.now() })
  scrollToBottom()
}

// ---- Common KaTeX options ----

const KATEX_OPTS = {
  throwOnError: false,
  strict: false,         // tolerate malformed LaTeX
  trust: true,           // allow \html... commands
  errorColor: '#d00',
}

function katexOpts(displayMode) {
  return { ...KATEX_OPTS, displayMode }
}

// ---- Final render (after streaming completes): full KaTeX + Markdown ----

function renderFinal(text) {
  if (!text) return ''
  const preprocessed = preprocessMarkdown(text)
  const fixed = fixLatex(preprocessed)
  const blocks = []
  let processed = fixed.replace(/\$\$([\s\S]*?)\$\$/g, (_, f) => {
    blocks.push(renderKatex(f, true))
    return `@@B${blocks.length - 1}@@`
  })
  processed = processed.replace(/\$(.+?)\$/g, (_, f) => {
    blocks.push(renderKatex(f, false))
    return `@@B${blocks.length - 1}@@`
  })
  // Protect raw LaTeX commands (\ln, \to, etc.) from markdown backslash escaping
  processed = processed.replace(/\\([a-zA-Z]+)/g, '\\\\$1')
  let html = marked.parse(processed, { breaks: true })
  return html.replace(/@@B(\d+)@@/g, (_, id) => blocks[parseInt(id)])
}

// Insert proper line breaks into AI output that lacks them
function preprocessMarkdown(text) {
  let t = text

  // Step 0: Add space after ###/##/# when missing (AI often outputs "###text" without space)
  t = t.replace(/^(#{1,4})([^\s#])/gm, '$1 $2')

  // Step 1: --- separator: ensure it stands alone as its own line
  // Cases: "text---###", "text---text", "---###", etc.
  t = t.replace(/---(#{1,4}\s?)/g, '\n\n---\n\n$1')     // ---### → \n\n---\n\n###
  t = t.replace(/---(\*\*)/g, '\n\n---\n\n$1')           // ---** → \n\n---\n\n**
  t = t.replace(/([^\n-])---([^\n-])/g, '$1\n\n---\n\n$2') // text---text → text\n\n---\n\ntext
  t = t.replace(/([^\n])---$/g, '$1\n\n---')              // end of line: text--- → text\n\n---
  t = t.replace(/^---([^\n])/gm, '---\n\n$1')             // start: ---text → ---\n\ntext

  // ### / ## / # headers: ensure blank line before
  // Cases: "text### Title", "。### Title"
  t = t.replace(/([^\n#])(#{1,4}\s)/g, '$1\n\n$2')

  // Push list markers (* -) after heading to next line
  t = t.replace(/^(#{1,4}\s[^\n]{1,60}?)([\*\-]\s+)/gm, '$1\n$2')

  // **Bold header** pattern like **第一步：...**  or **为什么用...**
  // Ensure these start on new lines with blank line before
  t = t.replace(/([。！？\.])(\*\*[^\*]+\*\*)/g, '$1\n\n$2')

  // Numbered steps: "步骤：1.xxx 2.xxx" → each on new line
  t = t.replace(/([：:])\s*(\d+[\.\、])/g, '$1\n$2')
  // Also split consecutive numbered items on same line
  t = t.replace(/([。；;])\s*(\d+[\.\、])/g, '$1\n$2')

  // Ensure "适用条件" / "标准步骤" / "易错点" type labels start on new lines
  t = t.replace(/([。！？\.])(\*\*适用)/g, '$1\n\n$2')
  t = t.replace(/([。！？\.])(\*\*标准)/g, '$1\n\n$2')
  t = t.replace(/([。！？\.])(\*\*易错)/g, '$1\n\n$2')

  // Normalize: 3+ newlines → 2 newlines (no excessive whitespace)
  t = t.replace(/\n{3,}/g, '\n\n')

  return t
}

function renderKatex(formula, displayMode) {
  try {
    return katex.renderToString(fixLatex(formula.trim()), katexOpts(displayMode))
  } catch (e) {
    return `<code>${escapeHtml(formula)}</code>`
  }
}

// ---- LaTeX fixer ----

const LATEX_FUNCTIONS = [
  'ln', 'log', 'lg', 'sin', 'cos', 'tan', 'csc', 'sec', 'cot',
  'arcsin', 'arccos', 'arctan', 'sinh', 'cosh', 'tanh',
  'exp', 'det', 'gcd', 'deg', 'dim', 'hom', 'ker', 'arg',
  'lim', 'max', 'min', 'sup', 'inf', 'Pr',
  'mathrm', 'mathit', 'mathbf', 'mathsf', 'mathtt', 'mathbb', 'mathcal', 'mathfrak',
  'operatorname', 'text', 'textbf', 'textit', 'texttt',
  'boxed', 'overline', 'underline', 'widehat', 'widetilde',
  'sqrt', 'frac', 'binom',
  'to', 'rightarrow', 'leftarrow', 'Rightarrow', 'Leftarrow',
  'infty', 'cdot', 'times', 'div', 'pm', 'mp',
  'leq', 'geq', 'neq', 'approx', 'equiv', 'sim',
  'forall', 'exists', 'in', 'notin', 'subset', 'supset',
  'cup', 'cap', 'setminus', 'emptyset', 'varnothing',
  'alpha', 'beta', 'gamma', 'delta', 'epsilon', 'theta', 'lambda', 'mu', 'pi', 'sigma', 'phi', 'omega',
  'sum', 'prod', 'int', 'oint', 'partial', 'nabla',
]

function fixLatex(text) {
  const cmdList = LATEX_FUNCTIONS.join('|')
  const re = new RegExp(`\\\\(${cmdList})([a-zA-Z\\d])`, 'g')
  return text.replace(re, '\\$1 $2')
}

// ---- Chat logic ----

async function send(msg) {
  const text = msg || userInput.value.trim()
  if (!text || streaming.value) return

  if (!msg) userInput.value = ''
  addMessage('user', text)

  streaming.value = true
  rawStream.value = ''

  const problemContent = props.selectedProblem?.content || ''

  const body = {
    problemId: props.selectedProblem?.id || null,
    problemContent: problemContent,
    message: problemContent && !msg ? ('请分析这道数学题：\n' + problemContent) : text,
    mode: mode.value,
    history: messages.value.slice(0, -1).map(m => ({ role: m.role, content: m.content }))
  }

  try {
    if (abortController) abortController.abort()
    abortController = new AbortController()
    const response = await apiFetch('/api/ai/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      signal: abortController.signal
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const parts = buffer.split('\n\n')
      buffer = parts.pop()

      for (const part of parts) {
        const lines = part.split('\n')
        for (const line of lines) {
          if (line.startsWith('data:')) {
            rawStream.value += line.replace(/^data: ?/, '')
          }
        }
      }
      scrollToBottom()
    }
    if (buffer.trim()) {
      const lines = buffer.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          rawStream.value += line.replace(/^data: ?/, '')
        }
      }
    }
  } catch (e) {
    rawStream.value += '\n\n[错误] 无法连接到AI服务，请检查网络或API Key配置'
  }

  if (rawStream.value) {
    addMessage('assistant', rawStream.value)
  }
  rawStream.value = ''
  streaming.value = false
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

function clearChat() {
  messages.value = []
}

defineExpose({ send, clearChat })
</script>

<template>
  <div class="thinking-ai">
    <div class="ai-header">
      <div class="mode-switch">
        <button :class="{ active: mode === 'full' }" @click="mode = 'full'">一次性讲解</button>
        <button :class="{ active: mode === 'step' }" @click="mode = 'step'">分步引导</button>
      </div>
      <button class="clear-btn" @click="clearChat" title="清空对话">清空</button>
    </div>

    <div class="chat-messages" ref="chatEl">
      <div v-if="messages.length === 0 && !streaming" class="welcome">
        <p><strong>高中数学八大核心解题思维</strong></p>
        <p>选择一个题目后点击「发给思维AI分析」，或直接输入你想问的数学问题。</p>
        <p class="welcome-hint">
          一次性讲解：AI 用八大思维完整分析<br>
          分步引导：AI 每次只讲一个环节，引导你思考
        </p>
      </div>

      <div
        v-for="(m, i) in messages" :key="i"
        class="chat-msg"
        :class="m.role"
      >
        <div v-if="m.role === 'user'" class="msg-bubble user-bubble" v-text="m.content"></div>
        <div v-else class="msg-bubble msg-html" v-html="m.html"></div>
      </div>

      <div v-if="streaming" class="chat-msg assistant">
        <div class="msg-bubble msg-html streaming-bubble" v-html="streamingHtml"></div>
      </div>
    </div>

    <div class="chat-input-area">
      <textarea
        v-model="userInput"
        placeholder="输入你的数学问题... (Enter 发送)"
        rows="2"
        @keydown="handleKeydown"
        :disabled="streaming"
      ></textarea>
      <button
        class="send-btn"
        :disabled="!userInput.trim() || streaming"
        @click="send()"
      >发送</button>
    </div>
  </div>
</template>

<style scoped>
.thinking-ai {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 10px;
}

.mode-switch { display: flex; gap: 4px; }
.mode-switch button {
  padding: 4px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.mode-switch button:hover { border-color: #409eff; color: #409eff; }
.mode-switch button.active { background: #409eff; color: #fff; border-color: #409eff; }

.clear-btn {
  padding: 4px 10px;
  border: 1px solid #eee;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  color: #999;
  cursor: pointer;
}
.clear-btn:hover { color: #f56c6c; border-color: #f56c6c; }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 4px 2px;
}

.welcome {
  text-align: center;
  color: #666;
  font-size: 13px;
  line-height: 1.8;
  padding: 20px 10px;
}
.welcome-hint {
  color: #888;
  font-size: 12px;
  margin-top: 8px;
}

.chat-msg {
  margin-bottom: 14px;
}

.user-bubble {
  background: #ecf5ff;
  color: #333;
  float: right;
  max-width: 85%;
}

.msg-bubble {
  display: inline-block;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 15px;
  line-height: 1.8;
  word-break: break-word;
  background: #f5f7fa;
  color: #1d1d1d;
  max-width: 95%;
}

.streaming-bubble::after {
  content: '';
  display: inline-block;
  width: 2px;
  height: 1em;
  background: #409eff;
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 1s infinite;
}
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* ====== Markdown / LaTeX typography (ChatGPT-style) ====== */
.msg-html :deep(h1) {
  margin: 20px 0 10px;
  font-size: 1.15em;
  font-weight: 700;
  color: #111;
  border-bottom: 1px solid #e5e5e5;
  padding-bottom: 8px;
}
.msg-html :deep(h2) {
  margin: 16px 0 8px;
  font-size: 1.08em;
  font-weight: 700;
  color: #1a1a1a;
}
.msg-html :deep(h3) {
  margin: 14px 0 8px;
  font-size: 1.02em;
  font-weight: 700;
  color: #222;
}
.msg-html :deep(h4) {
  margin: 12px 0 6px;
  font-size: 1em;
  font-weight: 600;
  color: #333;
}
.msg-html :deep(p) { margin: 8px 0; }
.msg-html :deep(strong) { color: #d84315; font-weight: 700; }
.msg-html :deep(hr) {
  border: none;
  height: 1px;
  background: #e0e0e0;
  margin: 20px 0;
}
.msg-html :deep(ul), .msg-html :deep(ol) {
  padding-left: 24px;
  margin: 10px 0;
}
.msg-html :deep(li) { margin: 5px 0; }
.msg-html :deep(li)::marker { color: #666; }
.msg-html :deep(code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.88em;
  color: #c7254e;
}
.msg-html :deep(pre) {
  background: #f8f8f8;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
  overflow-x: auto;
  margin: 12px 0;
}
.msg-html :deep(.katex) { font-size: 1.05em; }
.msg-html :deep(.katex-display) {
  margin: 14px 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 6px 0;
}
.msg-html :deep(blockquote) {
  border-left: 3px solid #94a3b8;
  padding: 8px 14px;
  margin: 12px 0;
  color: #555;
  background: #f8fafc;
  border-radius: 0 6px 6px 0;
}
.msg-html :deep(table) {
  border-collapse: collapse;
  margin: 12px 0;
  width: 100%;
}
.msg-html :deep(th) {
  background: #f5f7fa;
  border: 1px solid #ddd;
  padding: 8px 12px;
  font-size: 0.9em;
  font-weight: 600;
}
.msg-html :deep(td) {
  border: 1px solid #ddd;
  padding: 6px 12px;
  font-size: 0.9em;
}
.msg-html :deep(em) { color: #555; }
.msg-html :deep(br) { display: block; content: ''; margin-top: 4px; }

.chat-input-area {
  display: flex;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid #e8e8e8;
}
.chat-input-area textarea {
  flex: 1;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  font-family: inherit;
  resize: none;
  outline: none;
}
.chat-input-area textarea:focus { border-color: #409eff; }
.send-btn {
  padding: 0 16px;
  border: none;
  border-radius: 6px;
  background: #409eff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.send-btn:hover:not(:disabled) { background: #337ecc; }
.send-btn:disabled { opacity: 0.5; cursor: default; }
</style>
