<script setup>
import { watch, ref } from 'vue'
import { useMathProblems } from '../composables/useMathProblems.js'

const { selected, categories, thinkingLabels, statusLabels, update } = useMathProblems()

const form = ref({ title: '', content: '', category: '函数', status: 'not_started', attempts: 0, notes: '' })

watch(selected, (s) => {
  if (s) {
    form.value = { title: s.title, content: s.content, category: s.category, status: s.status, attempts: s.attempts, notes: s.notes }
  }
}, { immediate: true })

function saveField(field, value) {
  if (!selected.value) return
  update(selected.value.id, { [field]: value })
}

const emit = defineEmits(['sendToAI'])
function sendToAI() {
  if (selected.value) emit('sendToAI', selected.value)
}
</script>

<template>
  <div class="problem-edit" v-if="selected">
    <div class="edit-section">
      <label>题目名称</label>
      <input
        v-model="form.title"
        placeholder="例如：2024全国卷I 第17题"
        @input="saveField('title', form.title)"
      />
    </div>

    <div class="edit-section">
      <label>分类</label>
      <select v-model="form.category" @change="saveField('category', form.category)">
        <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
      </select>
    </div>

    <div class="edit-section">
      <label>题目内容</label>
      <textarea
        v-model="form.content"
        placeholder="粘贴或输入题目正文..."
        rows="6"
        @input="saveField('content', form.content)"
      ></textarea>
    </div>

    <div class="edit-section">
      <label>适用思维（可多选）</label>
      <div class="thinking-select">
        <label
          v-for="(t, i) in thinkingLabels" :key="i"
          class="thinking-check"
          :class="{ checked: selected.thinkingIds && selected.thinkingIds.includes(i + 1) }"
          @click="useMathProblems().toggleThinking(selected.id, i + 1)"
        >
          思维{{ i + 1 }}：{{ t }}
        </label>
      </div>
    </div>

    <div class="edit-section">
      <label>掌握度</label>
      <div class="status-select">
        <button
          v-for="(label, key) in statusLabels" :key="key"
          class="status-btn"
          :class="{ active: form.status === key }"
          @click="form.status = key; saveField('status', key)"
        >{{ label }}</button>
      </div>
    </div>

    <div class="edit-section">
      <label>练习次数：{{ form.attempts }}</label>
    </div>

    <div class="edit-section">
      <label>个人笔记</label>
      <textarea
        v-model="form.notes"
        placeholder="解题心得、易错点..."
        rows="3"
        @input="saveField('notes', form.notes)"
      ></textarea>
    </div>

    <button class="send-ai-btn" @click="sendToAI">发给思维AI分析</button>
  </div>

  <div class="problem-edit empty" v-else>
    <div class="no-select">← 选择一个题目，或添加新题目</div>
  </div>
</template>

<style scoped>
.problem-edit {
  border-top: 1px solid #e8e8e8;
  padding-top: 12px;
  overflow-y: auto;
  max-height: 45%;
  flex-shrink: 0;
}
.problem-edit.empty {
  display: flex;
  align-items: center;
  justify-content: center;
}
.no-select {
  color: #bbb;
  font-size: 14px;
}

.edit-section {
  margin-bottom: 10px;
}
.edit-section label {
  display: block;
  font-size: 12px;
  color: #888;
  margin-bottom: 4px;
  font-weight: 500;
}
.edit-section input,
.edit-section select,
.edit-section textarea {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 13px;
  font-family: inherit;
  color: #333;
  outline: none;
  resize: vertical;
}
.edit-section input:focus,
.edit-section select:focus,
.edit-section textarea:focus {
  border-color: #409eff;
}

.thinking-select {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.thinking-check {
  font-size: 11px;
  padding: 3px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  color: #888;
  transition: all 0.15s;
  user-select: none;
}
.thinking-check:hover { border-color: #409eff; color: #409eff; }
.thinking-check.checked { background: #ecf5ff; border-color: #409eff; color: #409eff; }

.status-select {
  display: flex;
  gap: 6px;
}
.status-btn {
  padding: 4px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.status-btn:hover { border-color: #409eff; color: #409eff; }
.status-btn.active { background: #409eff; color: #fff; border-color: #409eff; }

.send-ai-btn {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(135deg, #409eff, #6366f1);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.send-ai-btn:hover { opacity: 0.9; transform: translateY(-1px); }
</style>
