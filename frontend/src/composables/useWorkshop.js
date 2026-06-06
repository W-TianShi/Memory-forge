import { ref, computed } from 'vue'

const STORAGE_KEY = 'mf-workshop-docs'

const docs = ref([])
const currentDocId = ref(null)
const zoom = ref(0.6)
const bgDark = ref(true)

// Annotations: { [docId]: { [pageIndex]: [ { id, x, y, html, fontSize, color }, ... ] } }
const annotations = ref({})

// Undo stacks per doc
const undoStacks = ref({})
const redoStacks = ref({})

function load() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) docs.value = JSON.parse(saved)
  } catch { docs.value = [] }
}

function save() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(docs.value))
}

function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 6)
}

function addDoc(doc) {
  const id = generateId()
  docs.value.push({
    id,
    timestamp: Date.now(),
    ...doc
  })
  save()
  return id
}

function removeDoc(id) {
  docs.value = docs.value.filter(d => d.id !== id)
  if (currentDocId.value === id) currentDocId.value = null
  save()
}

function clearAll() {
  docs.value = []
  annotations.value = {}
  undoStacks.value = {}
  redoStacks.value = {}
  currentDocId.value = null
  save()
}

const currentDoc = computed(() => docs.value.find(d => d.id === currentDocId.value) || null)

const pageCount = computed(() => {
  const doc = currentDoc.value
  if (!doc || !doc.pages) return 0
  return doc.pages.length
})

// ---- Annotation CRUD ----

function getAnnotations(docId, pageIdx) {
  if (!annotations.value[docId]) annotations.value[docId] = {}
  if (!annotations.value[docId][pageIdx]) annotations.value[docId][pageIdx] = []
  return annotations.value[docId][pageIdx]
}

function pushUndo(docId) {
  if (!undoStacks.value[docId]) undoStacks.value[docId] = []
  if (!redoStacks.value[docId]) redoStacks.value[docId] = []
  const snap = JSON.parse(JSON.stringify(annotations.value[docId] || {}))
  undoStacks.value[docId].push(snap)
  if (undoStacks.value[docId].length > 50) undoStacks.value[docId].shift()
  redoStacks.value[docId] = []
}

function addAnnotation(docId, pageIdx, ann) {
  pushUndo(docId)
  const list = getAnnotations(docId, pageIdx)
  list.push({
    id: generateId(),
    x: ann.x || 20,
    y: ann.y || 20,
    html: ann.html || '',
    fontSize: ann.fontSize || 14,
    color: ann.color || '#333333',
  })
}

function updateAnnotation(docId, pageIdx, annId, updates) {
  pushUndo(docId)
  const list = getAnnotations(docId, pageIdx)
  const idx = list.findIndex(a => a.id === annId)
  if (idx !== -1) Object.assign(list[idx], updates)
}

function removeAnnotation(docId, pageIdx, annId) {
  pushUndo(docId)
  if (!annotations.value[docId]) return
  const list = annotations.value[docId][pageIdx]
  if (!list) return
  annotations.value[docId][pageIdx] = list.filter(a => a.id !== annId)
}

function undo(docId) {
  if (!undoStacks.value[docId] || undoStacks.value[docId].length === 0) return
  if (!redoStacks.value[docId]) redoStacks.value[docId] = []
  const current = JSON.parse(JSON.stringify(annotations.value[docId] || {}))
  redoStacks.value[docId].push(current)
  const prev = undoStacks.value[docId].pop()
  annotations.value[docId] = JSON.parse(JSON.stringify(prev))
}

function redo(docId) {
  if (!redoStacks.value[docId] || redoStacks.value[docId].length === 0) return
  if (!undoStacks.value[docId]) undoStacks.value[docId] = []
  const current = JSON.parse(JSON.stringify(annotations.value[docId] || {}))
  undoStacks.value[docId].push(current)
  const next = redoStacks.value[docId].pop()
  annotations.value[docId] = JSON.parse(JSON.stringify(next))
}

function canUndo(docId) {
  return undoStacks.value[docId] && undoStacks.value[docId].length > 0
}

function canRedo(docId) {
  return redoStacks.value[docId] && redoStacks.value[docId].length > 0
}

load()

export function useWorkshop() {
  return {
    docs,
    currentDocId,
    currentDoc,
    pages: computed(() => currentDoc.value?.pages || []),
    pageCount,
    zoom,
    bgDark,
    annotations,
    addDoc,
    removeDoc,
    clearAll,
    getAnnotations,
    addAnnotation,
    updateAnnotation,
    removeAnnotation,
    undo,
    redo,
    canUndo,
    canRedo,
  }
}
