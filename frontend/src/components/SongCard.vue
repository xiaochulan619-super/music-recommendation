<template>
  <el-card class="song-card" shadow="hover" @click="$emit('click', song)">
    <img :src="song.coverUrl || 'https://via.placeholder.com/150'" class="cover" />
    <div class="info">
      <div class="title">{{ song.title }}</div>
      <div class="artist">{{ song.artistName || '未知歌手' }}</div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({ song: { type: Object, required: true } })
defineEmits(['click'])
</script>

<style scoped>
.song-card { cursor: pointer; width: 165px; margin-bottom: 8px; }
.song-card .cover { width: 100%; height: 140px; object-fit: cover; border-radius: 4px; }
.song-card .info { margin-top: 8px; }
.song-card .title { font-size: 14px; font-weight: bold;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.song-card .artist { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
