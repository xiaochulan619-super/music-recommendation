<template>
  <div class="home-page">
    <!-- ===== Hero 区域 ===== -->
    <section class="hero">
      <h1 class="hero-title">
        <span class="gradient-text">发现你的音乐世界</span>
      </h1>
      <p class="hero-subtitle">探索百万曲库，AI 智能推荐，找到你的专属旋律</p>

      <!-- 居中搜索栏 - 干净无突出 -->
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索歌曲、歌手、专辑..."
          class="search-input"
          @keyup.enter="doSearch"
        />
        <button class="search-clear" v-if="searchKeyword" @click="searchKeyword=''">✕</button>
      </div>

      <!-- 热门标签 -->
      <div class="hot-tags">
        <span class="hot-label">🔥 热门：</span>
        <span v-for="t in hotTags" :key="t" class="hot-tag" @click="searchTag(t)">{{ t }}</span>
      </div>
    </section>

    <!-- ===== 为你推荐轮播 ===== -->
    <section class="carousel-section" v-if="recommendSongs.length">
      <div class="section-header">
        <h2 class="section-title"><span>🎵</span> 为你推荐</h2>
        <router-link to="/discover" class="see-all">发现更多 →</router-link>
      </div>
      <div class="carousel-track-wrapper" @mouseenter="pauseCarousel" @mouseleave="resumeCarousel">
        <div class="carousel-track" :style="{ transform: `translateX(-${carouselOffset}px)` }">
          <div v-for="(song, i) in infiniteSongs" :key="song.uniqueKey || `${song.id}-${i}`"
            class="carousel-item" @click="goSong(song)">
            <div class="caro-cover-wrap">
              <img :src="song.coverUrl || song.cover_url || genCover(song)" alt="" class="caro-cover" />
              <div class="caro-play">▶</div>
            </div>
            <div class="caro-title">{{ song.title }}</div>
            <div class="caro-artist">{{ song.artistName || '未知' }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 本周热榜 ===== -->
    <section class="content-section">
      <div class="section-header">
        <h2 class="section-title"><span>🏆</span> 本周热榜</h2>
        <router-link to="/charts" class="see-all">查看全部 →</router-link>
      </div>
      <div class="charts-mini">
        <div v-for="(item, i) in charts" :key="i" class="chart-item" @click="goSong(item.song || item)">
          <span class="chart-rank" :class="'rank-' + (i+1)">{{ String(i+1).padStart(2, '0') }}</span>
          <div class="chart-cover-wrap">
            <img :src="(item.song || item).coverUrl || genCover(item.song || item)" alt="" class="chart-cover-img" />
          </div>
          <div class="chart-info">
            <div class="chart-name">{{ (item.song || item).title || '未知' }}</div>
            <div class="chart-artist">{{ (item.song || item).artistName || '未知' }}</div>
          </div>
          <div class="chart-count">🔥 {{ fmtCount(item.playCount || 0) }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import SongCard from '@/components/SongCard.vue'

const router = useRouter()
const searchKeyword = ref('')
const recommendSongs = ref([])
const charts = ref([])
const carouselOffset = ref(0)
const hotTags = ['周杰伦', '陈奕迅', '林俊杰', '邓紫棋', '薛之谦', 'Taylor Swift', '摇滚', 'R&B']

let carouselTimer = null
let paused = false

function genCover(song) {
  if (!song) return ''
  const colors = ['667eea','e250c0','f5576c','43e97b','38f9d7','764ba2','f093fb','4facfe']
  const h = (song.title || '?').split('').reduce((a,c)=>a+c.charCodeAt(0),0)
  return `https://via.placeholder.com/200/${colors[h%colors.length]}/fff?text=${encodeURIComponent((song.title||'?').slice(0,4))}`
}
function fmtCount(n) {
  if (n >= 10000) return (n/10000).toFixed(1) + 'w'
  if (n >= 1000) return (n/1000).toFixed(1) + 'k'
  return String(n)
}

const infiniteSongs = computed(() => {
  const list = recommendSongs.value
  if (!list.length) return []
  return [...list.map((s,i)=>({...s, uniqueKey: `a-${s.id}-${i}`})), ...list.map((s,i)=>({...s, uniqueKey: `b-${s.id}-${i}`}))]
})

function goSong(song) { if (song?.id) router.push(`/song/${song.id}`) }
function doSearch() {
  const kw = searchKeyword.value.trim()
  if (kw) router.push({ path: '/discover', query: { keyword: kw } })
}
function searchTag(t) { router.push({ path: '/discover', query: { keyword: t } }) }

const ITEM_W = 164, GAP = 12, STEP = ITEM_W + GAP
function startCarousel() {
  stopCarousel()
  carouselTimer = setInterval(() => {
    if (paused || !recommendSongs.value.length) return
    carouselOffset.value += 0.7
    const tw = recommendSongs.value.length * STEP
    if (carouselOffset.value >= tw) carouselOffset.value = 0
  }, 20)
}
function stopCarousel() { if (carouselTimer) clearInterval(carouselTimer) }
function pauseCarousel() { paused = true }
function resumeCarousel() { paused = false }

onMounted(async () => {
  try {
    const [sRes, cRes] = await Promise.all([
      request.get('/songs/recommend', { params: { limit: 15 } }),
      request.get('/charts', { params: { type: 'weekly', limit: 10 } })
    ])
    recommendSongs.value = sRes.data || []
    charts.value = cRes.data || []
  } catch (e) {
    console.error('首页数据加载失败:', e)
  }
  startCarousel()
})
onUnmounted(stopCarousel)
</script>

<style scoped>
.home-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 20px 60px;
}

/* ===== Hero ===== */
.hero {
  position: relative;
  text-align: center;
  padding: 48px 20px 40px;
}
.hero-title { font-size: 42px; font-weight: 900; margin: 0 0 12px; letter-spacing: -1px; }
.gradient-text {
  background: linear-gradient(135deg, var(--accent), #e250c0, #f5576c);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-subtitle {
  font-size: 16px; color: var(--text-muted); margin: 0 0 32px;
}

/* 干净搜索栏 - 无突出 */
.search-box {
  max-width: 540px; margin: 0 auto 18px;
  position: relative;
  display: flex; align-items: center;
  background: var(--bg-card);
  border: 2px solid var(--border);
  border-radius: 28px;
  transition: all 0.3s;
}
.search-box:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(102,126,234,0.1);
}
.search-icon {
  position: absolute; left: 16px;
  font-size: 16px; pointer-events: none;
}
.search-input {
  width: 100%;
  padding: 14px 44px 14px 44px;
  border: none; outline: none; border-radius: 28px;
  background: transparent;
  font-size: 15px; color: var(--text-primary);
}
.search-input::placeholder { color: var(--text-muted); }
.search-clear {
  position: absolute; right: 12px;
  width: 24px; height: 24px; border-radius: 50%;
  border: none; background: var(--border);
  color: var(--text-secondary); cursor: pointer;
  font-size: 12px; display: flex; align-items: center;
  justify-content: center; transition: all 0.2s;
}
.search-clear:hover { background: var(--border-hover); }

.hot-tags { display: flex; align-items: center; justify-content: center; gap: 6px; flex-wrap: wrap; }
.hot-label { color: var(--text-muted); font-size: 12px; }
.hot-tag {
  padding: 3px 12px; border-radius: 14px; cursor: pointer;
  font-size: 12px; color: var(--text-secondary);
  background: var(--bg-card); border: 1px solid var(--border);
  transition: all 0.2s;
}
.hot-tag:hover { color: var(--accent-hover); border-color: var(--accent); background: rgba(102,126,234,0.08); }

/* ===== 轮播 ===== */
.carousel-section { margin: 16px 0 40px; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.section-title { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0; display: flex; align-items: center; gap: 8px; }
.see-all { font-size: 13px; color: var(--text-muted); text-decoration: none; transition: color 0.2s; }
.see-all:hover { color: var(--accent); }

.carousel-track-wrapper { overflow: hidden; border-radius: 12px; }
.carousel-track { display: flex; gap: 12px; will-change: transform; }
.carousel-item {
  flex-shrink: 0; width: 154px; cursor: pointer;
  border-radius: 12px; padding: 8px;
  background: var(--bg-card); border: 1px solid var(--border);
  transition: all 0.3s;
}
.carousel-item:hover { transform: translateY(-4px); border-color: var(--accent); }
.caro-cover-wrap {
  position: relative; aspect-ratio: 1; border-radius: 8px;
  overflow: hidden; background: var(--bg-secondary); margin-bottom: 8px;
}
.caro-cover { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s; }
.carousel-item:hover .caro-cover { transform: scale(1.08); }
.caro-play {
  position: absolute; inset: 0; background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: #fff; opacity: 0; transition: opacity 0.25s;
}
.carousel-item:hover .caro-play { opacity: 1; }
.caro-title { font-size: 13px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.caro-artist { font-size: 11px; color: var(--text-muted); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ===== 内容区 ===== */
.content-section { margin-bottom: 44px; }

/* 4列网格 */
.song-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 1100px) { .song-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) { .song-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) { .song-grid { grid-template-columns: 1fr; } }

/* 榜单 */
.charts-mini { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
@media (max-width: 768px) { .charts-mini { grid-template-columns: 1fr; } }
.chart-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 12px; cursor: pointer;
  background: var(--bg-card); border: 1px solid var(--border);
  transition: all 0.3s;
}
.chart-item:hover { background: var(--bg-card-hover); transform: translateX(4px); border-color: var(--accent); }
.chart-rank { font-size: 17px; font-weight: 800; width: 28px; text-align: center; color: var(--text-muted); }
.chart-rank.rank-1 { color: #f5576c; } .chart-rank.rank-2 { color: #f093fb; } .chart-rank.rank-3 { color: #667eea; }
.chart-cover-wrap { width: 40px; height: 40px; border-radius: 6px; overflow: hidden; flex-shrink: 0; background: #1a1a2e; }
.chart-cover-img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s; }
.chart-item:hover .chart-cover-img { transform: scale(1.12); }
.chart-info { flex: 1; min-width: 0; }
.chart-name { font-size: 13px; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chart-artist { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.chart-count { font-size: 11px; color: var(--text-muted); flex-shrink: 0; }
</style>
