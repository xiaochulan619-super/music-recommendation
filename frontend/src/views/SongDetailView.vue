<template>
  <div class="song-detail" v-if="song">
    <div class="detail-hero">
      <!-- 封面大图 - 悬浮有放大旋转效果 -->
      <div class="hero-cover-wrap">
        <img
          :src="coverSrc"
          :alt="song.title"
          class="hero-cover"
          @error="e => e.target.src = genCover(song.title)"
        />
        <div class="cover-shine-ring"></div>
        <div class="cover-search-btn" @click.stop="searchCovers" title="搜索真实封面">
          <span v-if="!searchingCover">🔍</span>
          <span v-else>⏳</span>
        </div>
      </div>

      <!-- 封面搜索结果 -->
      <div v-if="coverResults.length" class="cover-results">
        <p class="cover-results-title">选择封面图片：</p>
        <div class="cover-results-grid">
          <img
            v-for="(c, i) in coverResults"
            :key="i"
            :src="c.artworkUrl100 || c.artworkUrl600"
            class="cover-result-item"
            :class="{ selected: selectedCover === i }"
            @click="applyCover(c, i)"
            :title="c.trackName + ' - ' + c.collectionName"
          />
        </div>
        <el-button size="small" text @click="coverResults = []" style="color: var(--text-muted)">关闭</el-button>
      </div>

      <div class="hero-info">
        <span class="song-type-badge">单曲</span>
        <h1 class="hero-title">{{ song.title }}</h1>
        <p class="hero-artist">
          <router-link :to="'/artist/' + (song.artistId || 0)" class="artist-link">
            {{ displayName }}
          </router-link>
          <span v-if="song.nationality" class="nationality-tag">🌍 {{ song.nationality }}</span>
        </p>
        <p class="hero-meta">
          <span v-if="song.album">📀 专辑：{{ song.album }}</span>
          <span>⏱ {{ formatDuration(song.duration) }}</span>
          <span v-if="song.genre">🎼 {{ song.genre }}</span>
        </p>

        <!-- 操作按钮 -->
        <div class="hero-actions">
          <el-button type="primary" size="large" class="action-btn play-qq-btn" @click="openQQMusic">
            <span class="btn-icon">🎧</span> QQ音乐播放
          </el-button>
          <el-button
            :type="isFaved ? 'danger' : 'default'"
            size="large"
            class="action-btn fav-btn"
            @click="toggleFavorite"
          >
            <span class="btn-icon">{{ isFaved ? '💔' : '❤️' }}</span>
            {{ isFaved ? '取消收藏' : '收藏' }}
          </el-button>
          <div class="rating-group">
            <span class="rating-label">评分：</span>
            <el-rate
              v-model="rating"
              @change="submitRating"
              :colors="['#f5576c', '#f5576c', '#f5576c']"
              :void-color="'rgba(255,255,255,0.15)'"
            />
          </div>
        </div>

        <!-- 标签 -->
        <div class="tag-row" v-if="song.tags && song.tags.length">
          <span class="detail-tag" v-for="t in song.tags" :key="t">{{ t }}</span>
        </div>
      </div>
    </div>

    <!-- 评论区 -->
    <div class="comments-section">
      <div class="section-head">
        <h3>💬 评论 ({{ comments.length }})</h3>
      </div>

      <div class="comment-input-box">
        <el-input
          v-model="commentText"
          type="textarea"
          :rows="2"
          placeholder="写下你的评论..."
          class="comment-textarea"
        />
        <el-button
          type="primary"
          @click="submitComment"
          :disabled="!commentText.trim()"
          class="comment-submit-btn"
        >
          发表评论
        </el-button>
      </div>

      <div class="comments-list">
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <div class="comment-avatar">{{ (c.username || '?')[0] }}</div>
          <div class="comment-body">
            <div class="comment-head">
              <strong class="comment-user">{{ c.username }}</strong>
              <span class="comment-time">{{ c.createdAt }}</span>
            </div>
            <p class="comment-content">{{ c.content }}</p>
            <!-- 楼中楼回复 -->
            <div v-for="r in (c.replies || [])" :key="r.id" class="reply-item">
              <strong class="reply-user">{{ r.username }}</strong>
              <span class="reply-text">：{{ r.content }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="empty" v-if="!comments.length">
        <p>还没有评论，来说两句吧</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const song = ref(null)
const isFaved = ref(false)
const rating = ref(0)
const comments = ref([])
const commentText = ref('')
const coverResults = ref([])
const searchingCover = ref(false)
const selectedCover = ref(-1)

// 当前显示的封面（支持搜索结果替换）
const coverSrc = computed(() => {
  if (selectedCover.value >= 0 && coverResults.value[selectedCover.value]) {
    return coverResults.value[selectedCover.value].artworkUrl600
      || coverResults.value[selectedCover.value].artworkUrl100
  }
  return song.value?.coverUrl || genCover(song.value?.title || '?')
})

// 中文歌手用中文名，外国歌手用英文名
const displayName = computed(() => {
  if (!song.value) return '未知'
  const artist = song.value.artistName || '未知'
  // 如果数据库有原始外文名则优先显示
  return song.value.artistOriginalName || artist
})

function genCover(title) {
  const colors = ['667eea','e250c0','f5576c','43e97b','38f9d7','764ba2','f093fb']
  const h = (title||'?').split('').reduce((a,c)=>a+c.charCodeAt(0),0)
  return `https://via.placeholder.com/300/${colors[h%colors.length]}/fff?text=${encodeURIComponent((title||'?').slice(0,4))}`
}

function formatDuration(sec) {
  if (!sec) return '未知'
  return `${Math.floor(sec/60)}:${String(sec%60).padStart(2,'0')}`
}

// QQ音乐搜索跳转
function openQQMusic() {
  const query = encodeURIComponent(`${song.value.title} ${song.value.artistName || ''}`)
  window.open(`https://y.qq.com/n/ryqq/search?w=${query}`, '_blank')
  // 同时记录播放
  recordPlay()
}

async function loadSong() {
  const id = route.params.id
  try {
    const [sRes] = await Promise.all([request.get(`/songs/${id}`), loadComments()])
    song.value = sRes.data
  } catch (e) {}
  if (userStore.isLoggedIn) {
    try {
      const fRes = await request.get(`/favorites/check/${id}`)
      isFaved.value = !!fRes.data
    } catch (e) {}
  }
}

async function loadComments() {
  try {
    const res = await request.get(`/comments/song/${route.params.id}`)
    comments.value = res.data || []
  } catch (e) {}
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  try {
    if (isFaved.value) {
      await request.delete(`/favorites/${song.value.id}`)
      isFaved.value = false
      ElMessage.success('已取消收藏')
    } else {
      await request.post('/favorites', { songId: song.value.id })
      isFaved.value = true
      ElMessage.success('收藏成功')
    }
  } catch (e) {}
}

async function submitRating(val) {
  if (!userStore.isLoggedIn) { ElMessage.warning('请先登录'); return }
  try {
    await request.post('/ratings', { songId: song.value.id, score: val })
    ElMessage.success(`评分 ${val} 星`)
  } catch (e) {}
}

async function recordPlay() {
  if (!userStore.isLoggedIn) return
  try { await request.post('/history', { songId: song.value.id }) } catch (e) {}
}

async function searchCovers() {
  if (!song.value) return
  searchingCover.value = true
  try {
    const term = `${song.value.title} ${song.value.artistName || ''}`
    const res = await request.get('/covers/search', { params: { term, limit: 6 } })
    coverResults.value = res.data || []
    selectedCover.value = -1
  } catch (e) {
    ElMessage.warning('封面搜索失败')
  } finally {
    searchingCover.value = false
  }
}

function applyCover(cover, index) {
  selectedCover.value = index
}


onMounted(loadSong)
</script>

<style scoped>
.song-detail { max-width: 1100px; margin: 0 auto; padding: 0 24px 40px; }

/* ===== Hero区 ===== */
.detail-hero {
  display: flex; gap: 36px;
  padding: 32px;
  border-radius: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  margin-bottom: 40px;
}
@media (max-width: 768px) { .detail-hero { flex-direction: column; align-items: center; text-align: center; } }

.hero-cover-wrap {
  position: relative;
  width: 260px; height: 260px;
  flex-shrink: 0;
  border-radius: 16px; overflow: hidden;
  cursor: pointer;
  transition: transform 0.5s cubic-bezier(0.25,0.46,0.45,0.94);
}
.hero-cover-wrap:hover {
  transform: scale(1.05) rotate(2deg);
}
.hero-cover {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.5s;
}
.hero-cover-wrap:hover .hero-cover {
  transform: scale(1.08);
}
.cover-shine-ring {
  position: absolute; inset: 0;
  border-radius: 16px;
  border: 3px solid transparent;
  transition: all 0.5s;
}
.hero-cover-wrap:hover .cover-shine-ring {
  border-color: rgba(102,126,234,0.5);
  box-shadow: 0 0 40px rgba(102,126,234,0.2), inset 0 0 40px rgba(102,126,234,0.05);
}

/* 封面搜索按钮 */
.cover-search-btn {
  position: absolute;
  bottom: 10px; right: 10px;
  width: 32px; height: 32px;
  border-radius: 50%;
  background: rgba(0,0,0,0.6);
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; cursor: pointer;
  opacity: 0; transition: opacity 0.3s;
  z-index: 5;
}
.hero-cover-wrap:hover .cover-search-btn { opacity: 1; }
.cover-search-btn:hover { background: rgba(102,126,234,0.7); }

/* 封面搜索结果 */
.cover-results {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px; padding: 16px;
  max-width: 320px;
}
.cover-results-title {
  font-size: 12px; color: var(--text-muted); margin: 0 0 10px;
}
.cover-results-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
  margin-bottom: 10px;
}
.cover-result-item {
  width: 100%; aspect-ratio: 1; object-fit: cover;
  border-radius: 8px; cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
}
.cover-result-item:hover { border-color: var(--border-hover); transform: scale(1.05); }
.cover-result-item.selected { border-color: #667eea; box-shadow: 0 0 12px rgba(102,126,234,0.4); }

/* Hero信息 */
.hero-info { flex: 1; display: flex; flex-direction: column; gap: 8px; }
.song-type-badge {
  display: inline-block; padding: 3px 10px; border-radius: 20px;
  font-size: 11px; font-weight: 600; width: fit-content;
  background: rgba(102,126,234,0.2); color: var(--accent);
}
.hero-title { font-size: 32px; font-weight: 900; color: var(--text-primary); margin: 0; }
.hero-artist { font-size: 16px; color: var(--text-secondary); margin: 0; display: flex; align-items: center; gap: 10px; }
.artist-link { color: var(--text-secondary); text-decoration: none; transition: color 0.3s; }
.artist-link:hover { color: var(--accent); }
.nationality-tag {
  font-size: 12px; padding: 2px 8px; border-radius: 10px;
  background: var(--bg-card); color: var(--text-muted);
}
.hero-meta {
  display: flex; gap: 16px; flex-wrap: wrap;
  font-size: 13px; color: var(--text-muted); margin: 0;
}

/* 操作按钮 */
.hero-actions {
  display: flex; align-items: center; gap: 12px;
  margin-top: 8px; flex-wrap: wrap;
}
.action-btn {
  border-radius: 12px; font-weight: 600; transition: all 0.3s;
  display: flex; align-items: center; gap: 6px;
}
.action-btn:hover { transform: translateY(-2px); }
.btn-icon { font-size: 16px; }
.play-qq-btn {
  background: linear-gradient(135deg, #31c27c, #1aad5e);
  border: none; box-shadow: 0 4px 16px rgba(49,194,124,0.3);
  color: #fff;
}
.play-qq-btn:hover {
  box-shadow: 0 6px 24px rgba(49,194,124,0.5);
  transform: translateY(-2px);
}
.fav-btn {
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-primary);
}
.fav-btn:hover {
  background: rgba(255,80,80,0.1);
  border-color: rgba(255,80,80,0.3);
}
.rating-group { display: flex; align-items: center; gap: 8px; }
.rating-label { font-size: 13px; color: var(--text-muted); }

.tag-row { display: flex; gap: 6px; flex-wrap: wrap; }
.detail-tag {
  padding: 4px 12px; border-radius: 20px;
  font-size: 11px; color: var(--text-muted);
  background: var(--bg-card);
  border: 1px solid var(--border);
}

/* ===== 评论区 ===== */
.comments-section { }
.section-head { margin-bottom: 20px; }
.section-head h3 { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0; }

.comment-input-box { margin-bottom: 24px; }
:deep(.comment-textarea .el-textarea__inner) {
  background: var(--bg-input);
  border: 1px solid var(--border);
  border-radius: 12px; color: var(--text-primary);
}
:deep(.comment-textarea .el-textarea__inner:focus) {
  border-color: var(--accent);
}
.comment-submit-btn {
  margin-top: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none; border-radius: 10px;
}

.comments-list { display: flex; flex-direction: column; gap: 4px; }
.comment-item {
  display: flex; gap: 12px;
  padding: 14px; border-radius: 12px;
  background: var(--bg-card);
  border: 1px solid transparent;
  transition: all 0.2s;
}
.comment-item:hover { background: var(--bg-card-hover); border-color: var(--border); }
.comment-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #e250c0);
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 14px; flex-shrink: 0;
}
.comment-body { flex: 1; min-width: 0; }
.comment-head { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.comment-user { font-size: 13px; color: var(--text-primary); }
.comment-time { font-size: 11px; color: var(--text-muted); }
.comment-content { font-size: 14px; color: var(--text-secondary); margin: 0; line-height: 1.5; }

.reply-item {
  margin-top: 8px; margin-left: 6px; padding: 6px 10px;
  border-radius: 8px; background: var(--bg-card);
  font-size: 13px;
}
.reply-user { color: var(--accent); font-size: 12px; }
.reply-text { color: var(--text-secondary); }

.empty { text-align: center; padding: 40px 0; }
.empty p { color: var(--text-muted); font-size: 13px; }
</style>
