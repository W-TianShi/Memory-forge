<template>
  <Teleport to="body">
    <div class="toast-overlay" :class="{ show: visible }">
      <div class="toast-body">
        <span v-if="type === 'loading'" class="toast-spinner"></span>
        <svg v-else-if="type === 'success'" class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
        <svg v-else-if="type === 'error'" class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        <span class="toast-msg">{{ message }}</span>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({
  visible: Boolean,
  message: String,
  type: { type: String, default: 'info' }
})
</script>

<style scoped>
.toast-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.25s ease;
}
.toast-overlay.show { opacity: 1; }

.toast-body {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 24px;
  background: rgba(30, 30, 30, 0.82);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: #fff;
  border-radius: 10px;
  font-size: 14px;
  box-shadow: 0 8px 32px rgba(0,0,0,.18), 0 1px 4px rgba(0,0,0,.08);
  max-width: 80vw;
}

.toast-spinner {
  width: 18px; height: 18px;
  border: 2px solid rgba(255,255,255,.25);
  border-top-color: #fff;
  border-radius: 50%;
  animation: toast-spin .6s linear infinite;
  flex-shrink: 0;
}
@keyframes toast-spin { to { transform: rotate(360deg); } }

.toast-icon {
  width: 18px; height: 18px;
  flex-shrink: 0;
  stroke: #fff;
}

.toast-msg {
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
