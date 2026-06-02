import { ref, computed, watch, nextTick } from 'vue'

const ROWS_PER_PAGE = 25
const MAX_UNDO = 50

const DEFAULT_COUNT = 22

function createEmptyWords(count) {
  return Array.from({ length: count }, (_, i) => ({
    word: '', phonetic: '', meaning: '', originalIndex: i, col: i % 2
  }))
}

export function useWords(contentRef) {

  const words = ref(createEmptyWords(DEFAULT_COUNT))
  const currentPage = ref(0)
  const undoStack = ref([])
  const columnCount = ref(2)

  const pageSize = computed(() => columnCount.value * ROWS_PER_PAGE)
  const totalPages = computed(() => Math.max(1, Math.ceil(words.value.length / pageSize.value)))

  watch(totalPages, () => {
    if (currentPage.value < 0) currentPage.value = 0
    if (currentPage.value >= totalPages.value) {
      currentPage.value = Math.max(0, totalPages.value - 1)
    }
  })

  const pageWords = computed(() => {
    const start = currentPage.value * pageSize.value
    return words.value.slice(start, start + pageSize.value)
  })

  const columns = computed(() => {
    const cols = []
    for (let i = 0; i < columnCount.value; i++) {
      cols.push(pageWords.value.filter(w => w.col === i))
    }
    return cols
  })

  function nextCol() {
    if (words.value.length === 0) return 0
    return (words.value[words.value.length - 1].col + 1) % columnCount.value
  }

  function globalIndex(item) {
    return words.value.findIndex(w => w.originalIndex === item.originalIndex)
  }

  function displayIndex(item) {
    const posInPage = pageWords.value.findIndex(w => w.originalIndex === item.originalIndex)
    return String(posInPage + 1).padStart(2, '0')
  }

  function pushUndo() {
    undoStack.value.push(JSON.stringify(words.value))
    if (undoStack.value.length > MAX_UNDO) undoStack.value.shift()
  }

  function syncFromDOM() {
    const el = contentRef?.value
    if (!el) return
    el.querySelectorAll('.word').forEach(el => {
      const oid = Number(el.dataset.oid)
      const idx = words.value.findIndex(w => w.originalIndex === oid)
      if (idx >= 0) words.value[idx].word = el.textContent || ''
    })
    el.querySelectorAll('.phonetic').forEach(el => {
      const oid = Number(el.dataset.oid)
      const idx = words.value.findIndex(w => w.originalIndex === oid)
      if (idx >= 0) words.value[idx].phonetic = el.textContent || ''
    })
    el.querySelectorAll('.meaning-text').forEach(el => {
      const oid = Number(el.dataset.oid)
      const idx = words.value.findIndex(w => w.originalIndex === oid)
      if (idx >= 0) words.value[idx].meaning = el.textContent || ''
    })
  }

  function undo() {
    if (!undoStack.value.length) return
    syncFromDOM()
    const snapshot = undoStack.value.pop()
    words.value = JSON.parse(snapshot)
    if (currentPage.value >= totalPages.value) currentPage.value = Math.max(0, totalPages.value - 1)
  }

  function onWordKeydown(e, item) {
    if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey) {
      e.preventDefault()
      addWordEnd()
      return
    }
    if ((e.key === 'Backspace' || e.key === 'Delete') && !e.target.textContent.trim()) {
      if (words.value.length <= 1) return
      e.preventDefault()
      removeWord(item)
      return
    }
  }

  function addWordEnd() {
    syncFromDOM()
    pushUndo()
    const item = { word: '', phonetic: '', meaning: '', originalIndex: Date.now(), col: nextCol() }
    words.value.push(item)
    const newPage = Math.floor((words.value.length - 1) / pageSize.value)
    if (newPage !== currentPage.value) currentPage.value = newPage
    nextTick(() => {
      const wordEls = contentRef?.value?.querySelectorAll('.word')
      if (wordEls && wordEls.length > 0) wordEls[wordEls.length - 1].focus()
    })
  }

  function removeWord(item) {
    if (words.value.length <= 1) return
    pushUndo()
    const idx = words.value.findIndex(w => w.originalIndex === item.originalIndex)
    words.value.splice(idx, 1)
    if (currentPage.value >= totalPages.value) currentPage.value = Math.max(0, totalPages.value - 1)
  }

  function addWord() {
    addWordEnd()
  }

  function removeLastWord() {
    if (words.value.length <= 1) return
    syncFromDOM()
    pushUndo()
    words.value.pop()
    if (currentPage.value >= totalPages.value) currentPage.value = Math.max(0, totalPages.value - 1)
  }

  function addPage() {
    syncFromDOM()
    pushUndo()
    const lastPageStart = (totalPages.value - 1) * pageSize.value
    const countOnLastPage = words.value.length - lastPageStart
    const need = pageSize.value - countOnLastPage
    const toAdd = need > 0 ? need : pageSize.value
    const col = nextCol()
    for (let i = 0; i < toAdd; i++) {
      words.value.push({ word: '', phonetic: '', meaning: '', originalIndex: Date.now() + i, col: (col + i) % columnCount.value })
    }
    currentPage.value = totalPages.value - 1
  }

  function deletePage() {
    if (totalPages.value <= 1 && words.value.length <= pageSize.value) return
    if (!confirm(`确定要删除第 ${currentPage.value + 1} 页的全部单词吗？此操作可撤销(Ctrl+Z)。`)) return
    syncFromDOM()
    pushUndo()
    const start = currentPage.value * pageSize.value
    words.value.splice(start, pageSize.value)
    if (currentPage.value >= totalPages.value) currentPage.value = Math.max(0, totalPages.value - 1)
  }

  function batchAddWords(wordList) {
    if (!wordList || wordList.length === 0) return
    syncFromDOM()
    pushUndo()
    let wi = 0
    for (const w of words.value) {
      if (wi >= wordList.length) break
      if (!w.word.trim() && !w.phonetic.trim() && !w.meaning.trim()) {
        w.word = wordList[wi]
        wi++
      }
    }
    const startCol = nextCol()
    for (let i = wi; i < wordList.length; i++) {
      words.value.push({ word: wordList[i], phonetic: '', meaning: '', originalIndex: Date.now() + i, col: (startCol + (i - wi)) % columnCount.value })
    }
    const newPage = Math.floor((words.value.length - 1) / pageSize.value)
    if (newPage !== currentPage.value) currentPage.value = newPage
  }

  function prevPage() { if (currentPage.value > 0) currentPage.value-- }
  function nextPage() { if (currentPage.value < totalPages.value - 1) currentPage.value++ }

  return {
    words, currentPage, totalPages, pageWords, columns, undoStack, columnCount,
    nextCol, globalIndex, displayIndex,
    pushUndo, undo, syncFromDOM,
    onWordKeydown, removeWord, addWord, removeLastWord,
    addPage, deletePage, prevPage, nextPage,
    batchAddWords
  }
}
