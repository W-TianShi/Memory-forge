import { ref, computed, watch } from 'vue'

const STORAGE_KEY = 'math_problems'

const categories = ['函数', '三角', '数列', '几何', '向量', '概率', '不等式', '导数', '复数', '其他']
const thinkingLabels = ['化简优先', '隐藏条件挖掘', '题型归类+模板匹配', '等价转化', '逆向反推', '分类讨论', '最值与边界优先', '全局最优路径']
const statusLabels = { mastered: '已掌握', learning: '学习中', not_started: '未开始' }
const statusColors = { mastered: '#388e3c', learning: '#f57c00', not_started: '#999' }

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch { return [] }
}

const problems = ref(load())
const filterCategory = ref('全部')
const filterStatus = ref('全部')
const filterThinking = ref(0) // 0 = all
const selectedId = ref(null)

const selected = computed(() => problems.value.find(p => p.id === selectedId.value) || null)

const filtered = computed(() => {
  let list = problems.value
  if (filterCategory.value !== '全部') {
    list = list.filter(p => p.category === filterCategory.value)
  }
  if (filterStatus.value !== '全部') {
    list = list.filter(p => p.status === filterStatus.value)
  }
  if (filterThinking.value > 0) {
    list = list.filter(p => p.thinkingIds && p.thinkingIds.includes(filterThinking.value))
  }
  return list.sort((a, b) => b.createdAt - a.createdAt)
})

function save() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(problems.value))
}

function add() {
  const p = {
    id: Date.now(),
    title: '',
    content: '',
    category: '函数',
    thinkingIds: [],
    status: 'not_started',
    attempts: 0,
    notes: '',
    createdAt: Date.now()
  }
  problems.value.push(p)
  selectedId.value = p.id
  save()
  return p
}

function update(id, fields) {
  const idx = problems.value.findIndex(p => p.id === id)
  if (idx >= 0) {
    problems.value[idx] = { ...problems.value[idx], ...fields, updatedAt: Date.now() }
    save()
  }
}

function remove(id) {
  problems.value = problems.value.filter(p => p.id !== id)
  if (selectedId.value === id) {
    selectedId.value = problems.value.length > 0 ? problems.value[0].id : null
  }
  save()
}

function selectProblem(id) {
  selectedId.value = id
}

function toggleThinking(id, tId) {
  const p = problems.value.find(p => p.id === id)
  if (!p) return
  if (!p.thinkingIds) p.thinkingIds = []
  const idx = p.thinkingIds.indexOf(tId)
  if (idx >= 0) p.thinkingIds.splice(idx, 1)
  else p.thinkingIds.push(tId)
  save()
}

function incrementAttempt(id) {
  const p = problems.value.find(p => p.id === id)
  if (p) { p.attempts = (p.attempts || 0) + 1; save() }
}

export function useMathProblems() {
  return {
    problems, filtered, selected, selectedId,
    filterCategory, filterStatus, filterThinking,
    categories, thinkingLabels, statusLabels, statusColors,
    add, update, remove, selectProblem,
    toggleThinking, incrementAttempt, save
  }
}
