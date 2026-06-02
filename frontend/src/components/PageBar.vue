<script setup>
defineProps({
  currentPage: { type: Number, required: true },
  totalPages: { type: Number, required: true }
})

const emit = defineEmits(['prevPage', 'nextPage', 'addPage', 'deletePage'])
</script>

<template>
  <div class="bottom-bar">
    <div class="page-nav" v-if="totalPages > 1">
      <span class="page-arrow" :class="{ disabled: currentPage <= 0 }" @click="emit('prevPage')">◀</span>
      <span class="page-num">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <span class="page-arrow" :class="{ disabled: currentPage >= totalPages - 1 }" @click="emit('nextPage')">▶</span>
    </div>
    <div class="page-actions">
      <button class="page-btn" @click="emit('addPage')">+ 添加一页</button>
      <button class="page-btn del" @click="emit('deletePage')">- 删除本页</button>
    </div>
  </div>
</template>

<style scoped>
.bottom-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  user-select: none;
}

.page-nav {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-arrow {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  color: #606266;
  transition: all 0.15s;
}
.page-arrow:hover:not(.disabled) { background: #ecf5ff; color: #409eff; border-color: #409eff; }
.page-arrow.disabled { opacity: 0.3; cursor: default; }

.page-num {
  font-size: 14px;
  color: #666;
  min-width: 48px;
  text-align: center;
}

.page-actions {
  display: flex;
  gap: 8px;
}

.page-btn {
  padding: 6px 14px;
  font-size: 13px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
  cursor: pointer;
  transition: all 0.15s;
}
.page-btn:hover { background: #ecf5ff; color: #409eff; border-color: #409eff; }
.page-btn.del:hover { background: #fef0f0; color: #f56c6c; border-color: #f56c6c; }

@media print {
  .bottom-bar { display: none; }
}
</style>
