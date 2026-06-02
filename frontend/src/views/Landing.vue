<script setup>
import { setAuth, getUsername } from '../api/auth.js'
import { ref } from 'vue'

const emit = defineEmits(['enter'])
const username = ref(getUsername())
</script>

<template>
  <div class="landing">
    <div class="hero">
      <div class="hero-badge">MemoryForge</div>
      <h1 class="hero-title">学习笔记 · 打印即用</h1>
      <p class="hero-desc">
        在浏览器里写笔记、排单词表、选模板、导出 PDF<br>打印出来就是一本精致的笔记本
      </p>
      <div class="hero-btns">
        <button class="btn-primary" @click="$emit('enter')">开始使用</button>
      </div>
    </div>

    <div class="features">
      <div class="feat-card">
        <div class="feat-visual note-preview">
          <div class="mini-editor">
            <div class="grid-lines">
              <div class="mini-line" v-for="i in 6" :key="i"></div>
            </div>
            <div class="mini-text">
              <div class="mini-h">图推要义</div>
              <div class="mini-p">特殊值法：通过比较关键点快速判断</div>
              <div class="mini-p">主次元素分离法：将隐含二级元素分析</div>
              <div class="mini-blank">考试重点挖空记忆 ████</div>
            </div>
          </div>
        </div>
        <div class="feat-info">
          <div class="feat-icon">📝</div>
          <h3>笔记纸</h3>
          <p>编写笔记 · 挖空背诵 · 4 种模板背景 · 导出 PDF 打印</p>
        </div>
      </div>

      <div class="feat-card">
        <div class="feat-visual word-preview">
          <div class="mini-word-table">
            <div class="mini-row" v-for="i in 4" :key="i">
              <span class="mini-idx">{{ String(i).padStart(2,'0') }}</span>
              <span class="mini-wd">vocabulary</span>
              <span class="mini-ph">/vəˈkæbjəleri/</span>
              <span class="mini-mn">词汇</span>
            </div>
          </div>
        </div>
        <div class="feat-info">
          <div class="feat-icon">📖</div>
          <h3>背词纸</h3>
          <p>编排单词表 · 2/3 列布局 · 一键查词 · 导出打印</p>
        </div>
      </div>

      <div class="feat-card">
        <div class="feat-visual ai-preview">
          <div class="mini-chat">
            <div class="mini-msg user">这道几何题怎么解？</div>
            <div class="mini-msg ai">利用相似三角形...</div>
            <div class="mini-msg user">能再详细一点吗？</div>
            <div class="mini-msg ai">好的，我们先设未知数...</div>
          </div>
        </div>
        <div class="feat-info">
          <div class="feat-icon">🤖</div>
          <h3>数学 AI</h3>
          <p>DeepSeek 驱动 · 数学题深度解析 · 流式对话</p>
        </div>
      </div>

      <div class="feat-card">
        <div class="feat-visual mistake-preview">
          <div class="mini-mistakes">
            <div class="mini-m-item" v-for="i in 3" :key="i">
              <div class="mini-m-tag">代数</div>
              <div class="mini-m-text">一元二次方程求根...</div>
            </div>
          </div>
        </div>
        <div class="feat-info">
          <div class="feat-icon">❌</div>
          <h3>错题整理</h3>
          <p>拍照收集错题 · 分类管理 · 反复练习</p>
        </div>
      </div>
    </div>

    <div class="cta">
      <h2>打印效果预览</h2>
      <div class="print-previews">
        <div class="print-card">
          <div class="print-sheet note-sheet">
            <div class="sheet-lines" v-for="i in 8" :key="i"></div>
            <div class="sheet-text">
              <span>笔记纸 · A4 网格模板</span>
            </div>
          </div>
          <p>笔记纸导出效果</p>
        </div>
        <div class="print-card">
          <div class="print-sheet word-sheet">
            <div class="sheet-cols">
              <div class="sheet-col" v-for="c in 2" :key="c">
                <div class="sheet-row" v-for="r in 5" :key="r">
                  <span class="si">{{ String((c-1)*5+r).padStart(2,'0') }}</span>
                  <span class="sw">example</span>
                  <span class="sm">例子</span>
                </div>
              </div>
            </div>
          </div>
          <p>背词纸导出效果</p>
        </div>
      </div>
    </div>

    <div class="footer">
      <p>免费使用 · 数据存储在浏览器中 · 可导出备份</p>
    </div>
  </div>
</template>

