import { ref } from 'vue'

export function useVisibility(words) {

  const wordHidden = ref(false)
  const phoneticHidden = ref(false)
  const meaningHidden = ref(0)

  function toggleWordHidden() {
    wordHidden.value = !wordHidden.value
    if (wordHidden.value) {
      words.value.forEach(item => { item._savedWord = item.word; item.word = '' })
    } else {
      words.value.forEach(item => { if (item._savedWord !== undefined) { item.word = item._savedWord; delete item._savedWord } })
    }
  }

  function doBlankMeaning() {
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

  function togglePhoneticHidden() {
    phoneticHidden.value = !phoneticHidden.value
    if (phoneticHidden.value) {
      words.value.forEach(item => { item._savedPhonetic = item.phonetic; item.phonetic = '' })
    } else {
      words.value.forEach(item => { if (item._savedPhonetic !== undefined) { item.phonetic = item._savedPhonetic; delete item._savedPhonetic } })
    }
  }

  function toggleMeaningHidden() {
    if (meaningHidden.value === 0) {
      words.value.forEach(item => { item._savedMeaning = item.meaning })
      doBlankMeaning()
      meaningHidden.value = 1
    } else if (meaningHidden.value === 1) {
      words.value.forEach(item => { item._savedMeaning2 = item.meaning; item.meaning = '' })
      meaningHidden.value = 2
    } else {
      words.value.forEach(item => {
        if (item._savedMeaning !== undefined) { item.meaning = item._savedMeaning; delete item._savedMeaning }
        if (item._savedMeaning2 !== undefined) delete item._savedMeaning2
      })
      meaningHidden.value = 0
    }
  }

  function resetAll() {
    if (wordHidden.value) toggleWordHidden()
    if (phoneticHidden.value) togglePhoneticHidden()
    if (meaningHidden.value > 0) {
      words.value.forEach(item => {
        if (item._savedMeaning !== undefined) { item.meaning = item._savedMeaning; delete item._savedMeaning }
        if (item._savedMeaning2 !== undefined) delete item._savedMeaning2
      })
      meaningHidden.value = 0
    }
  }

  return {
    wordHidden, phoneticHidden, meaningHidden,
    toggleWordHidden, togglePhoneticHidden, toggleMeaningHidden,
    doBlankMeaning, resetAll
  }
}
