<script setup>
import { useMathProblems } from '../composables/useMathProblems.js'

const {
  filtered, selectedId, filterCategory, filterStatus, filterThinking,
  categories, thinkingLabels, statusLabels, statusColors,
  add, selectProblem, remove
} = useMathProblems()

function getStatusCircle(status) {
  const color = statusColors[status]
  return `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${color};margin-right:4px;"></span>`
}
</script>

<template>
  <div class="problem-list-side">
    <div class="list-toolbar">
      <button class="add-problem-btn" @click="add">+ 添加题目</button>
    </div>

    <div class="filters">
      <select v-model="filterCategory">
        <option value="全部">全部分类</option>
        <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
      </select>
      <select v-model="filterStatus">
        <option value="全部">全部状态</option>
        <option value="not_started">未开始</option>
        <option value="learning">学习中</option>
        <option value="mastered">已掌握</option>
      </select>
      <select v-model="filterThinking">
        <option :value="0">全部思维</option>
        <option v-for="(t, i) in thinkingLabels" :key="i" :value="i + 1">思维{{ i + 1 }}：{{ t }}</option>
      </select>
    </div>

    <div class="problem-items">
      <div v-if="filtered.length === 0" class="empty-tip">暂无题目，点击上方添加</div>
      <div
        v-for="p in filtered" :key="p.id"
        class="problem-item"
        :class="{ active: p.id === selectedId }"
        @click="selectProblem(p.id)"
      >
        <div class="item-header">
          <span class="item-category">{{ p.category }}</span>
          <span class="item-status" :style="{ color: statusColors[p.status] }">{{ statusLabels[p.status] }}</span>
        </div>
        <div class="item-title">{{ p.title || '未命名题目' }}</div>
        <div class="item-thinking">
          <span v-for="tId in p.thinkingIds" :key="tId" class="thinking-tag">思维{{ tId }}</span>
        </div>
        <button class="item-del" @click.stop="remove(p.id)">×</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.problem-list-side {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.list-toolbar {
  padding: 0 0 8px;
}

.add-problem-btn {
  width: 100%;
  padding: 8px;
  border: 1px solid #409eff;
  border-radius: 6px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}
.add-problem-btn:hover { background: #409eff; color: #fff; }

.filters {
  display: flex;
  gap: 6px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 8px;
}
.filters select {
  flex: 1;
  min-width: 0;
  padding: 4px 2px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
  color: #555;
  background: #fff;
}

.problem-items {
  flex: 1;
  overflow-y: auto;
}

.empty-tip {
  text-align: center;
  color: #bbb;
  font-size: 13px;
  padding: 40px 0;
}

.problem-item {
  position: relative;
  padding: 8px 10px;
  border: 1px solid #eee;
  border-radius: 6px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
.problem-item:hover { background: #f5f7fa; }
.problem-item.active { background: #e8f4ff; border-color: #409eff; }

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.item-category {
  font-size: 11px;
  background: #f0f0f0;
  color: #888;
  padding: 1px 6px;
  border-radius: 3px;
}
.item-status {
  font-size: 11px;
  font-weight: 500;
}
.item-title {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-thinking {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  flex-wrap: wrap;
}
.thinking-tag {
  font-size: 10px;
  background: #fff3e0;
  color: #e65100;
  padding: 1px 5px;
  border-radius: 3px;
}
.item-del {
  position: absolute;
  top: 4px;
  right: 6px;
  background: none;
  border: none;
  color: #ccc;
  font-size: 16px;
  cursor: pointer;
  line-height: 1;
}
.item-del:hover { color: #f56c6c; }
</style>
