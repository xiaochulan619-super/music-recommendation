<template>
  <div class="song-card" @click="$emit('click', song)">
    <div class="card-cover-wrapper">
      <img
        :src="coverSrc"
        :alt="song.title"
        class="card-cover"
        @error="onImgError"
      />
      <div class="cover-overlay">
        <div class="play-btn" @click.stop="handlePlay">
          <span>▶</span>
        </div>
        <div class="cover-actions">
          <span class="action-btn" @click.stop="handleQQMusic" title="QQ音乐打开">🎧</span>
        </div>
      </div>
      <div class="cover-shine"></div>
    </div>
    <div class="card-info">
      <div class="card-title" :title="song.title">{{ song.title }}</div>
      <div class="card-artist" :title="displayArtist">{{ displayArtist }}</div>
      <div class="card-tags" v-if="song.tags && song.tags.length">
        <span class="tag" v-for="t in song.tags.slice(0, 3)" :key="t">{{ t }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  song: { type: Object, required: true }
})
defineEmits(['click'])

const coverSrc = computed(() => {
  return props.song.coverUrl || props.song.cover_url || generatePlaceholder(props.song.title)
})

// 根据国籍显示合适名称：中国歌手用originalName(中文)，外国歌手用英文名
const displayArtist = computed(() => {
  const s = props.song
  const nationality = s.nationality || ''
  const artistName = s.artistName || s.artist_name || '未知歌手'
  const originalName = s.artistOriginalName || s.artist_original_name || ''
  // 中国/台湾/香港/新加坡：用 originalName (中文名)
  if (/中国|台湾|香港|新加坡|马来西亚/.test(nationality) && originalName) {
    return originalName
  }
  // 外国歌手用英文 artistName
  return artistName
})

function generatePlaceholder(title) {
  const colors = ['667eea', 'e250c0', 'f5576c', '43e97b', '38f9d7', '764ba2', 'f093fb', '4facfe']
  const hash = (title || '?').split('').reduce((a, c) => a + c.charCodeAt(0), 0)
  const bg = colors[hash % colors.length]
  const text = encodeURIComponent((title || '?').slice(0, 4))
  return `https://via.placeholder.com/300x300/${bg}/ffffff?text=${text}`
}

function onImgError(e) {
  e.target.src = generatePlaceholder(props.song.title)
}

function handlePlay() {
  window.__playTrack?.({
    title: props.song.title,
    artist: displayArtist.value,
    cover: coverSrc.value,
    previewUrl: props.song.previewUrl || props.song.preview_url
  })
}

function handleQQMusic() {
  const query = encodeURIComponent(`${props.song.title} ${displayArtist.value}`)
  window.open(`https://y.qq.com/n/ryqq/search?w=${query}`, '_blank')
}
</script>

<style scoped>
.song-card {
  cursor: pointer;
  border-radius: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
}
.song-card:hover {
  transform: translateY(-6px);
  border-color: var(--accent);
  box-shadow: var(--card-shadow);
}

/* 封面容器 */
.card-cover-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  background: var(--bg-secondary);
}
.card-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.song-card:hover .card-cover {
  transform: scale(1.1) rotate(2deg);
}

/* 封面悬浮层 */
.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  opacity: 0;
  transition: opacity 0.35s;
}
.song-card:hover .cover-overlay {
  opacity: 1;
}
.play-btn {
  width: 50px; height: 50px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #fff;
  transition: all 0.3s;
  border: 2px solid rgba(255,255,255,0.3);
}
.play-btn:hover {
  background: rgba(102, 126, 234, 0.7);
  transform: scale(1.15);
  border-color: #667eea;
}
.cover-actions {
  display: flex; gap: 8px;
}
.action-btn {
  padding: 5px 12px;
  border-radius: 20px;
  background: rgba(255,255,255,0.15);
  font-size: 13px; color: #fff;
  transition: all 0.3s;
}
.action-btn:hover {
  background: rgba(255,255,255,0.3);
  transform: scale(1.08);
}

/* 光效 */
.cover-shine {
  position: absolute;
  top: 0; left: -75%;
  width: 50%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.08), transparent);
  transform: skewX(-25deg);
  transition: left 0.6s;
}
.song-card:hover .cover-shine {
  left: 125%;
}

/* 信息区 */
.card-info {
  padding: 12px 14px;
}
.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
  transition: color 0.3s;
}
.song-card:hover .card-title { color: var(--accent); }
.card-artist {
  font-size: 12px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
  transition: color 0.3s;
}
.song-card:hover .card-artist { color: var(--text-secondary); }
.card-tags {
  display: flex; gap: 4px; flex-wrap: wrap;
}
.tag {
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 10px;
  background: rgba(102, 126, 234, 0.12);
  color: rgba(102, 126, 234, 0.8);
}
</style>
