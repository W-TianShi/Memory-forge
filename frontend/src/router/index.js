import { createRouter, createWebHashHistory } from 'vue-router'
import WordMemory from '../views/WordMemory.vue'
import NoteExport from '../views/NoteExport.vue'

const routes = [
  { path: '/', name: 'WordMemory', component: WordMemory },
  { path: '/notes', name: 'NoteExport', component: NoteExport },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
