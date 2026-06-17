<template>
  <div class="main-app">
    <!-- ===== 音符绕动背景 ===== -->
    <canvas ref="notesCanvas" class="notes-bg"></canvas>

    <!-- ===== 顶部导航栏 ===== -->
    <header class="top-nav">
      <div class="nav-inner">
        <router-link to="/" class="nav-logo">
          <span class="logo-icon">🎵</span>
          <span class="logo-text">MusicFlow</span>
        </router-link>

        <nav class="nav-menu">
          <router-link v-for="item in navItems" :key="item.path" :to="item.path"
            class="nav-item" :class="{ active: isActive(item) }">
            <span class="nav-icon">{{ item.icon }}</span>
            <span class="nav-label">{{ item.label }}</span>
          </router-link>
        </nav>

        <div class="nav-right">
          <!-- 主题切换 -->
          <div class="theme-toggle" @click="themeStore.toggle" :title="themeStore.mode === 'dark' ? '切换白天模式' : '切换夜间模式'">
            <span v-if="themeStore.mode === 'dark'">☀️</span>
            <span v-else>🌙</span>
          </div>

          <template v-if="userStore.isLoggedIn">
            <el-dropdown trigger="click" placement="bottom-end">
              <div class="user-avatar">
                <span class="avatar-icon">👤</span>
                <span class="user-name">{{ userStore.userInfo?.nickname || '用户' }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button class="nav-login-btn" @click="$router.push('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- ===== 主内容区 ===== -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const notesCanvas = ref(null)

const navItems = [
  { path: '/', label: '首页', icon: '🏠' },
  { path: '/discover', label: '发现', icon: '🔍' },
  { path: '/artists', label: '歌手', icon: '🎤' },
  { path: '/charts', label: '榜单', icon: '🏆' },
  { path: '/recommend', label: '推荐', icon: '⭐' },
  { path: '/favorites', label: '收藏', icon: '❤️' },
  { path: '/history', label: '历史', icon: '🕐' },
]

function isActive(item) {
  if (item.path === '/') return route.path === '/'
  return route.path.startsWith(item.path)
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

// ===== 音符绕动 Canvas 动画 =====
let animId = null
function initNotesCanvas() {
  const canvas = notesCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let w, h

  function resize() {
    w = canvas.width = window.innerWidth
    h = canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  // 音符对象
  const notes = []
  const symbols = ['♪', '♫', '♬', '♩', '♭', '♯']
  for (let i = 0; i < 40; i++) {
    notes.push({
      x: Math.random() * w,
      y: Math.random() * h,
      size: 14 + Math.random() * 20,
      speed: 0.3 + Math.random() * 0.6,
      amplitude: 30 + Math.random() * 60,
      phase: Math.random() * Math.PI * 2,
      symbol: symbols[Math.floor(Math.random() * symbols.length)],
      opacity: 0.08 + Math.random() * 0.12,
    })
  }

  function draw() {
    ctx.clearRect(0, 0, w, h)
    const isDark = themeStore.mode === 'dark'
    const time = Date.now() * 0.001

    for (const n of notes) {
      n.phase += n.speed * 0.008
      const x = n.x + Math.sin(time * n.speed + n.phase) * n.amplitude * 0.5
      const y = n.y + Math.cos(time * n.speed * 0.7 + n.phase) * n.amplitude * 0.3

      if (x < -50) n.x = w + 50
      if (x > w + 50) n.x = -50
      if (y < -50) n.y = h + 50
      if (y > h + 50) n.y = -50

      ctx.save()
      ctx.font = `${n.size}px serif`
      ctx.fillStyle = isDark
        ? `rgba(180, 200, 255, ${n.opacity})`
        : `rgba(100, 80, 180, ${n.opacity * 0.7})`
      ctx.fillText(n.symbol, x, y)
      ctx.restore()
    }
    animId = requestAnimationFrame(draw)
  }
  draw()
}

onMounted(initNotesCanvas)
onUnmounted(() => { if (animId) cancelAnimationFrame(animId) })
</script>

<style>
/* ===== 全局 CSS 变量：日/夜双主题 ===== */
:root,
[data-theme="dark"] {
  --bg-primary: #0f0f18;
  --bg-secondary: #161622;
  --bg-card: rgba(255, 255, 255, 0.03);
  --bg-card-hover: rgba(255, 255, 255, 0.06);
  --bg-input: rgba(255, 255, 255, 0.05);
  --border: rgba(255, 255, 255, 0.06);
  --border-hover: rgba(255, 255, 255, 0.12);
  --text-primary: #e8e8e8;
  --text-secondary: rgba(255, 255, 255, 0.6);
  --text-muted: rgba(255, 255, 255, 0.35);
  --accent: #667eea;
  --accent-hover: #8899ff;
  --nav-bg: rgba(15, 15, 24, 0.85);
  --card-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  --hero-gradient: linear-gradient(135deg, #1a1a3e, #2d1b3e);
}

[data-theme="light"] {
  --bg-primary: #f0f2f8;
  --bg-secondary: #f8f9fc;
  --bg-card: rgba(0, 0, 0, 0.02);
  --bg-card-hover: rgba(0, 0, 0, 0.04);
  --bg-input: rgba(0, 0, 0, 0.04);
  --border: rgba(0, 0, 0, 0.08);
  --border-hover: rgba(0, 0, 0, 0.15);
  --text-primary: #1a1a2e;
  --text-secondary: rgba(0, 0, 0, 0.55);
  --text-muted: rgba(0, 0, 0, 0.35);
  --accent: #5568d4;
  --accent-hover: #4455bb;
  --nav-bg: rgba(248, 249, 252, 0.9);
  --card-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  --hero-gradient: linear-gradient(135deg, #e8ecf8, #f0e6f6);
}
</style>

<style scoped>
.main-app {
  min-height: 100vh;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  transition: background 0.4s, color 0.4s;
}

/* ===== 音符背景 Canvas ===== */
.notes-bg {
  position: fixed; top: 0; left: 0;
  width: 100%; height: 100%;
  z-index: 0; pointer-events: none;
}

/* ===== 顶部导航 ===== */
.top-nav {
  position: fixed; top: 0; left: 0; right: 0;
  z-index: 1000;
  background: var(--nav-bg);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border-bottom: 1px solid var(--border);
  transition: background 0.4s;
}
.nav-inner {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 16px;
  height: 56px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-logo {
  display: flex; align-items: center; gap: 8px;
  text-decoration: none; margin-right: 18px; flex-shrink: 0;
}
.logo-icon { font-size: 26px; animation: logoBounce 2s ease-in-out infinite; }
@keyframes logoBounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}
.logo-text {
  font-size: 19px; font-weight: 800;
  background: linear-gradient(135deg, var(--accent), #e250c0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-menu {
  display: flex; align-items: center; gap: 2px; flex: 1;
}
.nav-item {
  position: relative;
  display: flex; align-items: center; gap: 5px;
  padding: 7px 12px; border-radius: 8px;
  text-decoration: none;
  color: var(--text-secondary);
  font-size: 13px; font-weight: 500;
  transition: all 0.2s; white-space: nowrap;
}
.nav-item:hover { color: var(--text-primary); background: var(--bg-card-hover); }
.nav-item.active { color: var(--accent); background: rgba(102, 126, 234, 0.1); }
.nav-icon { font-size: 15px; }

.nav-right {
  display: flex; align-items: center; gap: 10px; flex-shrink: 0;
}

/* 主题切换 */
.theme-toggle {
  width: 34px; height: 34px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 10px; cursor: pointer;
  font-size: 18px; transition: all 0.25s;
  background: var(--bg-card);
}
.theme-toggle:hover { background: var(--bg-card-hover); transform: scale(1.1); }

.user-avatar {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 10px; border-radius: 10px;
  cursor: pointer; transition: all 0.2s;
  background: var(--bg-card);
}
.user-avatar:hover { background: var(--bg-card-hover); }
.avatar-icon { font-size: 18px; }
.user-name { font-size: 13px; color: var(--text-secondary); max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.nav-login-btn {
  background: linear-gradient(135deg, var(--accent), #764ba2);
  border: none; color: #fff; border-radius: 10px;
  padding: 7px 18px; font-weight: 600; transition: all 0.3s;
}
.nav-login-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(102,126,234,0.4); }

/* ===== 主内容 ===== */
.main-content {
  position: relative; z-index: 1;
  padding-top: 56px;
  min-height: calc(100vh - 56px);
}

/* 页面过渡 */
.page-fade-enter-active,
.page-fade-leave-active { transition: all 0.2s ease; }
.page-fade-enter-from,
.page-fade-leave-to { opacity: 0; transform: translateY(6px); }

@media (max-width: 768px) {
  .nav-menu { overflow-x: auto; gap: 1px; -webkit-overflow-scrolling: touch; }
  .nav-item { padding: 6px 8px; font-size: 12px; }
  .nav-label { display: none; }
  .logo-text { display: none; }
  .nav-inner { padding: 0 8px; }
}
</style>
