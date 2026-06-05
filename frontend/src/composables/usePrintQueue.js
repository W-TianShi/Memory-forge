import { ref, computed } from 'vue'

const STORAGE_KEY = 'mf-print-queue'

const items = ref([])
const panelVisible = ref(false)

function load() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) items.value = JSON.parse(saved)
  } catch { items.value = [] }
}

function save() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items.value))
}

function add(item) {
  items.value.push({
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
    timestamp: Date.now(),
    newSheet: false,
    landscape: false,
    gridType: null,
    gridColor: null,
    blankHtml: null,
    blank: false,
    ...item
  })
  save()
}

function remove(id) {
  items.value = items.value.filter(i => i.id !== id)
  save()
}

function move(fromIdx, toIdx) {
  const arr = items.value
  const item = arr.splice(fromIdx, 1)[0]
  arr.splice(toIdx, 0, item)
  save()
}

function insertBlank(afterId) {
  const idx = items.value.findIndex(i => i.id === afterId)
  items.value.splice(idx + 1, 0, {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 6),
    source: 'blank',
    title: '空白页',
    blank: true,
    timestamp: Date.now()
  })
  save()
}

function clearAll() {
  items.value = []
  save()
}

function togglePanel() {
  panelVisible.value = !panelVisible.value
}

const count = computed(() => items.value.length)

// Load persisted queue on module init
load()

export function usePrintQueue() {
  return { items, count, panelVisible, add, remove, move, insertBlank, clearAll, togglePanel }
}
