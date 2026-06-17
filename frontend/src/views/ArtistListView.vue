<template>
  <div class="artists-page">
    <div class="page-hero">
      <h2 class="page-title">🎤 歌手</h2>
      <p class="page-desc">发现你喜爱的艺术家</p>
    </div>

    <div class="search-row">
      <el-input
        v-model="keyword"
        placeholder="搜索歌手..."
        size="large"
        prefix-icon="Search"
        @keyup.enter="loadArtists"
        clearable
        class="artist-search"
      />
    </div>

    <!-- Loading骨架 -->
    <div class="artists-grid" v-if="loading">
      <div v-for="i in 12" :key="'sk'+i" class="artist-card sk-artist">
        <div class="artist-avatar-wrap">
          <div class="sk-circle"></div>
        </div>
        <div class="sk-line sk-name"></div>
        <div class="sk-line sk-nat"></div>
      </div>
    </div>
    <div class="artists-grid" v-else>
      <div
        v-for="a in artists"
        :key="a.id"
        class="artist-card"
        @click="$router.push(`/artist/${a.id}`)"
      >
        <div class="artist-avatar-wrap">
          <img
            :src="a.imageUrl || genAvatar(displayName(a))"
            :alt="a.name"
            class="artist-avatar"
            @error="e => e.target.src = genAvatar(displayName(a))"
          />
          <div class="avatar-ring"></div>
        </div>
        <div class="artist-name">{{ displayName(a) }}</div>
        <div class="artist-lang" v-if="a.nationality">{{ a.nationality }}</div>
      </div>
    </div>

    <div class="empty" v-if="!loading && !artists.length && !errorMsg">
      <span class="empty-icon">🎤</span>
      <p>暂无歌手数据</p>
    </div>
    <div class="empty error" v-if="errorMsg">
      <span class="empty-icon">⚠️</span><p>{{ errorMsg }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const keyword = ref('')
const artists = ref([])
const loading = ref(false)
const errorMsg = ref('')

// 中国歌手显示中文名，外国歌手显示原名
function displayName(a) {
  const nat = a.nationality || ''
  if (/中国|台湾|香港|新加坡|马来西亚/.test(nat) && a.originalName) {
    return a.originalName
  }
  return a.name
}

function genAvatar(name) {
  const colors = ['667eea','e250c0','f5576c','43e97b','764ba2','f093fb','4facfe','fa709a']
  const h = (name||'?').charCodeAt(0) || 0
  return `https://via.placeholder.com/120/${colors[h%colors.length]}/fff?text=${encodeURIComponent((name||'?')[0])}`
}

async function loadArtists() {
  loading.value = true; errorMsg.value = ''
  try {
    const res = await request.get('/artists', { params: { keyword: keyword.value, page: 1, size: 100 } })
    artists.value = res.data.list || []
  } catch (e) { errorMsg.value = '歌手列表加载失败' } finally { loading.value = false }
}
onMounted(loadArtists)
</script>

<style scoped>
.artists-page { max-width: 1400px; margin: 0 auto; padding: 0 24px 40px; }
.page-hero { margin-bottom: 24px; }
.page-title { font-size: 28px; font-weight: 800; color: var(--text-primary); margin: 0; }
.page-desc { color: var(--text-muted); margin: 6px 0 0; font-size: 14px; }

.search-row { margin-bottom: 28px; }
.artist-search { max-width: 420px; }
:deep(.artist-search .el-input__wrapper) {
  background: var(--bg-input);
  border: 2px solid var(--border);
  border-radius: 14px; box-shadow: none;
}
:deep(.artist-search .el-input__wrapper:hover) { border-color: rgba(102,126,234,0.4); }
:deep(.artist-search .el-input__wrapper.is-focus) {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(102,126,234,0.15);
}

.artists-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 24px;
}
@media (max-width: 1100px) { .artists-grid { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 768px) { .artists-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 480px) { .artists-grid { grid-template-columns: repeat(2, 1fr); } }

.artist-card {
  text-align: center; cursor: pointer;
  padding: 16px 8px; border-radius: 16px;
  transition: all 0.35s;
  background: var(--bg-card);
  border: 1px solid transparent;
}
.artist-card:hover {
  background: var(--bg-card-hover);
  border-color: var(--border);
  transform: translateY(-6px);
}
.artist-avatar-wrap {
  position: relative;
  width: 100px; height: 100px;
  margin: 0 auto 12px;
}
.artist-avatar {
  width: 100%; height: 100%;
  border-radius: 50%; object-fit: cover;
  transition: all 0.5s;
  position: relative; z-index: 1;
}
.artist-card:hover .artist-avatar {
  transform: scale(1.08);
  box-shadow: 0 8px 28px rgba(102,126,234,0.3);
}
.avatar-ring {
  position: absolute; inset: -4px; border-radius: 50%;
  border: 2px solid transparent;
  transition: all 0.5s;
}
.artist-card:hover .avatar-ring {
  border-color: rgba(102,126,234,0.4);
  transform: rotate(180deg);
}
.artist-name {
  font-size: 14px; font-weight: 600; color: var(--text-primary);
  transition: color 0.3s;
}
.artist-card:hover .artist-name { color: var(--accent); }
.artist-lang {
  font-size: 11px; color: var(--text-muted); margin-top: 4px;
}

.empty { text-align: center; padding: 60px 0; }
.empty-icon { font-size: 48px; }
.empty p { color: var(--text-muted); margin: 8px 0; }
.empty.error p { color: #f56c6c; }

/* Skeleton */
.sk-artist { pointer-events: none; }
.sk-circle {
  width: 100px; height: 100px; border-radius: 50%;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite; margin: 0 auto;
}
.sk-line {
  height: 12px; border-radius: 6px; margin: 0 auto;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.sk-name { width: 80px; margin-top: 12px; }
.sk-nat { width: 50px; margin-top: 6px; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
