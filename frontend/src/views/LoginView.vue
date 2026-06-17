<template>
  <div class="login-page">
    <!-- 动态背景：音符粒子 -->
    <canvas ref="canvasRef" class="particles-bg"></canvas>

    <!-- 浮动音符 -->
    <div class="floating-notes">
      <span v-for="n in 15" :key="n" class="note"
        :style="{
          left: randomLeft(n),
          animationDelay: randomDelay(n) + 's',
          animationDuration: randomDuration(n) + 's',
          fontSize: randomSize(n) + 'px',
          opacity: 0.25 + (n % 5) * 0.08
        }">{{ notes[n % notes.length] }}</span>
    </div>

    <!-- 主内容 -->
    <div class="login-content">
      <!-- 欢迎标题 -->
      <div class="welcome-section">
        <h1 class="welcome-title">
          <span class="title-char" v-for="(ch, i) in '音乐推荐系统'" :key="i"
            :style="{ animationDelay: i * 0.1 + 's' }">{{ ch }}</span>
        </h1>
        <p class="welcome-subtitle">发现你的专属音乐世界</p>
        <div class="wave-divider">
          <span v-for="i in 5" :key="i" class="wave-bar" :style="{ animationDelay: i * 0.12 + 's' }"></span>
        </div>
      </div>

      <!-- 登录卡片 -->
      <div class="login-card">
        <div class="card-glow"></div>
        <div class="card-content">
          <div class="card-icon">🎵</div>
          <h2>欢迎回来</h2>
          <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                prefix-icon="User"
                size="large"
                class="custom-input"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                show-password
                size="large"
                class="custom-input"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                @click="handleLogin"
                :loading="loading"
                size="large"
                class="login-btn"
              >
                <span v-if="!loading">登 录</span>
                <span v-else>登录中...</span>
              </el-button>
            </el-form-item>
          </el-form>
          <p class="tip">
            还没有账号？
            <router-link to="/register" class="register-link">
              <span>立即注册</span>
              <span class="link-arrow">→</span>
            </router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref(null)
const canvasRef = ref(null)

const notes = ['♪', '♫', '♬', '♩', '🎵', '🎶', '🎼', '🎹']

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function randomLeft(n) {
  const seeds = [5,12,20,28,35,42,50,58,65,72,78,85,92,15,48]
  return (seeds[n-1] || (n * 7) % 90) + '%'
}
function randomDelay(n) { return ((n * 1.7) % 8).toFixed(1) }
function randomDuration(n) { return (8 + (n % 5) * 3).toFixed(1) }
function randomSize(n) { return 18 + (n % 6) * 4 }

