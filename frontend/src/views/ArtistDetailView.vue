<template>
  <div class="artist-detail" v-if="loading" style="padding-top:40px">
    <div class="artist-hero">
      <div class="artist-hero-bg"></div>
      <div class="artist-hero-content">
        <div class="sk-img-lg"></div>
        <div class="artist-hero-info" style="flex:1">
          <div class="sk-line w-30"></div>
          <div class="sk-line w-60" style="margin-top:16px;height:36px"></div>
          <div class="sk-line w-90" style="margin-top:20px"></div>
        </div>
      </div>
    </div>
  </div>
  <div class="artist-detail" v-else-if="artist">
    <div class="artist-hero">
      <div class="artist-hero-bg"></div>
      <div class="artist-hero-content">
        <img
          :src="artist.imageUrl || genAvatar(artist.name)"
          :alt="artist.name"
          class="artist-hero-img"
          @error="e => e.target.src = genAvatar(artist.name)"
        />
        <div class="artist-hero-info">
          <span class="artist-type-badge">艺人</span>
          <h1 class="artist-hero-name">{{ artist.name }}</h1>
          <p class="artist-hero-desc">{{ artist.description || '这位艺术家还没有简介，但他的音乐值得一听。' }}</p>
          <div class="artist-hero-meta">
            <span v-if="artist.nationality">🌍 {{ artist.nationality }}</span>
            <span>🎵 {{ songs.length }} 首歌曲</span>
          </div>
        </div>
      </div>
    </div>

    <div class="songs-section">
      <h3 class="section-heading">热门歌曲</h3>
      <div class="song-grid">
        <SongCard v-for="s in songs" :key="s.id" :song="s" @click="goSong" />
      </div>
      <div class="empty" v-if="!songs.length">
        <p>暂无歌曲</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import SongCard from '@/components/SongCard.vue'

const route = useRoute()
const router = useRouter()
const artist = ref(null)
const songs = ref([])
const loading = ref(true)
const errorMsg = ref('')

function genAvatar(name) {
  const colors = ['667eea','e250c0','f5576c','43e97b','764ba2','f093fb','4facfe']
  const h = (name||'?').charCodeAt(0) || 0
  return `https://via.placeholder.com/200/${colors[h%colors.length]}/fff?text=${encodeURIComponent((name||'?')[0])}`
}
function goSong(s) { router.push(`/song/${s.id}`) }

onMounted(async () => {
  loading.value = true; errorMsg.value = ''
  try {
    const res = await request.get(`/artists/${route.params.id}`)
    artist.value = res.data.artist
    songs.value = res.data.songs || []
  } catch (e) { errorMsg.value = '歌手信息加载失败' } finally { loading.value = false }
})
</script>

<style scoped>
.artist-detail { max-width: 1400px; margin: 0 auto; padding: 0 24px 40px; }

.artist-hero {
  position: relative;
  border-radius: 20px; overflow: hidden;
  margin-bottom: 40px;
}
.artist-hero-bg {
  position: absolute; inset: 0;
  background: var(--hero-gradient);
  z-index: 0;
}
.artist-hero-content {
  position: relative; z-index: 1;
  display: flex; align-items: center; gap: 32px;
  padding: 40px;
}
.artist-hero-img {
  width: 180px; height: 180px;
  border-radius: 50%; object-fit: cover;
  box-shadow: 0 8px 40px rgba(0,0,0,0.5);
  transition: transform 0.5s;
}
.artist-hero-img:hover { transform: scale(1.05) rotate(3deg); }
.artist-hero-info { flex: 1; }
.artist-type-badge {
  display: inline-block; padding: 4px 12px; border-radius: 20px;
  font-size: 11px; font-weight: 600;
  background: rgba(102,126,234,0.2); color: var(--accent);
  margin-bottom: 12px;
}
.artist-hero-name { font-size: 40px; font-weight: 900; color: var(--text-primary); margin: 0 0 10px; }
.artist-hero-desc { color: var(--text-muted); font-size: 14px; margin: 0 0 14px; max-width: 600px; }
.artist-hero-meta { display: flex; gap: 20px; }
.artist-hero-meta span { font-size: 13px; color: var(--text-muted); }

.section-heading { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0 0 18px; }
.song-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}
@media (max-width: 1100px) { .song-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) { .song-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) { .song-grid { grid-template-columns: 1fr; } }

.empty { text-align: center; padding: 40px 0; }
.empty p { color: var(--text-muted); }

/* Skeleton */
.sk-img-lg {
  width: 180px; height: 180px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.sk-line {
  height: 12px; border-radius: 6px;
  background: linear-gradient(90deg, var(--bg-card) 25%, var(--bg-card-hover) 50%, var(--bg-card) 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite;
}
.w-30 { width: 30%; } .w-60 { width: 60%; } .w-90 { width: 90%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