<style scoped>
.landing {
  min-height: 100vh;
  background: linear-gradient(180deg, #f0f4f8 0%, #fff 30%);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.hero {
  text-align: center;
  padding: 64px 20px 40px;
}
.hero-badge {
  display: inline-block;
  padding: 4px 16px;
  background: #409eff;
  color: #fff;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  margin-bottom: 20px;
}
.hero-title {
  font-size: 36px;
  font-weight: 800;
  color: #1a1a2e;
  margin-bottom: 12px;
}
.hero-desc {
  font-size: 16px;
  color: #666;
  line-height: 1.8;
  margin-bottom: 28px;
}
.hero-btns { display: flex; gap: 12px; justify-content: center; }
.btn-primary {
  padding: 12px 36px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  cursor: pointer;
  transition: all .2s;
  box-shadow: 0 4px 16px rgba(64,158,255,.3);
}
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(64,158,255,.4); }

.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
  padding: 0 20px 40px;
  max-width: 1100px;
  margin: 0 auto;
}
.feat-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
  transition: all .2s;
}
.feat-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,.1); }
.feat-visual { height: 140px; overflow: hidden; }
.feat-info { padding: 16px 18px 20px; }
.feat-info h3 { font-size: 17px; font-weight: 700; color: #1a1a2e; margin-bottom: 6px; }
.feat-info p { font-size: 13px; color: #888; line-height: 1.6; }
.feat-icon { font-size: 28px; margin-bottom: 8px; }

/* visual: note */
.note-preview { background: #fdfdfd; padding: 16px 18px; }
.mini-editor { position: relative; }
.grid-lines { position: absolute; inset: 0; background: repeating-linear-gradient(#eee 0,#eee 1px,transparent 1px,transparent 12px); opacity:.6; }
.mini-text { position: relative; z-index:1; }
.mini-h { font-size: 16px; font-weight: 700; color: #1a1a2e; margin-bottom: 6px; }
.mini-p { font-size: 10px; color: #555; margin-bottom: 4px; line-height: 1.4; }
.mini-blank { font-size: 10px; color: #409eff; margin-top: 6px; border-bottom: 1px solid #409eff; display: inline-block; }

/* visual: word */
.word-preview { background: #fff; padding: 12px 14px; }
.mini-word-table { font-size: 9px; }
.mini-row { display: flex; gap: 6px; padding: 3px 4px; border: 1px solid #eee; border-bottom: none; align-items: center; }
.mini-row:last-child { border-bottom: 1px solid #eee; }
.mini-idx { color: #999; width: 16px; }
.mini-wd { font-weight: 600; color: #1a1a2e; width: 60px; }
.mini-ph { color: #aaa; width: 60px; font-size: 8px; }
.mini-mn { color: #555; }

/* visual: ai */
.ai-preview { background: #f8f9fb; padding: 12px 14px; }
.mini-chat { display: flex; flex-direction: column; gap: 6px; }
.mini-msg { font-size: 10px; padding: 5px 8px; border-radius: 8px; max-width: 85%; line-height: 1.4; }
.mini-msg.user { background: #409eff; color: #fff; align-self: flex-end; }
.mini-msg.ai { background: #fff; color: #333; border: 1px solid #eee; }

/* visual: mistakes */
.mistake-preview { background: #fff; padding: 12px 14px; }
.mini-mistakes { display: flex; flex-direction: column; gap: 6px; }
.mini-m-item { display: flex; gap: 8px; align-items: center; padding: 6px 8px; border: 1px solid #eee; border-radius: 6px; }
.mini-m-tag { font-size: 9px; padding: 1px 6px; background: #f56c6c; color: #fff; border-radius: 3px; }
.mini-m-text { font-size: 11px; color: #555; }

.cta { text-align: center; padding: 40px 20px; background: #f8f9fb; }
.cta h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin-bottom: 24px; }
.print-previews { display: flex; gap: 20px; justify-content: center; flex-wrap: wrap; max-width: 700px; margin: 0 auto; }
.print-card { text-align: center; }
.print-card p { margin-top: 10px; font-size: 12px; color: #999; }
.print-sheet { width: 180px; height: 240px; background: #fff; border: 1px solid #e0e0e0; border-radius: 4px; margin: 0 auto; box-shadow: 0 2px 10px rgba(0,0,0,.08); overflow: hidden; }
.note-sheet { padding: 12px; position: relative; }
.sheet-lines { height: 1px; background: #e8e8e8; margin-bottom: 12px; }
.sheet-text { font-size: 10px; color: #aaa; text-align: center; margin-top: 60px; }
.word-sheet { padding: 10px; }
.sheet-cols { display: flex; gap: 6px; }
.sheet-col { flex:1; display:flex; flex-direction:column; gap:4px; }
.sheet-row { display:flex; gap:3px; font-size:7px; align-items:center; padding:2px 3px; border:1px solid #eee; border-radius:2px; }
.si { color:#bbb; }
.sw { font-weight:600; color:#333; }
.sm { color:#888; }

.footer { text-align: center; padding: 30px 20px; color: #bbb; font-size: 12px; }
</style>
