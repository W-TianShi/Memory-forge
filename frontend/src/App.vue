<template>
  <div id="app-root">
    <nav class="app-nav">
      <router-link to="/">单词记忆</router-link>
      <router-link to="/notes">笔记工具</router-link>
      <router-link to="/math-ai">数学AI</router-link>
      <router-link to="/mistakes">错题整理</router-link>
      <div class="nav-right" id="nav-right">
        <span class="nav-user" v-if="username" :title="username">{{ username }}</span>
        <button class="nav-login-btn" v-if="!username" @click="openAuth">登录</button>
        <button class="nav-logout-btn" v-if="username" @click="doLogout">退出</button>
      </div>
    </nav>

    <div class="auth-overlay" v-if="authVisible" @click.self="authVisible = false">
      <div class="auth-box">
        <div class="auth-logo">MF</div>
        <div class="auth-title">{{ authMode === 'login' ? '欢迎回来' : '创建账号' }}</div>
        <div class="auth-subtitle">{{ authMode === 'login' ? '登录你的 MemoryForge 账号' : '注册后可使用全部功能' }}</div>

        <div class="auth-field">
          <svg class="auth-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
          <input v-model="form.email" placeholder="请输入邮箱" type="email" />
        </div>

        <div class="auth-field" v-if="authMode === 'register'">
          <svg class="auth-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          <input v-model="form.code" placeholder="请输入6位验证码" maxlength="6" />
          <button class="auth-send" @click="doSendCode" :disabled="sending">{{ sendText }}</button>
        </div>

        <div class="auth-field">
          <svg class="auth-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          <input v-model="form.password" placeholder="请输入密码（6-20位）" type="password" />
        </div>

        <div class="auth-err" v-if="errMsg">{{ errMsg }}</div>

        <button class="auth-submit" @click="doSubmit" :disabled="loading">
          {{ loading ? '处理中...' : authMode === 'login' ? '登  录' : '注  册' }}
        </button>

        <div class="auth-switch">
          <span v-if="authMode === 'login'">还没有账号？<a href="#" @click.prevent="switchMode('register')">立即注册</a></span>
          <span v-else>已有账号？<a href="#" @click.prevent="switchMode('login')">立即登录</a></span>
        </div>
      </div>
    </div>

    <router-view v-slot="{ Component }">
      <keep-alive :include="['WordMemory', 'NoteExport', 'MathAI', 'MistakesBook']">
        <component :is="Component" />
      </keep-alive>
    </router-view>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { sendCode, register, login, setAuth, clearAuth, getUsername, isLoggedIn } from './api/auth.js'

const authVisible = ref(false)
const authMode = ref('login')
const username = ref(getUsername())
const errMsg = ref('')
const loading = ref(false)
const sending = ref(false)
const sendText = ref('获取验证码')

const form = reactive({ email: '', password: '', code: '' })

function openAuth() {
  authMode.value = 'login'
  form.email = ''
  form.password = ''
  form.code = ''
  errMsg.value = ''
  authVisible.value = true
}

function switchMode(mode) {
  authMode.value = mode
  form.email = ''
  form.password = ''
  form.code = ''
  errMsg.value = ''
  sendText.value = '获取验证码'
}

async function doSendCode() {
  if (!form.email.includes('@')) { errMsg.value = '请输入正确的邮箱'; return }
  errMsg.value = ''
  sending.value = true
  try {
    const res = await sendCode(form.email)
    if (res.success) {
      let sec = 60
      sendText.value = `${sec}s后重发`
      const timer = setInterval(() => {
        sec--
        sendText.value = `${sec}s后重发`
        if (sec <= 0) { clearInterval(timer); sendText.value = '获取验证码'; sending.value = false }
      }, 1000)
    } else {
      errMsg.value = res.message || '发送失败'
      sending.value = false
    }
  } catch { errMsg.value = '网络错误'; sending.value = false }
}

