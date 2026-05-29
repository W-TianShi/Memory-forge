<template>
  <div id="app-root">
    <nav class="app-nav">
      <router-link to="/">单词记忆</router-link>
      <router-link to="/notes">笔记工具</router-link>
      <router-link to="/math-ai">数学AI</router-link>
      <router-link to="/mistakes">错题整理</router-link>
      <div class="nav-right" id="nav-right"></div>
    </nav>
    <router-view v-slot="{ Component }">
      <keep-alive :include="['WordMemory', 'NoteExport', 'MathAI', 'MistakesBook']">
        <component :is="Component" />
      </keep-alive>
    </router-view>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
#app-root {
  min-height: 100vh;
}
.app-nav {
  display: flex;
  gap: 16px;
  padding: 10px 20px;
  background: #f5f7fa;
  border-bottom: 1px solid #e0e0e0;
  justify-content: center;
  position: sticky;
  top: 0;
  z-index: 100;
}
.app-nav a {
  text-decoration: none;
  color: #606266;
  font-size: 14px;
  padding: 6px 16px;
  border-radius: 4px;
  transition: all 0.2s;
}
.app-nav a:hover {
  background: #ecf5ff;
  color: #409eff;
}
.app-nav a.router-link-active {
  background: #409eff;
  color: #fff;
}
.app-nav .nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  position: relative;
}
</style>
