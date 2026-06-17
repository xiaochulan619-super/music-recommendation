<template>
  <div class="recommend-page">
    <div class="page-hero">
      <h2 class="page-title">⭐ 智能推荐</h2>
      <p class="page-desc">AI 分析你的喜好，为你精准推荐</p>
    </div>

    <!-- 训练状态卡片 -->
    <div class="train-card">
      <div class="train-status">
        <div class="status-indicator" :class="trainStatus?.status || 'idle'"></div>
        <div class="status-info">
          <span class="status-label">训练状态</span>
          <span class="status-value">
            <el-tag v-if="trainStatus" :type="statusType" effect="dark" size="small">{{ statusText }}</el-tag>
            <span v-else class="no-status">暂无训练记录</span>
          </span>
        </div>
      </div>
      <el-button
        type="primary"
        @click="triggerTrain"
        :loading="training"
        class="train-btn"
      >
        <span v-if="!training">🔄 开始训练模型</span>
        <span v-else>训练中...</span>
      </el-button>
      <p v-if="trainStatus?.message" class="train-msg">{{ trainStatus.message?.substring(0, 200) }}</p>
    </div>

    <!-- 推荐结果 -->
    <div class="recs-section" v-if="loadingRecs">
      <h3 class="section-heading">为你推荐</h3>
      <div class="recs-grid">
        <div v-for="i in 8" :key="'sk'+i" class="rec-card sk-rec">
          <div class="rec-cover-wrap sk-cover"></div>
          <div class="rec-info">
            <div class="sk-line w-70"></div>
            <div class="sk-line w-50"></div>
          </div>
        </div>
      </div>
    </div>
    <div class="recs-section" v-else-if="recs.length">
      <h3 class="section-heading">为你推荐</h3>
      <div class="recs-grid">
        <div
          v-for="r in recs"
          :key="r.song_id"
          class="rec-card"
          @click="$router.push(`/song/${r.song_id}`)"
        >
          <div class="rec-cover-wrap">
            <img :src="r.cover_url || genCover(r.title)" alt="" class="rec-cover" />
            <div class="rec-score-badge">{{ (r.score * 100).toFixed(0) }}%</div>
          </div>
          <div class="rec-info">
            <div class="rec-title">{{ r.title }}</div>
            <div class="rec-artist">{{ r.artist_name || '未知' }}</div>
            <el-tag size="small" :type="r.source==='fusion'?'success':r.source==='ncf'?'primary':'warning'" effect="dark">
              {{ r.source === 'fusion' ? '融合推荐' : r.source === 'ncf' ? 'NCF推荐' : '热门推荐' }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <div class="empty" v-if="!loadingRecs && !recs.length && !errorMsg">
      <span class="empty-icon">🤖</span>
      <p>还没有推荐结果</p>
      <p class="empty-hint">点击上方按钮开始 AI 训练，获取个性化推荐</p>
    </div>
    <div class="empty error" v-if="errorMsg">
      <span class="empty-icon">⚠️</span><p>{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const trainStatus = ref(null)
const training = ref(false)
const recs = ref([])
const loadingRecs = ref(false)
const errorMsg = ref('')
let pollTimer = null

function genCover(title) {
  const colors = ['667eea','e250c0','f5576c','43e97b','764ba2']
  const h = (title||'?').charCodeAt(0) || 0
  return `https://via.placeholder.com/200/${colors[h%colors.length]}/fff?text=${encodeURIComponent((title||'?').slice(0,4))}`
}

const statusText = computed(() => {
  const map = { pending: '等待中', running: '训练中...', completed: '已完成', failed: '失败' }
  return map[trainStatus.value?.status] || '未知'
})
const statusType = computed(() => {
  const map = { pending: 'info', running: 'warning', completed: 'success', failed: 'danger' }
  return map[trainStatus.value?.status] || 'info'
})

async function triggerTrain() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  training.value = true
  try {
    await request.post('/train/trigger')
    ElMessage.success('训练任务已启动')
    pollStatus()
  } catch (e) {} finally { training.value = false }
}

async function pollStatus() {
  try {
    const res = await request.get('/train/status')
    trainStatus.value = res.data
    if (['running', 'pending'].includes(trainStatus.value?.status)) {
      pollTimer = setTimeout(pollStatus, 3000)
    } else if (trainStatus.value?.status === 'completed') {
      loadRecs()
    }
  } catch (e) {}
}

async function loadRecs() {
  loadingRecs.value = true; errorMsg.value = ''
  try {
    const res = await request.get('/recommendations', { params: { limit: 20 } })
    recs.value = res.data || []
  } catch (e) { errorMsg.value = '推荐数据加载失败' } finally { loadingRecs.value = false }
}

onMounted(async () => {
  try { trainStatus.value = (await request.get('/train/status')).data } catch (e) {}
  if (userStore.isLoggedIn) loadRecs()
})
</script>

<style scoped>
.recommend-page { max-width: 1400px; margin: 0 auto; padding: 0 24px 40px; }
.page-hero { margin-bottom: 24px; }
.page-title { font-size: 28px; font-weight: 800; color: var(--text-primary); margin: 0; }
.page-desc { color: var(--text-muted); margin: 6px 0 0; font-size: 14px; }

.train-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px; padding: 24px;
  margin-bottom: 36px;
  display: flex; align-items: center; flex-wrap: wrap; gap: 20px;
}
.train-status { display: flex; align-items: center; gap: 12px; flex: 1; }
.status-indicator {
  width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0;
  background: var(--border);
}
.status-indicator.running { background: #e6a23c; animation: pulse 1.5s infinite; }
.status-indicator.completed { background: #67c23a; }
.status-indicator.failed { background: #f56c6c; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
.status-label { font-size: 12px; color: var(--text-muted); display: block; }
.status-value { font-size: 14px; color: var(--text-primary); }
.no-status { color: var(--text-muted); font-size: 13px; }
.train-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none; border-radius: 12px; font-weight: 600; transition: all 0.3s;
}
.train-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba(102,126,234,0.4); }
.train-msg { width: 100%; font-size: 12px; color: var(--text-muted); margin: 0; }

.section-heading { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0 0 18px; }
.recs-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
@media (max-width: 1100px) { .recs-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) { .recs-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) { .recs-grid { grid-template-columns: 1fr; } }

.rec-card {
  cursor: pointer; border-radius: 14px; overflow: hidden;
  background: var(--bg-card);
  border: 1px solid var(--border);
  transition: all 0.35s;
}
.rec-card:hover {
  transform: translateY(-6px);
  border-color: rgba(102,126,234,0.3);
  box-shadow: var(--card-shadow);
}
.rec-cover-wrap { position: relative; aspect-ratio: 1; overflow: hidden; background: var(--bg-secondary); }
.rec-cover {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.5s;
}
.rec-card:hover .rec-cover { transform: scale(1.1) rotate(2deg); }
.rec-score-badge {
  position: absolute; top: 8px; right: 8px;
  padding: 3px 8px; border-radius: 12px;
  font-size: 11px; font-weight: 700;
  background: rgba(0,0,0,0.6); color: #67c23a;
  backdrop-filter: blur(4px);
}
.rec-info { padding: 12px 14px; }
.rec-title { font-size: 14px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-artist { font-size: 12px; color: var(--text-muted); margin: 4px 0 8px; }

.empty { text-align: center; padding: 60px 0; }
.empty-icon { font-size: 48px; }
.empty p { color: var(--text-muted); margin: 8px 0; }
.empty.error p { color: #f56c6c; }
.empty-hint { font-size: 13px; color: var(--text-muted); }

/* Skeleton */
.sk-rec { pointer-events: none; }
.sk-cover {
  aspect-ratio: 1;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.sk-line {
  height: 12px; border-radius: 6px; margin: 6px 0;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.w-70 { width: 70%; } .w-50 { width: 50%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
