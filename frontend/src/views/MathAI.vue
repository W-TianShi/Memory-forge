<script>
export default { name: 'MathAI' }
</script>

<script setup>
import { ref } from 'vue'
import { useMathProblems } from '../composables/useMathProblems.js'
import MathProblemList from '../components/MathProblemList.vue'
import MathProblemEdit from '../components/MathProblemEdit.vue'
import ThinkingAI from '../components/ThinkingAI.vue'

const { selected, incrementAttempt } = useMathProblems()
const aiRef = ref(null)

function sendToAI(problem) {
  incrementAttempt(problem.id)
  if (aiRef.value) {
    aiRef.value.send('请分析这道数学题：\n' + problem.content)
  }
}
</script>

<template>
  <div class="math-ai-app">
    <div class="left-panel">
      <MathProblemList />
      <MathProblemEdit @sendToAI="sendToAI" />
    </div>
    <div class="divider"></div>
    <div class="right-panel">
      <ThinkingAI ref="aiRef" :selectedProblem="selected" />
    </div>
  </div>
</template>

<style scoped>
.math-ai-app {
  display: flex;
  height: calc(100vh - 42px);
  overflow: hidden;
  background: #f8f9fa;
}

.left-panel {
  width: 420px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  overflow: hidden;
}

.divider {
  width: 6px;
  background: transparent;
  flex-shrink: 0;
  cursor: col-resize;
  transition: background 0.15s;
}
.divider:hover { background: #409eff33; }

.right-panel {
  flex: 1;
  padding: 16px;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
