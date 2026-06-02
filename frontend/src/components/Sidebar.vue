<script setup>
import SvgIcon from './SvgIcon.vue'

defineProps({
  wordHidden: Boolean,
  phoneticHidden: Boolean,
  meaningHidden: Number,
  timestampVisible: Boolean
})

const emit = defineEmits([
  'searchAll', 'toggleWordHidden', 'togglePhoneticHidden', 'toggleMeaningHidden',
  'resetAll', 'addWord', 'removeLastWord', 'batchImport', 'exportPdf',
  'toggleTimestamp', 'toggleColumns'
])
</script>

<template>
  <div class="sidebar">
    <div class="btn-group">
      <div class="icon-btn" title="一键查询音标+释义" @click="emit('searchAll')">
        <SvgIcon name="bookSearch" />
      </div>
      <div class="icon-btn" title="隐藏单词" @click="emit('toggleWordHidden')" :class="{ active: wordHidden }">
        <SvgIcon name="wordBlank" />
      </div>
      <div class="icon-btn" title="隐藏音标" @click="emit('togglePhoneticHidden')" :class="{ active: phoneticHidden }">
        <SvgIcon name="phonetic" />
      </div>
      <div class="icon-btn" title="隐藏释义" @click="emit('toggleMeaningHidden')" :class="{ active: meaningHidden > 0 }">
        <SvgIcon name="meaningBlank" />
      </div>
      <div class="icon-btn" title="复原" @click="emit('resetAll')">
        <SvgIcon name="rotateCcw" />
      </div>
      <div class="bar-sep"></div>
      <div class="icon-btn" title="增加单词框" @click="emit('addWord')">
        <SvgIcon name="addWordBox" />
      </div>
      <div class="icon-btn" title="删除单词框" @click="emit('removeLastWord')">
        <SvgIcon name="delWordBox" />
      </div>
      <div class="bar-sep"></div>
      <div class="icon-btn" title="列数设置" @click="emit('toggleColumns')">
        <SvgIcon name="columns" />
      </div>
      <div class="bar-sep"></div>
      <div class="icon-btn" title="时间戳" @click="emit('toggleTimestamp')" :class="{ active: timestampVisible }">
        <SvgIcon name="timestamp" />
      </div>
      <div class="bar-sep"></div>
      <div class="icon-btn" title="批量导入单词" @click="emit('batchImport')">
        <SvgIcon name="importWords" />
      </div>
      <div class="icon-btn" title="导出 PDF" @click="emit('exportPdf')">
        <SvgIcon name="exportPdf" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  position: fixed;
  right: calc(50vw - 105mm - 48px);
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
}

.btn-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.icon-btn {
  width: 38px; height: 38px;
  display: flex; align-items: center; justify-content: center;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
  color: #606266;
}
.icon-btn:hover { background: #ecf5ff; color: #409eff; transform: scale(1.08); }
.icon-btn:active { transform: scale(0.95); }
.icon-btn.active { background: #409eff; color: #fff; }
.icon-btn svg { width: 20px; height: 20px; display: block; }

.bar-sep {
  width: 24px; height: 1px;
  background: #d0d0d0;
  flex-shrink: 0;
}

@media print {
  .sidebar { display: none; }
}
</style>
