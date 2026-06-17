<template>
  <div class="recommend-page">
    <div class="page-hero">
      <h2 class="page-title">🤖 AI 智能推荐</h2>
      <p class="page-desc">DeepSeek 大模型分析你的听歌习惯，为你精准推荐</p>
    </div>

    <!-- 操作区 -->
    <div class="action-card">
      <div class="action-info">
        <span class="action-label">推荐引擎</span>
        <el-tag :type="recSource === 'deepseek' ? 'success' : 'warning'" effect="dark" size="small">
          {{ recSource === 'deepseek' ? 'DeepSeek AI' : '智能匹配' }}
        </el-tag>
        <span v-if="recSource === 'deepseek'" class="powered-by">Powered by DeepSeek</span>
      </div>
      <el-button
        type="primary"
        @click="fetchRecs"
        :loading="loading"
        class="refresh-btn"
      >
        <span v-if="!loading">🔄 获取推荐</span>
        <span v-else>分析中...</span>
      </el-button>
    </div>

    <!-- 降级提示 -->
    <el-alert
      v-if="isFallback"
      title="DeepSeek AI 暂时不可用，已为你切换到智能标签匹配推荐"
      type="warning"
      :closable="false"
      show-icon
      class="fallback-alert"
    />

    <!-- 加载骨架 -->
    <div class="recs-section" v-if="loading">
      <h3 class="section-heading">为你推荐</h3>
      <div class="recs-grid">
        <div v-for="i in 12" :key="'sk'+i" class="rec-card sk-rec">
          <div class="rec-cover-wrap sk-cover"></div>
          <div class="rec-info">
            <div class="sk-line w-70"></div>
            <div class="sk-line w-50"></div>
            <div class="sk-line w-60"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 推荐结果 -->
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
          </div>
          <div class="rec-info">
            <div class="rec-title">{{ r.title }}</div>
            <div class="rec-artist">
              {{ r.artist_original_name || r.artist_name || '未知' }}
            </div>
            <div class="rec-reason" v-if="r.reason">
              💡 {{ r.reason }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty" v-if="!loading && !recs.length && !errorMsg">
      <span class="empty-icon">🎵</span>
      <p>点击上方按钮，让 AI 为你发现好音乐</p>
      <p class="empty-hint">收藏几首喜欢的歌，推荐会更精准哦</p>
    </div>

    <!-- 错误 -->
    <div class="empty error" v-if="errorMsg">
      <span class="empty-icon">⚠️</span>
      <p>{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const recs = ref([])
const loading = ref(false)
const errorMsg = ref('')
const recSource = ref('')
const isFallback = ref(false)

function genCover(title) {
  const colors = ['667eea','e250c0','f5576c','43e97b','764ba2']
  const h = (title||'?').charCodeAt(0) || 0
  return `https://via.placeholder.com/200/${colors[h%colors.length]}/fff?text=${encodeURIComponent((title||'?').slice(0,4))}`
}

async function fetchRecs() {
  if (!userStore.isLoggedIn) {
    errorMsg.value = '请先登录后再获取推荐'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await request.post('/recommendations')
    const data = res.data
    recs.value = data.songs || []
    recSource.value = data.source || 'fallback'
    isFallback.value = data.fallback || false
  } catch (e) {
    errorMsg.value = '推荐加载失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    fetchRecs()
  }
})
</script>

<style scoped>
.recommend-page { max-width: 1400px; margin: 0 auto; padding: 0 24px 40px; }
.page-hero { margin-bottom: 24px; }
.page-title { font-size: 28px; font-weight: 800; color: var(--text-primary); margin: 0; }
.page-desc { color: var(--text-muted); margin: 6px 0 0; font-size: 14px; }

.action-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px; padding: 20px 24px;
  margin-bottom: 24px;
  display: flex; align-items: center; flex-wrap: wrap; gap: 16px;
  justify-content: space-between;
}
.action-info { display: flex; align-items: center; gap: 10px; }
.action-label { font-size: 14px; color: var(--text-muted); }
.powered-by { font-size: 12px; color: var(--text-muted); opacity: 0.7; }
.refresh-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none; border-radius: 12px; font-weight: 600;
  transition: all 0.3s; padding: 10px 24px;
}
.refresh-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba(102,126,234,0.4); }

.fallback-alert { margin-bottom: 20px; }

.section-heading { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0 0 18px; }

.recs-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
@media (max-width: 1100px) { .recs-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px)  { .recs-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px)  { .recs-grid { grid-template-columns: 1fr; } }

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
.rec-cover-wrap {
  position: relative; aspect-ratio: 1; overflow: hidden;
  background: var(--bg-secondary);
}
.rec-cover {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.5s;
}
.rec-card:hover .rec-cover { transform: scale(1.1) rotate(2deg); }
.rec-info { padding: 12px 14px; }
.rec-title {
  font-size: 14px; font-weight: 600; color: var(--text-primary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.rec-artist { font-size: 12px; color: var(--text-muted); margin: 4px 0 6px; }
.rec-reason {
  font-size: 12px; color: var(--text-secondary); line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden;
}

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
.w-70 { width: 70%; } .w-50 { width: 50%; } .w-60 { width: 60%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