// Canvas 粒子动画
let animId = null
function initCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let w, h

  function resize() {
    w = canvas.width = window.innerWidth
    h = canvas.height = window.innerHeight
  }
  resize()
  window.addEventListener('resize', resize)

  const particles = []
  const count = 80
  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      r: Math.random() * 2.5 + 0.5,
      vx: (Math.random() - 0.5) * 0.6,
      vy: (Math.random() - 0.5) * 0.6,
      hue: Math.random() * 60 + 240
    })
  }

  function draw() {
    ctx.clearRect(0, 0, w, h)

    // 连线
    for (let i = 0; i < particles.length; i++) {
      const p = particles[i]
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0) p.x = w
      if (p.x > w) p.x = 0
      if (p.y < 0) p.y = h
      if (p.y > h) p.y = 0

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `hsla(${p.hue}, 80%, 65%, ${0.5 + Math.random() * 0.3})`
      ctx.fill()

      for (let j = i + 1; j < particles.length; j++) {
        const p2 = particles[j]
        const dist = Math.hypot(p.x - p2.x, p.y - p2.y)
        if (dist < 120) {
          ctx.beginPath()
          ctx.moveTo(p.x, p.y)
          ctx.lineTo(p2.x, p2.y)
          ctx.strokeStyle = `hsla(${p.hue}, 70%, 60%, ${0.08 * (1 - dist / 120)})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }
    animId = requestAnimationFrame(draw)
  }
  draw()
}

onMounted(initCanvas)
onUnmounted(() => { if (animId) cancelAnimationFrame(animId) })

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await request.post('/auth/login', form)
    userStore.setLogin(res.data.token, res.data.user)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #0f0c29, #302b63, #24243e);
  overflow: hidden;
}

/* ===== Canvas 粒子背景 ===== */
.particles-bg {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  z-index: 1;
}

/* ===== 浮动音符 ===== */
.floating-notes {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  z-index: 2;
  pointer-events: none;
}
.note {
  position: absolute;
  bottom: -40px;
  color: rgba(255,255,255,0.3);
  animation: float-up linear infinite;
  user-select: none;
}
@keyframes float-up {
  0%   { transform: translateY(0) rotate(0deg); opacity: 0; }
  10%  { opacity: 0.6; }
  90%  { opacity: 0.1; }
  100% { transform: translateY(-105vh) rotate(360deg); opacity: 0; }
}

/* ===== 主内容区 ===== */
.login-content {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 60px;
  max-width: 1000px;
  width: 90%;
}

/* ===== 欢迎标题 ===== */
.welcome-section {
  flex: 1;
  text-align: left;
}
.welcome-title {
  font-size: 52px;
  font-weight: 900;
  margin: 0 0 16px;
  letter-spacing: 4px;
}
.title-char {
  display: inline-block;
  background: linear-gradient(135deg, #667eea, #e250c0, #f5576c);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: titleBounce 2s ease-in-out infinite;
}
@keyframes titleBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
.welcome-subtitle {
  font-size: 18px;
  color: rgba(255,255,255,0.6);
  margin: 0 0 28px;
  letter-spacing: 2px;
}

/* 音波分割线 */
.wave-divider {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 40px;
}
.wave-bar {
  width: 4px;
  border-radius: 2px;
  background: linear-gradient(to top, #667eea, #e250c0);
  animation: waveAnim 1.2s ease-in-out infinite;
}
@keyframes waveAnim {
  0%, 100% { height: 8px; }
  50% { height: 36px; }
}

/* ===== 登录卡片 ===== */
.login-card {
  width: 400px;
  position: relative;
}
.card-glow {
  position: absolute;
  inset: -3px;
  border-radius: 20px;
  background: linear-gradient(135deg, #667eea, #e250c0, #f5576c, #667eea);
  background-size: 300% 300%;
  animation: glowRotate 4s ease infinite;
  filter: blur(12px);
  opacity: 0.7;
  z-index: -1;
}
@keyframes glowRotate {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}
.card-content {
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  border-radius: 18px;
  padding: 44px 36px;
  position: relative;
  z-index: 1;
}
.card-icon {
  text-align: center;
  font-size: 48px;
  margin-bottom: 8px;
  animation: iconPulse 2s ease-in-out infinite;
}
@keyframes iconPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}
.card-content h2 {
  text-align: center;
  margin: 0 0 28px;
  color: #1a1a2e;
  font-size: 22px;
  font-weight: 700;
}

/* 输入框 */
:deep(.custom-input .el-input__wrapper) {
  border-radius: 12px;
  background: #f5f5fa;
  border: 2px solid transparent;
  transition: all 0.3s;
}
:deep(.custom-input .el-input__wrapper:hover) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102,126,234,0.1);
}
:deep(.custom-input .el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102,126,234,0.15);
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  transition: all 0.3s;
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102,126,234,0.4);
}
.login-btn:active {
  transform: translateY(0);
}

.tip {
  text-align: center;
  color: #909399;
  font-size: 14px;
  margin-top: 16px;
}
.register-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s;
}
.register-link:hover {
  color: #e250c0;
}
.register-link:hover .link-arrow {
  transform: translateX(4px);
}
.link-arrow {
  display: inline-block;
  margin-left: 4px;
  transition: transform 0.3s;
}

/* 响应式 */
@media (max-width: 768px) {
  .login-content {
    flex-direction: column;
    gap: 30px;
  }
  .welcome-title { font-size: 32px; text-align: center; }
  .welcome-subtitle { text-align: center; }
  .wave-divider { justify-content: center; }
  .login-card { width: 340px; }
}
</style>
