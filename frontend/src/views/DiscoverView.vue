<template>
  <div class="page-wrap">
    <div class="page-hero">
      <h2 class="page-title">🔍 发现音乐</h2>
      <p class="page-desc">探索百万曲库</p>
    </div>
    <div class="search-row">
      <el-input v-model="keyword" placeholder="搜索歌曲名、歌手..." size="large"
        @keyup.enter="fetchSongs" clearable class="search-inp" />
      <div class="filter-tags" v-if="allTags.length">
        <span v-for="t in allTags" :key="t" class="filter-tag" :class="{ active: activeTag===t }"
          @click="filterByTag(t)">{{ t }}</span>
      </div>
    </div>
    <!-- Loading 骨架 -->
    <div class="song-grid" v-if="loading">
      <div v-for="i in 8" :key="'sk'+i" class="skeleton-card">
        <div class="sk-cover"></div>
        <div class="sk-line sk-line-1"></div>
        <div class="sk-line sk-line-2"></div>
      </div>
    </div>
    <div class="song-grid" v-else>
      <SongCard v-for="s in songs" :key="s.id" :song="s" @click="goSong" />
    </div>
    <div class="empty" v-if="!songs.length && !loading">
      <span class="empty-icon">🎵</span><p>暂无歌曲数据</p>
    </div>
    <div class="empty error" v-if="errorMsg">
      <span class="empty-icon">⚠️</span><p>{{ errorMsg }}</p>
    </div>
    <div class="pagination-wrap" v-if="total > size">
      <el-pagination background layout="prev, pager, next"
        v-model:current-page="page" :page-size="size" :total="total" @current-change="fetchSongs" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import SongCard from '@/components/SongCard.vue'

const route = useRoute(); const router = useRouter()
const keyword = ref(route.query.keyword || '')
const songs = ref([]); const page = ref(1); const size = 20; const total = ref(0); const loading = ref(false)
const allTags = ref([]); const activeTag = ref(''); const errorMsg = ref('')

function goSong(s) { router.push(`/song/${s.id}`) }
function filterByTag(t) {
  activeTag.value = activeTag.value === t ? '' : t
  keyword.value = activeTag.value ? t : ''
  page.value = 1; fetchSongs()
}
async function fetchSongs() {
  loading.value = true; errorMsg.value = ''
  try {
    const res = await request.get('/songs', { params: { keyword: keyword.value, page: page.value, size } })
    songs.value = res.data.list || []; total.value = res.data.total || 0
  } catch (e) { errorMsg.value = '加载失败，请检查网络连接' } finally { loading.value = false }
}
onMounted(async () => {
  await fetchSongs()
  try { allTags.value = ((await request.get('/tags')).data || []).map(t => t.name || t).slice(0, 12) } catch (e) {}
})
</script>

<style scoped>
.page-wrap { max-width: 1280px; margin: 0 auto; padding: 0 20px 40px; }
.page-hero { margin-bottom: 20px; }
.page-title { font-size: 26px; font-weight: 800; color: var(--text-primary); margin: 0; }
.page-desc { color: var(--text-muted); margin: 4px 0 0; font-size: 14px; }
.search-row { margin-bottom: 20px; }
.search-inp { max-width: 480px; }
.filter-tags { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 10px; }
.filter-tag {
  padding: 4px 12px; border-radius: 14px; cursor: pointer;
  font-size: 12px; color: var(--text-secondary);
  background: var(--bg-card); border: 1px solid var(--border);
  transition: all 0.2s;
}
.filter-tag:hover, .filter-tag.active { color: var(--accent); border-color: var(--accent); background: rgba(102,126,234,0.08); }
.song-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 1100px) { .song-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) { .song-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) { .song-grid { grid-template-columns: 1fr; } }
.empty { text-align: center; padding: 60px 0; }
.empty-icon { font-size: 48px; }
.empty p { color: var(--text-muted); margin: 8px 0; }
.empty.error p { color: #f56c6c; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 28px; }

/* Skeleton loading */
.skeleton-card {
  border-radius: 14px; overflow: hidden;
  background: var(--bg-card); border: 1px solid var(--border);
}
.sk-cover {
  width: 100%; aspect-ratio: 1;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.sk-line {
  height: 14px; margin: 10px 14px;
  border-radius: 6px;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.sk-line-1 { width: 75%; }
.sk-line-2 { width: 50%; margin-bottom: 14px; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
