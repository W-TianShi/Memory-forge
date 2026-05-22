<template>
  <div class="a4-container">
    <div class="header">
      <div class="title">单词记忆练习表（最终完美版）</div>
      <div class="btn-group">
        <button @click="searchAll">一键查询音标+释义</button>
        <button @click="blankWord">单词挖空</button>
        <button @click="blankMeaning">释义挖空</button>
        <button @click="window.print()">打印A4</button>
      </div>
    </div>

    <div class="grid-container">
      <div class="table-column" v-for="(colData, colIdx) in [col1, col2]" :key="colIdx">
        <div class="table-header">
          <div>序号</div>
          <div>单词 / 音标</div>
          <div>释义</div>
        </div>
        <div class="table-row" v-for="(item, rowIdx) in colData" :key="item.originalIndex">
          <div class="index">{{ String(item.originalIndex + 1).padStart(2, '0') }}</div>
          <div class="word-section">
            <input class="word" v-model="item.word">
            <input class="phonetic" v-model="item.phonetic">
          </div>
          <div
            class="meaning-text"
            contenteditable="true"
            @input="e => item.meaning = ($event.target).innerText"
            v-text="item.meaning"
          ></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const wordList = [
  "unfortunately", "universal", "upset", "vague", "vary",
  "vast", "version", "violate", "virtue", "vital", "vote",
  "abandon", "ability", "able", "abroad", "absent",
  "accept", "access", "accident", "account", "achieve"
]

const words = ref(wordList.map((w, i) => ({
  word: w,
  phonetic: '',
  meaning: '',
  originalIndex: i
})))

const half = computed(() => Math.ceil(words.value.length / 2))
const col1 = computed(() => words.value.slice(0, half.value))
const col2 = computed(() => words.value.slice(half.value))

async function searchAll() {
  for (let i = 0; i < words.value.length; i++) {
    const w = words.value[i].word.trim()
    if (!w) continue

    try {
      const pRes = await fetch(`/api/word/phonetic?word=${encodeURIComponent(w)}`)
      words.value[i].phonetic = (await pRes.text()) || ''
    } catch (e) {
      words.value[i].phonetic = ''
    }

    try {
      const mRes = await fetch(`/api/word/search?word=${encodeURIComponent(w)}`)
      const data = await mRes.json()
      words.value[i].meaning = data?.desc || ''
    } catch (e) {
      words.value[i].meaning = ''
    }
  }
}

function blankWord() {
  words.value.forEach(item => {
    const w = item.word.trim()
    if (w.length > 2) {
      item.word = w[0] + '*'.repeat(w.length - 2) + w.at(-1)
    }
  })
}

function blankMeaning() {
  words.value.forEach(item => {
    let text = (item.meaning || '').trim()
    if (!text) return

    text = text.replace(/[()<>]/g, '').trim()

    const posPattern = /(vt\.|vi\.|n\.|adj\.|adv\.|prep\.|conj\.|pron\.|num\.|art\.|int\.|aux\.|modal\.|v\.|v\.t\.|v\.i\.)/gi
    let newText = text.replace(posPattern, match => `${match} ________`)

    newText = newText.replace(/[一-龥,.，、;；:：]+/g, ' ')
    newText = newText.replace(/\s+/g, ' ').trim()
    newText = newText.replace(/[,;]+$/, '')

    if (!newText.includes('________')) newText = '________'

    item.meaning = newText
  })
}
</script>

<style scoped>
@page {
  size: A4;
  margin: 10mm;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: "SimSun", "Microsoft YaHei", sans-serif;
}

.a4-container {
  width: 210mm;
  min-height: 297mm;
  margin: 0 auto;
  padding: 12mm;
  background: #fff;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10mm;
}

.title {
  font-size: 14pt;
  font-weight: bold;
}

.btn-group {
  display: flex;
  gap: 8px;
}

button {
  padding: 5px 10px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 9pt;
  cursor: pointer;
}

.grid-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8mm;
}

.table-column {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.table-header {
  display: grid;
  grid-template-columns: 10mm 35mm 1fr;
  padding: 3px 5px;
  background: #f0f5f9;
  border: 1px solid #ddd;
  font-size: 9pt;
  font-weight: bold;
  color: #555;
}

.table-row {
  display: grid;
  grid-template-columns: 10mm 35mm 1fr;
  align-items: start;
  gap: 4px;
  padding: 3px 5px;
  border: 1px solid #ddd;
  min-height: 8mm;
}

.index {
  font-size: 9pt;
  color: #666;
  text-align: center;
  padding-top: 2px;
}

.word-section {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.word {
  font-size: 10pt;
  font-weight: 600;
  color: #222;
  border: none;
  background: transparent;
  padding: 1px 3px;
  outline: none;
  width: 100%;
}

.phonetic {
  font-size: 7pt;
  color: #666;
  border: none;
  background: transparent;
  padding: 1px 3px;
  outline: none;
  width: 100%;
}

.meaning-text {
  border: none;
  background: transparent;
  font-size: 9pt;
  padding: 2px 4px;
  width: 100%;
  white-space: normal;
  word-wrap: break-word;
  height: auto;
  min-height: 16px;
  line-height: 1.4;
  outline: none;
}

@media print {
  .btn-group {
    display: none;
  }
}
</style>
