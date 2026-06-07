<template>
  <div v-if="artist">
    <h2>{{ artist.name }}</h2>
    <p style="color:#909399">{{ artist.description || '暂无简介' }}</p>
    <el-divider />
    <h4>歌曲列表</h4>
    <div class="song-grid">
      <SongCard v-for="s in songs" :key="s.id" :song="s" @click="goSong" />
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

function goSong(s) { router.push(`/song/${s.id}`) }

onMounted(async () => {
  try {
    const res = await request.get(`/artists/${route.params.id}`)
    artist.value = res.data.artist
    songs.value = res.data.songs || []
  } catch (e) {}
})
</script>

<style scoped>
.song-grid { display: flex; flex-wrap: wrap; gap: 12px; }
</style>
