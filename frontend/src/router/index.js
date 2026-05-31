import { createRouter, createWebHashHistory } from 'vue-router'
import WordMemory from '../views/WordMemory.vue'
import NoteExport from '../views/NoteExport.vue'
import MathAI from '../views/MathAI.vue'
import MistakesBook from '../views/MistakesBook.vue'

const routes = [
  { path: '/', name: 'NoteExport', component: NoteExport },
  { path: '/words', name: 'WordMemory', component: WordMemory },
  { path: '/math-ai', name: 'MathAI', component: MathAI },
  { path: '/mistakes', name: 'MistakesBook', component: MistakesBook },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
