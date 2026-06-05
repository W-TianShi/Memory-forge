import { ref } from 'vue'

export function useToast() {
  const visible = ref(false)
  const message = ref('')
  const type = ref('info')
  let timer = null

  function show(msg, t = 'info', duration = 2000) {
    message.value = msg
    type.value = t
    visible.value = true
    clearTimeout(timer)
    if (t !== 'loading') {
      timer = setTimeout(() => { visible.value = false }, duration)
    }
  }

  function hide() {
    visible.value = false
    clearTimeout(timer)
  }

  return { visible, message, type, show, hide }
}