async function doSubmit() {
  errMsg.value = ''
  if (!form.email.includes('@')) { errMsg.value = '请输入正确的邮箱'; return }
  if (form.password.length < 6) { errMsg.value = '密码至少6位'; return }
  loading.value = true
  try {
    let res
    if (authMode.value === 'login') {
      res = await login(form.email, form.password)
    } else {
      if (!form.code) { errMsg.value = '请输入验证码'; loading.value = false; return }
      res = await register(form.email, form.password, form.code)
    }
    if (res.success && res.token) {
      setAuth(res.token, res.username || form.email.split('@')[0])
      username.value = getUsername()
      authVisible.value = false
    } else {
      errMsg.value = res.message || '操作失败'
    }
  } catch { errMsg.value = '网络错误' }
  loading.value = false
}

function doLogout() {
  clearAuth()
  username.value = ''
}
</script>

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
.app-nav a:hover { background: #ecf5ff; color: #409eff; }
.app-nav a.router-link-active { background: #409eff; color: #fff; }
.app-nav .nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  position: relative;
  gap: 10px;
}
.nav-user { font-size: 13px; color: #409eff; font-weight: 500; }
.nav-login-btn {
  padding: 4px 14px;
  font-size: 13px;
  border: 1px solid #409eff;
  border-radius: 4px;
  background: #fff;
  color: #409eff;
  cursor: pointer;
}
.nav-login-btn:hover { background: #ecf5ff; }
.nav-logout-btn {
  padding: 2px 8px;
  font-size: 11px;
  border: 1px solid #ddd;
  border-radius: 3px;
  background: #fff;
  color: #999;
  cursor: pointer;
}
.nav-logout-btn:hover { color: #f56c6c; border-color: #f56c6c; }

.auth-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.45);
  backdrop-filter: blur(4px);
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
}
.auth-box {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px 32px;
  box-shadow: 0 20px 60px rgba(0,0,0,.2), 0 0 0 1px rgba(0,0,0,.05);
  animation: authIn .3s ease;
}
@keyframes authIn {
  from { opacity: 0; transform: translateY(-20px) scale(.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.auth-logo {
  width: 52px; height: 52px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
}
.auth-title { font-size: 22px; font-weight: 700; color: #1a1a1a; margin-bottom: 6px; text-align: center; }
.auth-subtitle { font-size: 13px; color: #999; margin-bottom: 28px; text-align: center; }

.auth-field {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f7f8fa;
  border: 1px solid transparent;
  border-radius: 10px;
  padding: 0 14px;
  margin-bottom: 14px;
  transition: all .2s;
}
.auth-field:focus-within { background: #fff; border-color: #409eff; box-shadow: 0 0 0 3px rgba(64,158,255,.1); }
.auth-field input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 13px 0;
  font-size: 15px;
  outline: none;
  color: #333;
}
.auth-icon { flex-shrink: 0; }

.auth-send {
  padding: 6px 14px;
  font-size: 12px;
  white-space: nowrap;
  border: 1px solid #409eff;
  border-radius: 6px;
  background: #fff;
  color: #409eff;
  cursor: pointer;
  flex-shrink: 0;
}
.auth-send:hover { background: #ecf5ff; }
.auth-send:disabled { color: #ccc; border-color: #e0e0e0; cursor: not-allowed; }

.auth-err {
  color: #f56c6c;
  font-size: 13px;
  background: #fef0f0;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 12px;
}
.auth-submit {
  width: 100%;
  padding: 13px 0;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  cursor: pointer;
  margin-top: 6px;
  transition: all .2s;
  letter-spacing: 4px;
}
.auth-submit:hover { background: linear-gradient(135deg, #66b1ff, #409eff); transform: translateY(-1px); box-shadow: 0 4px 14px rgba(64,158,255,.4); }
.auth-submit:disabled { background: #c8d9e8; transform: none; box-shadow: none; cursor: not-allowed; }
.auth-switch { text-align: center; margin-top: 20px; font-size: 13px; color: #999; }
.auth-switch a { color: #409eff; text-decoration: none; font-weight: 500; margin-left: 2px; }
.auth-switch a:hover { text-decoration: underline; }
</style>
